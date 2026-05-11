package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoRequeridoDto;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.DocumentoRequeridoInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoRequeridoInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoRequeridoInformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoRequeridoInformeServiceTest {

    private static final String CONTRATISTA_EMAIL = "contratista@sed.gov.co";

    @Mock private DocumentoRequeridoInformeRepository repository;
    @Mock private InformeService informeService;
    @Mock private CurrentUserService currentUserService;
    @Mock private DocumentStorageService storageService;

    private DocumentoRequeridoInformeService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoRequeridoInformeService(
            repository, informeService, currentUserService, storageService
        );
    }

    // -----------------------------------------------------------------------
    // Listar
    // -----------------------------------------------------------------------

    @Test
    void listarIncludeFacturaPendienteWhenContratistaEsResponsableIva() {
        Informe informe = informeConIva(50L, true, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());

        List<DocumentoRequeridoDto> lista = service.listar(50L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getClaveLogica()).isEqualTo(DocumentoRequeridoInformeService.CLAVE_FACTURA);
        assertThat(lista.get(0).isCargado()).isFalse();
        assertThat(lista.get(0).isPorIva()).isTrue();
    }

    @Test
    void listarNoIncludeFacturaCuandoContratistaNoEsResponsableIva() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());

        List<DocumentoRequeridoDto> lista = service.listar(50L);

        assertThat(lista).isEmpty();
    }

    @Test
    void listarMarcaFacturaCargadaCuandoExisteEnBd() {
        Informe informe = informeConIva(50L, true, EstadoInforme.BORRADOR);
        DocumentoRequeridoInforme facturaReg = registroConArchivo(10L, DocumentoRequeridoInformeService.CLAVE_FACTURA);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.singletonList(facturaReg));

        List<DocumentoRequeridoDto> lista = service.listar(50L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).isCargado()).isTrue();
        assertThat(lista.get(0).isPorIva()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Upload PDF
    // -----------------------------------------------------------------------

    @Test
    void cargarArchivoPdfExitosoParaContratistaEnBorrador() throws IOException {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndClaveLogicaAndActivoTrue(50L, "POLIZA")).thenReturn(Optional.empty());
        when(storageService.storeFile(anyString(), any())).thenReturn("docs-requeridos/50/uuid_poliza.pdf");
        when(repository.save(any(DocumentoRequeridoInforme.class))).thenAnswer(inv -> {
            DocumentoRequeridoInforme r = inv.getArgument(0);
            r.setId(20L);
            return r;
        });

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "poliza.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        DocumentoRequeridoDto dto = service.cargarArchivo(50L, "POLIZA", archivo);

        assertThat(dto.getId()).isEqualTo(20L);
        assertThat(dto.isCargado()).isTrue();
        assertThat(dto.getExtension()).isEqualTo(".pdf");
    }

    @Test
    void cargarArchivoEmlExitosoParaContratistaEnBorrador() throws IOException {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndClaveLogicaAndActivoTrue(50L, "CORREO")).thenReturn(Optional.empty());
        when(storageService.storeFile(anyString(), any())).thenReturn("docs-requeridos/50/uuid_correo.eml");
        when(repository.save(any(DocumentoRequeridoInforme.class))).thenAnswer(inv -> {
            DocumentoRequeridoInforme r = inv.getArgument(0);
            r.setId(21L);
            return r;
        });

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "correo.eml", "message/rfc822", new byte[]{1, 2, 3}
        );

        DocumentoRequeridoDto dto = service.cargarArchivo(50L, "CORREO", archivo);

        assertThat(dto.getId()).isEqualTo(21L);
        assertThat(dto.getExtension()).isEqualTo(".eml");
    }

    @Test
    void cargarArchivoRechazaExtensionNoPermitida() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "documento.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.cargarArchivo(50L, "POLIZA", archivo))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_REQUERIDO_FORMATO_INVALIDO));
    }

    @Test
    void cargarArchivoRechazaEstadoNoEditable() {
        Informe informe = informeConIva(50L, false, EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "poliza.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.cargarArchivo(50L, "POLIZA", archivo))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_REQUERIDO_NO_EDITABLE));
    }

    @Test
    void cargarArchivoRechazaNoContratistaPropietario() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        // Usuario con id diferente al contratista del contrato (id=1L)
        when(currentUserService.getCurrentUser()).thenReturn(contratista(99L));

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "poliza.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.cargarArchivo(50L, "POLIZA", archivo))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void cargarArchivoFacturaRechazadaSiContratistaNoEsResponsableIva() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "factura.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.cargarArchivo(50L, DocumentoRequeridoInformeService.CLAVE_FACTURA, archivo))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_REQUERIDO_NO_ENCONTRADO));
    }

    @Test
    void cargarArchivoFacturaExitosoParaResponsableIva() throws IOException {
        Informe informe = informeConIva(50L, true, EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByInformeIdAndClaveLogicaAndActivoTrue(50L, DocumentoRequeridoInformeService.CLAVE_FACTURA))
            .thenReturn(Optional.empty());
        when(storageService.storeFile(anyString(), any())).thenReturn("docs-requeridos/50/uuid_factura.pdf");
        when(repository.save(any(DocumentoRequeridoInforme.class))).thenAnswer(inv -> {
            DocumentoRequeridoInforme r = inv.getArgument(0);
            r.setId(30L);
            return r;
        });

        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "factura.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        DocumentoRequeridoDto dto = service.cargarArchivo(50L, DocumentoRequeridoInformeService.CLAVE_FACTURA, archivo);

        assertThat(dto.getId()).isEqualTo(30L);
        assertThat(dto.isPorIva()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Validacion de documentos requeridos completos (para T5)
    // -----------------------------------------------------------------------

    @Test
    void assertDocumentosRequeridosCompletosNoLanzaExcepcionSiContratistaNoEsResponsableIva() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        // No debe lanzar excepcion
        service.assertDocumentosRequeridosCompletos(informe);
    }

    @Test
    void assertDocumentosRequeridosCompletosLanzaExcepcionSiFacturaFaltaParaResponsableIva() {
        Informe informe = informeConIva(50L, true, EstadoInforme.BORRADOR);
        when(repository.existsByInformeIdAndClaveLogicaAndStoragePathIsNotNullAndActivoTrue(
            50L, DocumentoRequeridoInformeService.CLAVE_FACTURA
        )).thenReturn(false);

        assertThatThrownBy(() -> service.assertDocumentosRequeridosCompletos(informe))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_REQUERIDO_FALTANTE));
    }

    @Test
    void assertDocumentosRequeridosCompletosNoLanzaExcepcionSiFacturaCargadaParaResponsableIva() {
        Informe informe = informeConIva(50L, true, EstadoInforme.BORRADOR);
        when(repository.existsByInformeIdAndClaveLogicaAndStoragePathIsNotNullAndActivoTrue(
            50L, DocumentoRequeridoInformeService.CLAVE_FACTURA
        )).thenReturn(true);

        // No debe lanzar excepcion
        service.assertDocumentosRequeridosCompletos(informe);
    }

    // -----------------------------------------------------------------------
    // Eliminar
    // -----------------------------------------------------------------------

    @Test
    void eliminarArchivoSoftDeleteEnBorrador() {
        Informe informe = informeConIva(50L, false, EstadoInforme.BORRADOR);
        DocumentoRequeridoInforme registro = registroConArchivo(10L, "POLIZA");
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));
        when(repository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(registro));

        service.eliminarArchivo(50L, 10L);

        ArgumentCaptor<DocumentoRequeridoInforme> captor = ArgumentCaptor.forClass(DocumentoRequeridoInforme.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
    }

    @Test
    void eliminarArchivoRechazaEstadoNoEditable() {
        Informe informe = informeConIva(50L, false, EstadoInforme.APROBADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(1L));

        assertThatThrownBy(() -> service.eliminarArchivo(50L, 10L))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_REQUERIDO_NO_EDITABLE));
        verify(repository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Informe informeConIva(Long id, boolean responsableIva, EstadoInforme estado) {
        Usuario contratista = new Usuario();
        contratista.setId(1L);
        contratista.setEmail(CONTRATISTA_EMAIL);
        contratista.setNombre("Contratista Test");
        contratista.setRol(RolUsuario.CONTRATISTA);
        contratista.setResponsableIva(responsableIva);
        contratista.setActivo(true);

        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setContratista(contratista);
        contrato.setActivo(true);

        Informe informe = new Informe();
        informe.setId(id);
        informe.setContrato(contrato);
        informe.setEstado(estado);
        informe.setActivo(true);
        return informe;
    }

    private static Usuario contratista(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(RolUsuario.CONTRATISTA);
        u.setActivo(true);
        return u;
    }

    private static DocumentoRequeridoInforme registroConArchivo(Long id, String clave) {
        DocumentoRequeridoInforme r = new DocumentoRequeridoInforme();
        r.setId(id);
        r.setClaveLogica(clave);
        r.setNombreDisplay(clave);
        r.setNombreArchivo(clave.toLowerCase() + ".pdf");
        r.setContentType("application/pdf");
        r.setExtension(".pdf");
        r.setStoragePath("docs-requeridos/50/uuid_" + clave.toLowerCase() + ".pdf");
        r.setTamanoBytes(1024L);
        r.setActivo(true);

        // Necesita informe para la validacion de pertenencia
        Informe informe = new Informe();
        informe.setId(50L);
        r.setInforme(informe);
        return r;
    }
}
