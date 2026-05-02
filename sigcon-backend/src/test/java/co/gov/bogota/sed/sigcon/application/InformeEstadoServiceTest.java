package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeEstadoServiceTest {

    private static final String CONTRATISTA_EMAIL = "contratista@educacionbogota.edu.co";
    private static final String REVISOR_EMAIL = "revisor@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL = "supervisor@educacionbogota.edu.co";

    @Mock private InformeRepository informeRepository;
    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private InformeService informeService;
    @Mock private ObservacionService observacionService;

    private InformeEstadoService service;

    @BeforeEach
    void setUp() {
        service = new InformeEstadoService(informeRepository, actividadRepository, informeService, observacionService);
    }

    @Test
    void enviarMovesDraftToEnviadoWhenInformeHasActivities() {
        Informe informe = informe(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(actividadRepository.countByInformeIdAndActivoTrue(50L)).thenReturn(2);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.enviar(50L, CONTRATISTA_EMAIL);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.ENVIADO);
        assertThat(informe.getFechaUltimoEnvio()).isNotNull();
        verify(informeRepository).save(informe);
    }

    @Test
    void enviarRejectsInformeWithoutActivities() {
        Informe informe = informe(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(actividadRepository.countByInformeIdAndActivoTrue(50L)).thenReturn(0);

        assertThatThrownBy(() -> service.enviar(50L, CONTRATISTA_EMAIL))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVIDAD_REQUERIDA));
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.BORRADOR);
        verify(informeRepository, never()).save(any(Informe.class));
    }

    @Test
    void enviarAcceptsDevueltoAndUpdatesLastSubmitDate() {
        Informe informe = informe(EstadoInforme.DEVUELTO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(actividadRepository.countByInformeIdAndActivoTrue(50L)).thenReturn(1);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.enviar(50L, CONTRATISTA_EMAIL);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.ENVIADO);
        assertThat(informe.getFechaUltimoEnvio()).isNotNull();
    }

    @Test
    void aprobarRevisionMovesEnviadoToEnRevisionForAssignedRevisor() {
        Informe informe = informe(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobarRevision(50L, REVISOR_EMAIL, "Se revisa sin novedades");

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
        verify(observacionService).registrar(eq(informe), eq(RolObservacion.REVISOR), eq("Se revisa sin novedades"));
    }

    @Test
    void aprobarRevisionRejectsWrongRevisor() {
        Informe informe = informe(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.aprobarRevision(50L, "otro@educacionbogota.edu.co", null))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void devolverRevisionRequiresObservation() {
        Informe informe = informe(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.devolverRevision(50L, REVISOR_EMAIL, " "))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OBSERVACION_REQUERIDA));
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.ENVIADO);
    }

    @Test
    void devolverRevisionMovesEnviadoToDevueltoAndRegistersObservation() {
        Informe informe = informe(EstadoInforme.ENVIADO);
        Observacion observacion = new Observacion();
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(observacionService.registrar(informe, RolObservacion.REVISOR, "Falta soporte"))
            .thenReturn(observacion);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.devolverRevision(50L, REVISOR_EMAIL, "Falta soporte");

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.DEVUELTO);
        verify(observacionService).registrar(informe, RolObservacion.REVISOR, "Falta soporte");
    }

    @Test
    void aprobarMovesEnRevisionToAprobadoWithNullPdfRuta() {
        Informe informe = informe(EstadoInforme.EN_REVISION);
        informe.setPdfRuta("no-debe-quedar.pdf");
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobar(50L, SUPERVISOR_EMAIL);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.APROBADO);
        assertThat(informe.getFechaAprobacion()).isNotNull();
        assertThat(informe.getPdfRuta()).isNull();
    }

    @Test
    void devolverFinalMovesEnRevisionToDevueltoAndRegistersSupervisorObservation() {
        Informe informe = informe(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(observacionService.registrar(informe, RolObservacion.SUPERVISOR, "Ajustar periodo"))
            .thenReturn(new Observacion());
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.devolver(50L, SUPERVISOR_EMAIL, "Ajustar periodo");

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.DEVUELTO);
        verify(observacionService).registrar(informe, RolObservacion.SUPERVISOR, "Ajustar periodo");
    }

    @Test
    void invalidTransitionReturnsTransicionInvalida() {
        Informe informe = informe(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.aprobarRevision(50L, REVISOR_EMAIL, null))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSICION_INVALIDA));
    }

    @Test
    void aprobadoIsTerminal() {
        Informe informe = informe(EstadoInforme.APROBADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.enviar(50L, CONTRATISTA_EMAIL))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INFORME_NO_EDITABLE));
    }

    private static Informe informe(EstadoInforme estado) {
        Usuario contratista = usuario(1L, CONTRATISTA_EMAIL, RolUsuario.CONTRATISTA);
        Usuario revisor = usuario(2L, REVISOR_EMAIL, RolUsuario.REVISOR);
        Usuario supervisor = usuario(3L, SUPERVISOR_EMAIL, RolUsuario.SUPERVISOR);

        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setContratista(contratista);
        contrato.setRevisor(revisor);
        contrato.setSupervisor(supervisor);
        contrato.setActivo(true);

        Informe informe = new Informe();
        informe.setId(50L);
        informe.setContrato(contrato);
        informe.setEstado(estado);
        informe.setActivo(true);
        return informe;
    }

    private static Usuario usuario(Long id, String email, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNombre(email);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}
