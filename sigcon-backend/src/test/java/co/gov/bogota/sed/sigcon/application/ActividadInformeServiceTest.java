package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeRequest;
import co.gov.bogota.sed.sigcon.application.mapper.ActividadInformeMapper;
import co.gov.bogota.sed.sigcon.application.mapper.SoporteAdjuntoMapper;
import co.gov.bogota.sed.sigcon.application.service.ActividadInformeService;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadInformeServiceTest {

    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private ObligacionRepository obligacionRepository;
    @Mock private SoporteAdjuntoRepository soporteRepository;
    @Mock private InformeService informeService;
    @Mock private CurrentUserService currentUserService;

    private ActividadInformeService service;

    @BeforeEach
    void setUp() {
        service = new ActividadInformeService(
            actividadRepository,
            obligacionRepository,
            soporteRepository,
            informeService,
            currentUserService,
            new ActividadInformeMapper(new SoporteAdjuntoMapper())
        );
    }

    @Test
    void createsActividadForObligacionOfSameContract() {
        Informe informe = informe(50L, contrato(10L, usuario(2L), EstadoContrato.EN_EJECUCION));
        Obligacion obligacion = new Obligacion();
        obligacion.setId(4L);
        obligacion.setContrato(informe.getContrato());
        obligacion.setActivo(true);

        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));
        when(obligacionRepository.findById(4L)).thenReturn(Optional.of(obligacion));
        when(actividadRepository.save(any(ActividadInforme.class))).thenAnswer(inv -> {
            ActividadInforme actividad = inv.getArgument(0);
            actividad.setId(9L);
            return actividad;
        });

        ActividadInformeRequest request = request(4L, "avance", new BigDecimal("35.5"));

        assertThat(service.crear(50L, request).getId()).isEqualTo(9L);
    }

    @Test
    void rejectsActividadFromAnotherInforme() {
        Informe informe = informe(50L, contrato(10L, usuario(2L), EstadoContrato.EN_EJECUCION));
        ActividadInforme actividad = new ActividadInforme();
        actividad.setId(9L);
        actividad.setInforme(informe(99L, contrato(10L, usuario(2L), EstadoContrato.EN_EJECUCION)));
        actividad.setActivo(true);

        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(usuario(2L));
        when(actividadRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(actividad));

        assertThatThrownBy(() -> service.actualizar(50L, 9L, request(4L, "avance", BigDecimal.TEN)))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    private static ActividadInformeRequest request(Long obligacionId, String descripcion, BigDecimal porcentaje) {
        ActividadInformeRequest request = new ActividadInformeRequest();
        request.setIdObligacion(obligacionId);
        request.setDescripcion(descripcion);
        request.setPorcentaje(porcentaje);
        return request;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(RolUsuario.CONTRATISTA);
        usuario.setActivo(true);
        return usuario;
    }

    private static Contrato contrato(Long id, Usuario contratista, EstadoContrato estado) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setContratista(contratista);
        contrato.setEstado(estado);
        contrato.setActivo(true);
        return contrato;
    }

    private static Informe informe(Long id, Contrato contrato) {
        Informe informe = new Informe();
        informe.setId(id);
        informe.setContrato(contrato);
        informe.setEstado(EstadoInforme.BORRADOR);
        informe.setActivo(true);
        return informe;
    }
}
