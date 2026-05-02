package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteUrlRequest;
import co.gov.bogota.sed.sigcon.application.mapper.SoporteAdjuntoMapper;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.SoporteAdjuntoService;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoporteAdjuntoServiceTest {

    @Mock private SoporteAdjuntoRepository soporteRepository;
    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private InformeService informeService;
    @Mock private CurrentUserService currentUserService;
    @Mock private DocumentStorageService documentStorageService;

    private SoporteAdjuntoService service;

    @BeforeEach
    void setUp() {
        service = new SoporteAdjuntoService(
            soporteRepository,
            actividadRepository,
            informeService,
            currentUserService,
            documentStorageService,
            new SoporteAdjuntoMapper()
        );
    }

    @Test
    void rejectsNonHttpUrl() {
        when(actividadRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(actividad()));
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));

        SoporteUrlRequest request = new SoporteUrlRequest();
        request.setNombre("Drive");
        request.setUrl("ftp://example.gov/file");

        assertThatThrownBy(() -> service.agregarSoporteUrl(7L, request))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SOPORTE_INVALIDO));
    }

    @Test
    void storesArchivoInContractInformeActividadSubdir() throws Exception {
        when(actividadRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(actividad()));
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));
        when(documentStorageService.storeFile(eq("soportes/10/50/7"), any()))
            .thenReturn("soportes/10/50/7/archivo.pdf");
        when(soporteRepository.save(any(SoporteAdjunto.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "archivo.pdf", "application/pdf", "x".getBytes());

        assertThat(service.agregarSoporteArchivo(7L, file).getReferencia())
            .isEqualTo("soportes/10/50/7/archivo.pdf");
        verify(documentStorageService).storeFile(eq("soportes/10/50/7"), any());
    }

    private static ActividadInforme actividad() {
        Usuario contratista = usuario(2L);
        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setContratista(contratista);
        Informe informe = new Informe();
        informe.setId(50L);
        informe.setContrato(contrato);
        informe.setEstado(EstadoInforme.BORRADOR);
        ActividadInforme actividad = new ActividadInforme();
        actividad.setId(7L);
        actividad.setInforme(informe);
        actividad.setActivo(true);
        return actividad;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(RolUsuario.CONTRATISTA);
        usuario.setActivo(true);
        return usuario;
    }
}
