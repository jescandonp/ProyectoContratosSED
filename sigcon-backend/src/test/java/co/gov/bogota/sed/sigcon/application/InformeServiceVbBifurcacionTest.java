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
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T5 — I9: Bifurcacion VB en enviar() y aprobarRevision().
 *
 * <p>Verifica que el estado destino depende del flag VB_ACTIVO:</p>
 * <ul>
 *   <li>VB activo  → EN_VISTO_BUENO</li>
 *   <li>VB inactivo → EN_REVISION (flujo anterior)</li>
 * </ul>
 *
 * <p>Inconsistencia documentada: el plan I9 menciona InformeService como archivo
 * a modificar, pero la maquina de estados real reside en InformeEstadoService.
 * Se aplica el codigo vigente como fuente de verdad (CONSTITUTION §autoridad).</p>
 */
@ExtendWith(MockitoExtension.class)
class InformeServiceVbBifurcacionTest {

    private static final String CONTRATISTA_EMAIL = "contratista@educacionbogota.edu.co";
    private static final String REVISOR_EMAIL     = "revisor@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL  = "supervisor@educacionbogota.edu.co";

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

    // ── enviar() ─────────────────────────────────────────────────────────────

    @Test
    void enviar_sinRevisor_vbActivo_pasaAEnVistoBueno() {
        Informe informe = informeSinRevisor(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        ActividadInforme actividad = actividad(101L);
        when(actividadRepository.findByInformeIdAndActivoTrue(50L))
            .thenReturn(Collections.singletonList(actividad));
        when(soporteRepository.existsByActividadIdAndTipoAndActivoTrue(101L, TipoSoporte.URL))
            .thenReturn(true);
        when(parametroService.isVbActivo()).thenReturn(true);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.enviar(50L, CONTRATISTA_EMAIL);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_VISTO_BUENO);
    }

    @Test
    void enviar_sinRevisor_vbInactivo_pasaAEnRevision() {
        Informe informe = informeSinRevisor(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        ActividadInforme actividad = actividad(101L);
        when(actividadRepository.findByInformeIdAndActivoTrue(50L))
            .thenReturn(Collections.singletonList(actividad));
        when(soporteRepository.existsByActividadIdAndTipoAndActivoTrue(101L, TipoSoporte.URL))
            .thenReturn(true);
        when(parametroService.isVbActivo()).thenReturn(false);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.enviar(50L, CONTRATISTA_EMAIL);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
    }

    // ── aprobarRevision() ────────────────────────────────────────────────────

    @Test
    void aprobarRevision_vbActivo_pasaAEnVistoBueno() {
        Informe informe = informeConRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(parametroService.isVbActivo()).thenReturn(true);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobarRevision(50L, REVISOR_EMAIL, null);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_VISTO_BUENO);
    }

    @Test
    void aprobarRevision_vbInactivo_pasaAEnRevision() {
        Informe informe = informeConRevisor(EstadoInforme.ENVIADO);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(parametroService.isVbActivo()).thenReturn(false);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobarRevision(50L, REVISOR_EMAIL, null);

        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Informe cuyo contrato NO tiene revisor asignado (flujo directo a VB o EN_REVISION). */
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

    /** Informe cuyo contrato SÍ tiene revisor asignado. */
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

    private static ActividadInforme actividad(Long id) {
        ActividadInforme a = new ActividadInforme();
        a.setId(id);
        a.setActivo(true);
        return a;
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
