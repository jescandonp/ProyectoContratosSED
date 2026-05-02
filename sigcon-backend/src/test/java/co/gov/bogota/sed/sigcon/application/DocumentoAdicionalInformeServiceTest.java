package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalRequest;
import co.gov.bogota.sed.sigcon.application.mapper.DocumentoAdicionalMapper;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentoAdicionalInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoAdicionalInformeServiceTest {

    @Mock private DocumentoAdicionalRepository documentoAdicionalRepository;
    @Mock private DocumentoCatalogoRepository documentoCatalogoRepository;
    @Mock private InformeService informeService;
    @Mock private CurrentUserService currentUserService;

    private DocumentoAdicionalInformeService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoAdicionalInformeService(
            documentoAdicionalRepository,
            documentoCatalogoRepository,
            informeService,
            currentUserService,
            new DocumentoAdicionalMapper()
        );
    }

    @Test
    void addsDocumentoAdicionalToEditableInforme() {
        Informe informe = informe(50L);
        DocumentoCatalogo catalogo = new DocumentoCatalogo();
        catalogo.setId(3L);
        catalogo.setNombre("Planilla");
        catalogo.setActivo(true);

        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));
        when(documentoCatalogoRepository.findById(3L)).thenReturn(Optional.of(catalogo));
        when(documentoAdicionalRepository.save(any(DocumentoAdicional.class))).thenAnswer(inv -> {
            DocumentoAdicional documento = inv.getArgument(0);
            documento.setId(7L);
            return documento;
        });

        DocumentoAdicionalRequest request = new DocumentoAdicionalRequest();
        request.setIdCatalogo(3L);
        request.setReferencia("https://example.gov/doc");

        assertThat(service.agregar(50L, request).getId()).isEqualTo(7L);
    }

    @Test
    void rejectsInactiveCatalogItem() {
        Informe informe = informe(50L);
        DocumentoCatalogo catalogo = new DocumentoCatalogo();
        catalogo.setId(3L);
        catalogo.setActivo(false);

        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));
        when(documentoCatalogoRepository.findById(3L)).thenReturn(Optional.of(catalogo));

        DocumentoAdicionalRequest request = new DocumentoAdicionalRequest();
        request.setIdCatalogo(3L);
        request.setReferencia("ref");

        assertThatThrownBy(() -> service.agregar(50L, request))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DOCUMENTO_ADICIONAL_REQUERIDO));
    }

    private static Informe informe(Long id) {
        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setEstado(EstadoContrato.EN_EJECUCION);
        contrato.setActivo(true);
        Informe informe = new Informe();
        informe.setId(id);
        informe.setContrato(contrato);
        informe.setEstado(EstadoInforme.BORRADOR);
        informe.setActivo(true);
        return informe;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(RolUsuario.CONTRATISTA);
        usuario.setActivo(true);
        return usuario;
    }
}
