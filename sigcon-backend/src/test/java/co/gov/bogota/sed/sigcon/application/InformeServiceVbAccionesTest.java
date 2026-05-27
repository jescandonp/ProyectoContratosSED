package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.service.DocumentoRequeridoInformeService;
import co.gov.bogota.sed.sigcon.application.service.EmailNotificacionService;
import co.gov.bogota.sed.sigcon.application.service.EventoInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.application.service.ParametroService;
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
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T6 — I9: Acciones del Actor Administrativo.
 *
 * <p>Verifica darVistosBueno(), escalar() y devolver() extendido
 * desde el estado EN_VISTO_BUENO.</p>
 */
@ExtendWith(MockitoExtension.class)
class InformeServiceVbAccionesTest {

    private static final String ADMIN_EMAIL = "administrativo@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL = "supervisor@educacionbogota.edu.co";

    @Mock private InformeRepository              informeRepository;
    @Mock private ActividadInformeRepository     actividadRepository;
    @Mock private SoporteAdjuntoRepository       soporteRepository;
    @Mock private InformeService                 informeService;
    @Mock private ObservacionService             observacionService;
    @Mock private PdfInformeService              pdfInformeService;
    @Mock private EventoInformeService           eventoInformeService;
    @Mock private DocumentoRequeridoInformeService documentoRequeridoInformeService;
    @Mock private EmailNotificacionService       emailNotificacionService;
    @Mock private ParametroService               parametroService;

    private InformeEstadoService service;

    @BeforeEach
    void setUp() {
        service = new InformeEstadoService(
            informeRepository, actividadRepository, soporteRepository,
            informeService, observacionService,
            pdfInformeService, eventoInformeService,
            documentoRequeridoInformeService, emailNotificacionService,
            parametroService
        );
    }

    // ── darVistosBueno() ─────────────────────────────────────────────────────

    @Test
    void darVistosBueno_estadoCorrecto_pasaAEnRevision() {
        Informe informe = informeEnVistoBueno();
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.darVistosBueno(50L, null);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
    }

    @Test
    void darVistosBueno_estadoIncorrecto_lanzaExcepcion() {
        Informe informe = informeConEstado(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.darVistosBueno(50L, null))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRANSICION_INVALIDA));
    }

    // ── escalar() ────────────────────────────────────────────────────────────

    @Test
    void escalar_persisteObservacionConAccionEscalacion() {
        Informe informe = informeEnVistoBueno();
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
        when(observacionService.registrarConAccion(
                eq(informe), eq(RolObservacion.ADMINISTRATIVO), eq("Escalo al revisor"), accionCaptor.capture()))
            .thenReturn(new Observacion());
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.escalar(50L, "Escalo al revisor");

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.ENVIADO);
        assertThat(accionCaptor.getValue()).isEqualTo("ESCALACION");
    }

    // ── devolver() extendido ─────────────────────────────────────────────────

    @Test
    void devolver_desdeVistoBueno_actorAdministrativo_pasaADevuelto() {
        Informe informe = informeEnVistoBueno();
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(observacionService.registrarConAccion(
                eq(informe), eq(RolObservacion.ADMINISTRATIVO), eq("Falta soporte"), eq("DEVOLUCION")))
            .thenReturn(new Observacion());
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.devolverDesdeVistoBueno(50L, "Falta soporte");

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.DEVUELTO);
    }

    @Test
    void devolver_desdeVistoBueno_sinObservacion_lanzaExcepcion() {
        Informe informe = informeEnVistoBueno();
        when(informeService.findActiveInforme(50L)).thenReturn(informe);

        assertThatThrownBy(() -> service.devolverDesdeVistoBueno(50L, null))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OBSERVACION_REQUERIDA));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Informe informeEnVistoBueno() {
        return informeConEstado(EstadoInforme.EN_VISTO_BUENO);
    }

    private static Informe informeConEstado(EstadoInforme estado) {
        Usuario contratista = usuario(1L, "contratista@educacionbogota.edu.co", RolUsuario.CONTRATISTA);
        Usuario supervisor  = usuario(3L, SUPERVISOR_EMAIL, RolUsuario.SUPERVISOR);

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

    private static Usuario usuario(Long id, String email, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(email);
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }
}
