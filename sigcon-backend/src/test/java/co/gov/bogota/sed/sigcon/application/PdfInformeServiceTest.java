package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.InformePdfTemplateService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfInformeServiceTest {

    @Mock private InformePdfTemplateService templateService;
    @Mock private DocumentStorageService storageService;
    @Mock private InformeRepository informeRepository;

    private PdfInformeService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfInformeService(templateService, storageService, informeRepository);
    }

    @Test
    void firmaContratistaAusenteBloquea() {
        Informe informe = informe(contrato(usuarioSinFirma(2L), usuarioConFirma(4L)));

        assertThatThrownBy(() -> pdfService.generarYPersistir(informe))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FIRMA_REQUERIDA));
    }

    @Test
    void firmaSupervisorAusenteBloquea() {
        Informe informe = informe(contrato(usuarioConFirma(2L), usuarioSinFirma(4L)));

        assertThatThrownBy(() -> pdfService.generarYPersistir(informe))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FIRMA_REQUERIDA));
    }

    @Test
    void firmaRevisorAsignadoAusenteBloquea() {
        Contrato contrato = contrato(usuarioConFirma(2L), usuarioConFirma(4L));
        contrato.setRevisor(usuarioSinFirma(5L));
        Informe informe = informe(contrato);

        assertThatThrownBy(() -> pdfService.generarYPersistir(informe))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FIRMA_REQUERIDA));
    }

    @Test
    void pdfExistenteNoSeRegenera() throws Exception {
        Informe informe = informe(contrato(usuarioConFirma(2L), usuarioConFirma(4L)));
        informe.setPdfRuta("pdfs/1/1/informe-1.pdf");

        pdfService.generarYPersistir(informe);

        verify(templateService, never()).generarPdf(any(), any(), any(), any());
    }

    @Test
    void generacionExitosaSetaRutaHashYTimestamp() throws Exception {
        Usuario contratista = usuarioConFirma(2L);
        Usuario supervisor  = usuarioConFirma(4L);
        Informe informe = informe(contrato(contratista, supervisor));

        byte[] fakePdf = new byte[]{1, 2, 3, 4, 5};
        byte[] fakeFirmaBytes = new byte[]{9, 8, 7};

        when(storageService.loadFile(contratista.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(storageService.loadFile(supervisor.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(templateService.generarPdf(any(), any(), any(), any())).thenReturn(fakePdf);
        when(storageService.storeBytes(anyString(), anyString(), eq(fakePdf)))
            .thenReturn("pdfs/1/1/informe-1.pdf");
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));

        pdfService.generarYPersistir(informe);

        assertThat(informe.getPdfRuta()).isEqualTo("pdfs/1/1/informe-1.pdf");
        assertThat(informe.getPdfHash()).isNotNull().isNotEmpty();
        assertThat(informe.getPdfGeneradoAt()).isNotNull().isBefore(LocalDateTime.now().plusSeconds(1));
        verify(informeRepository).save(informe);
    }

    @Test
    void generacionConRevisorAsignadoCargaFirmaRevisor() throws Exception {
        Usuario contratista = usuarioConFirma(2L);
        Usuario supervisor  = usuarioConFirma(4L);
        Usuario revisor     = usuarioConFirma(5L, RolUsuario.REVISOR);
        Contrato contrato = contrato(contratista, supervisor);
        contrato.setRevisor(revisor);
        Informe informe = informe(contrato);

        byte[] fakePdf = new byte[]{1, 2, 3, 4, 5};
        byte[] fakeFirmaBytes = new byte[]{9, 8, 7};
        byte[] fakeFirmaRevisor = new byte[]{6, 5, 4};

        when(storageService.loadFile(contratista.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(storageService.loadFile(supervisor.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(storageService.loadFile(revisor.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaRevisor));
        when(templateService.generarPdf(any(), any(), any(), eq(fakeFirmaRevisor))).thenReturn(fakePdf);
        when(storageService.storeBytes(anyString(), anyString(), eq(fakePdf)))
            .thenReturn("pdfs/1/1/informe-1.pdf");
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));

        pdfService.generarYPersistir(informe);

        verify(templateService).generarPdf(eq(informe), eq(fakeFirmaBytes), eq(fakeFirmaBytes), eq(fakeFirmaRevisor));
    }

    @Test
    void errorEnStorageLanzaPdfGeneracionFallida() throws Exception {
        Usuario contratista = usuarioConFirma(2L);
        Usuario supervisor  = usuarioConFirma(4L);
        Informe informe = informe(contrato(contratista, supervisor));

        byte[] fakeFirmaBytes = new byte[]{9, 8, 7};
        when(storageService.loadFile(contratista.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(storageService.loadFile(supervisor.getFirmaImagen()))
            .thenReturn(new ByteArrayInputStream(fakeFirmaBytes));
        when(templateService.generarPdf(any(), any(), any(), any())).thenReturn(new byte[]{1});
        when(storageService.storeBytes(any(), any(), any()))
            .thenThrow(new java.io.IOException("disco lleno"));

        assertThatThrownBy(() -> pdfService.generarYPersistir(informe))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PDF_GENERACION_FALLIDA));
    }

    // ---- Helpers ----

    private static Usuario usuarioConFirma(Long id) {
        return usuarioConFirma(id, id == 4L ? RolUsuario.SUPERVISOR : RolUsuario.CONTRATISTA);
    }

    private static Usuario usuarioConFirma(Long id, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@educacionbogota.edu.co");
        u.setNombre("Usuario " + id);
        u.setRol(rol);
        u.setFirmaImagen("firmas/" + id + "/firma.png");
        u.setActivo(true);
        return u;
    }

    private static Usuario usuarioSinFirma(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@educacionbogota.edu.co");
        u.setNombre("Usuario " + id);
        u.setRol(id == 4L ? RolUsuario.SUPERVISOR : RolUsuario.CONTRATISTA);
        u.setFirmaImagen(null);
        u.setActivo(true);
        return u;
    }

    private static Contrato contrato(Usuario contratista, Usuario supervisor) {
        Contrato c = new Contrato();
        c.setId(1L);
        c.setNumero("OPS-2026-001");
        c.setObjeto("Objeto de prueba");
        c.setTipo(TipoContrato.OPS);
        c.setValorTotal(BigDecimal.valueOf(18000000));
        c.setFechaInicio(LocalDate.of(2026, 1, 15));
        c.setFechaFin(LocalDate.of(2026, 12, 31));
        c.setEstado(EstadoContrato.EN_EJECUCION);
        c.setContratista(contratista);
        c.setSupervisor(supervisor);
        c.setActivo(true);
        return c;
    }

    private static Informe informe(Contrato contrato) {
        Informe i = new Informe();
        i.setId(1L);
        i.setNumero(1);
        i.setContrato(contrato);
        i.setFechaInicio(LocalDate.of(2026, 1, 1));
        i.setFechaFin(LocalDate.of(2026, 1, 31));
        i.setFechaElaboracion(LocalDate.of(2026, 2, 4));
        i.setEstado(EstadoInforme.EN_REVISION);
        i.setActivo(true);
        return i;
    }
}
