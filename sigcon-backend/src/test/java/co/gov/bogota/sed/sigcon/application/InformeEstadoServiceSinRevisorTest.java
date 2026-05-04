package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.service.EventoInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * I4 tests: supervisor acts directly from ENVIADO when contrato has no revisor.
 */
@ExtendWith(MockitoExtension.class)
class InformeEstadoServiceSinRevisorTest {

    private static final String CONTRATISTA_EMAIL = "contratista@educacionbogota.edu.co";
    private static final String REVISOR_EMAIL     = "revisor@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL  = "supervisor@educacionbogota.edu.co";

    @Mock private InformeRepository informeRepository;
    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private InformeService informeService;
    @Mock private ObservacionService observacionService;
    @Mock private PdfInformeService pdfInformeService;
    @Mock private EventoInformeService eventoInformeService;

    private InformeEstadoService service;

    @BeforeEach
    void setUp() {
        service = new InformeEstadoService(
            informeRepository, actividadRepository, informeService, observacionService,
            pdfInformeService, eventoInformeService
        );
    }

    // -------------------------------------------------------------------------
    // 1. aprobarSinRevisorDesdeEnviado
    // -------------------------------------------------------------------------

    @Test
    void aprobarSinRevisorDesdeEnviado() throws Exception {
        Informe informe = informeSinRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobar(50L, SUPERVISOR_EMAIL);

        verify(pdfInformeService).generarYPersistir(informe);
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.APROBADO);
    }

    // -------------------------------------------------------------------------
    // 2. devolverSinRevisorDesdeEnviado
    // -------------------------------------------------------------------------

    @Test
    void devolverSinRevisorDesdeEnviado() {
        Informe informe = informeSinRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(observacionService.registrar(informe, RolObservacion.SUPERVISOR, "Ajustar datos"))
            .thenReturn(new Observacion());
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.devolver(50L, SUPERVISOR_EMAIL, "Ajustar datos");

        verify(observacionService).registrar(informe, RolObservacion.SUPERVISOR, "Ajustar datos");
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.DEVUELTO);
    }

    // -------------------------------------------------------------------------
    // 3. aprobarConRevisorDesdeEnviadoFalla
    // -------------------------------------------------------------------------

    @Test
    void aprobarConRevisorDesdeEnviadoFalla() {
        // informe with a revisor assigned but state is ENVIADO — must throw TRANSICION_INVALIDA
        Informe informe = informeConRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.aprobar(50L, SUPERVISOR_EMAIL))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSICION_INVALIDA));
    }

    // -------------------------------------------------------------------------
    // 4. supervisorNoAsignadoFallaEnFlujoSinRevisor
    // -------------------------------------------------------------------------

    @Test
    void supervisorNoAsignadoFallaEnFlujoSinRevisor() {
        Informe informe = informeSinRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.aprobar(50L, "otro@educacionbogota.edu.co"))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    // -------------------------------------------------------------------------
    // 5. revisorNoPuedeActuarEnContratoSinRevisor
    // -------------------------------------------------------------------------

    @Test
    void revisorNoPuedeActuarEnContratoSinRevisor() {
        // aprobarRevision() calls assertAssignedEmail(contrato.getRevisor(), email)
        // When revisor == null, assertAssignedEmail throws ACCESO_DENEGADO
        Informe informe = informeSinRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.aprobarRevision(50L, REVISOR_EMAIL, null))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    // -------------------------------------------------------------------------
    // 6. revisorRemovidoConInformeEnRevision
    // -------------------------------------------------------------------------

    @Test
    void revisorRemovidoConInformeEnRevision() throws Exception {
        // informe already in EN_REVISION, revisor was removed → supervisor can still approve
        Informe informe = informeSinRevisor(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobar(50L, SUPERVISOR_EMAIL);

        verify(pdfInformeService).generarYPersistir(informe);
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.APROBADO);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds an informe whose contrato has NO revisor assigned. */
    private static Informe informeSinRevisor(EstadoInforme estado) {
        Usuario contratista = usuario(1L, CONTRATISTA_EMAIL, RolUsuario.CONTRATISTA);
        Usuario supervisor  = usuario(3L, SUPERVISOR_EMAIL,  RolUsuario.SUPERVISOR);

        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setContratista(contratista);
        contrato.setRevisor(null);
        contrato.setSupervisor(supervisor);
        contrato.setActivo(true);

        Informe informe = new Informe();
        informe.setId(50L);
        informe.setContrato(contrato);
        informe.setEstado(estado);
        informe.setActivo(true);
        return informe;
    }

    /** Builds an informe whose contrato HAS a revisor assigned (standard flow). */
    private static Informe informeConRevisor(EstadoInforme estado) {
        Usuario contratista = usuario(1L, CONTRATISTA_EMAIL, RolUsuario.CONTRATISTA);
        Usuario revisor     = usuario(2L, REVISOR_EMAIL,     RolUsuario.REVISOR);
        Usuario supervisor  = usuario(3L, SUPERVISOR_EMAIL,  RolUsuario.SUPERVISOR);

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
