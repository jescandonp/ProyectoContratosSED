package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.application.service.ParametroService;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;/**
 * Maquina de estados del informe.
 * I3: genera PDF y publica eventos de notificacion en cada transicion.
 * I7: valida documentos requeridos (incluida FACTURA por IVA) antes de enviar.
 */
@Service
@Transactional
public class InformeEstadoService {

    private final InformeRepository informeRepository;
    private final ActividadInformeRepository actividadRepository;
    private final SoporteAdjuntoRepository soporteRepository;
    private final InformeService informeService;
    private final ObservacionService observacionService;
    private final PdfInformeService pdfInformeService;
    private final EventoInformeService eventoInformeService;
    private final DocumentoRequeridoInformeService documentoRequeridoInformeService;
    private final EmailNotificacionService emailNotificacionService;
    private final ParametroService parametroService;

    public InformeEstadoService(
        InformeRepository informeRepository,
        ActividadInformeRepository actividadRepository,
        SoporteAdjuntoRepository soporteRepository,
        InformeService informeService,
        ObservacionService observacionService,
        PdfInformeService pdfInformeService,
        EventoInformeService eventoInformeService,
        DocumentoRequeridoInformeService documentoRequeridoInformeService,
        EmailNotificacionService emailNotificacionService,
        ParametroService parametroService
    ) {
        this.informeRepository = informeRepository;
        this.actividadRepository = actividadRepository;
        this.soporteRepository = soporteRepository;
        this.informeService = informeService;
        this.observacionService = observacionService;
        this.pdfInformeService = pdfInformeService;
        this.eventoInformeService = eventoInformeService;
        this.documentoRequeridoInformeService = documentoRequeridoInformeService;
        this.emailNotificacionService = emailNotificacionService;
        this.parametroService = parametroService;
    }

    /**
     * BORRADOR | DEVUELTO -> ENVIADO
     * Requiere al menos una actividad.
     * I3: publica INFORME_ENVIADO al revisor.
     * I9: si el contrato no tiene revisor y VB esta activo, el estado destino
     *     es EN_VISTO_BUENO en lugar de EN_REVISION.
     */
    public InformeDetalleDto enviar(Long informeId, String contratistaEmail) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.BORRADOR, EstadoInforme.DEVUELTO);
        assertAssignedEmail(informe.getContrato().getContratista(), contratistaEmail);
        List<ActividadInforme> actividades = actividadRepository.findByInformeIdAndActivoTrue(informe.getId());
        if (actividades.isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.ACTIVIDAD_REQUERIDA,
                "El informe debe tener al menos una actividad para ser enviado",
                HttpStatus.BAD_REQUEST
            );
        }
        assertSoporteUrlPorActividad(actividades);
        documentoRequeridoInformeService.assertDocumentosRequeridosCompletos(informe);
        informe.setEstado(EstadoInforme.ENVIADO);
        informe.setFechaUltimoEnvio(LocalDateTime.now());
        // I9: si no hay revisor y VB esta activo, el informe salta directamente a EN_VISTO_BUENO
        boolean sinRevisor = informe.getContrato().getRevisor() == null;
        if (sinRevisor && parametroService.isVbActivo()) {
            informe.setEstado(EstadoInforme.EN_VISTO_BUENO);
        } else if (sinRevisor) {
            informe.setEstado(EstadoInforme.EN_REVISION);
        }
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);
        eventoInformeService.publicar(TipoEvento.INFORME_ENVIADO, informe, null);
        return detalle;
    }

    /**
     * ENVIADO -> EN_VISTO_BUENO | EN_REVISION (aprobacion del revisor).
     * I3: publica REVISION_APROBADA al supervisor.
     * I9: el estado destino depende de VB_ACTIVO:
     *     - VB activo  → EN_VISTO_BUENO (cola del equipo administrativo)
     *     - VB inactivo → EN_REVISION   (flujo anterior directo al supervisor)
     */
    public InformeDetalleDto aprobarRevision(Long informeId, String revisorEmail, String observacionOpcional) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.ENVIADO);
        assertAssignedEmail(informe.getContrato().getRevisor(), revisorEmail);
        if (hasText(observacionOpcional)) {
            observacionService.registrar(informe, RolObservacion.REVISOR, observacionOpcional);
        }
        // I9: bifurcacion segun flag VB_ACTIVO
        EstadoInforme estadoDestino = parametroService.isVbActivo()
            ? EstadoInforme.EN_VISTO_BUENO
            : EstadoInforme.EN_REVISION;
        informe.setEstado(estadoDestino);
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);
        eventoInformeService.publicar(TipoEvento.REVISION_APROBADA, informe, observacionOpcional);
        return detalle;
    }

    /**
     * ENVIADO -> DEVUELTO (devolucion por revision).
     * I3: publica REVISION_DEVUELTA al contratista.
     */
    public InformeDetalleDto devolverRevision(Long informeId, String revisorEmail, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.ENVIADO);
        assertAssignedEmail(informe.getContrato().getRevisor(), revisorEmail);
        requireObservation(observacion);
        observacionService.registrar(informe, RolObservacion.REVISOR, observacion);
        informe.setEstado(EstadoInforme.DEVUELTO);
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);
        eventoInformeService.publicar(TipoEvento.REVISION_DEVUELTA, informe, observacion);
        return detalle;
    }

    /** Alias de devolverRevision para coherencia con el plan. */
    public InformeDetalleDto devolverEnRevision(Long informeId, String revisorEmail, String observacion) {
        return devolverRevision(informeId, revisorEmail, observacion);
    }

    /**
     * EN_REVISION -> APROBADO (aprobacion final del supervisor).
     *
     * <p>Orden transaccional (spec I3 §6):</p>
     * <ol>
     *   <li>Validar permisos y estado.</li>
     *   <li>Generar PDF (puede lanzar FIRMA_REQUERIDA o PDF_GENERACION_FALLIDA).</li>
     *   <li>Solo si PDF exitoso: persistir estado APROBADO y fechaAprobacion.</li>
     *   <li>Publicar INFORME_APROBADO (no critico).</li>
     * </ol>
     */
    public InformeDetalleDto aprobar(Long informeId, String supervisorEmail) {
        Informe informe = informeService.findActiveInforme(informeId);
        boolean sinRevisor = informe.getContrato().getRevisor() == null;
        if (sinRevisor) {
            assertState(informe, EstadoInforme.ENVIADO, EstadoInforme.EN_REVISION);
        } else {
            assertState(informe, EstadoInforme.EN_REVISION);
        }
        assertAssignedEmail(informe.getContrato().getSupervisor(), supervisorEmail);

        LocalDateTime fechaAprobacion = LocalDateTime.now();
        informe.setFechaAprobacion(fechaAprobacion);

        // Paso 2: generar PDF antes de cambiar estado (puede fallar)
        try {
            pdfInformeService.generarYPersistir(informe);
        } catch (RuntimeException e) {
            informe.setFechaAprobacion(null);
            throw e;
        }

        // Paso 3: persistir estado solo si PDF exitoso
        informe.setEstado(EstadoInforme.APROBADO);
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);

        // Paso 4: efectos secundarios no criticos
        eventoInformeService.publicar(TipoEvento.INFORME_APROBADO, informe, null);
        try {
            emailNotificacionService.notificarAprobacionAdmin(informe);
        } catch (Exception e) {
            // El fallo de email no revierte la aprobacion — se registra para soporte
            org.slf4j.LoggerFactory.getLogger(InformeEstadoService.class)
                .error("Error al notificar aprobacion de informe id={}: {}", informe.getId(), e.getMessage(), e);
        }
        return detalle;
    }

    /**
     * EN_REVISION -> DEVUELTO (devolucion por el supervisor).
     * I3: publica INFORME_DEVUELTO al contratista.
     */
    public InformeDetalleDto devolver(Long informeId, String supervisorEmail, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        boolean sinRevisor = informe.getContrato().getRevisor() == null;
        if (sinRevisor) {
            assertState(informe, EstadoInforme.ENVIADO, EstadoInforme.EN_REVISION);
        } else {
            assertState(informe, EstadoInforme.EN_REVISION);
        }
        assertAssignedEmail(informe.getContrato().getSupervisor(), supervisorEmail);
        requireObservation(observacion);
        observacionService.registrar(informe, RolObservacion.SUPERVISOR, observacion);
        informe.setEstado(EstadoInforme.DEVUELTO);
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);
        eventoInformeService.publicar(TipoEvento.INFORME_DEVUELTO, informe, observacion);
        return detalle;
    }

    /** Alias de devolver para coherencia con el plan. */
    public InformeDetalleDto devolverFinal(Long informeId, String supervisorEmail, String observacion) {
        return devolver(informeId, supervisorEmail, observacion);
    }

    // ── I9: Acciones del Actor Administrativo ────────────────────────────────

    /**
     * EN_VISTO_BUENO -> EN_REVISION (Dar Visto Bueno).
     * I9: actor ADMINISTRATIVO aprueba el informe y lo pasa al Supervisor.
     * Observacion opcional; accion = VISTO_BUENO para trazabilidad.
     */
    public InformeDetalleDto darVistosBueno(Long informeId, String observacionOpcional) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_VISTO_BUENO);
        if (hasText(observacionOpcional)) {
            observacionService.registrarConAccion(
                informe, RolObservacion.ADMINISTRATIVO, observacionOpcional, "VISTO_BUENO");
        }
        informe.setEstado(EstadoInforme.EN_REVISION);
        return saveAndBuildDetalle(informe);
    }

    /**
     * EN_VISTO_BUENO -> EN_REVISION (Escalar al Supervisor).
     * I9: actor ADMINISTRATIVO escala el informe directamente al Supervisor.
     * Observacion recomendada pero no bloqueante; accion = ESCALACION para trazabilidad.
     */
    public InformeDetalleDto escalar(Long informeId, String observacionRecomendada) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_VISTO_BUENO);
        observacionService.registrarConAccion(
            informe, RolObservacion.ADMINISTRATIVO, observacionRecomendada, "ESCALACION");
        informe.setEstado(EstadoInforme.EN_REVISION);
        return saveAndBuildDetalle(informe);
    }

    /**
     * EN_VISTO_BUENO -> DEVUELTO (Devolver al Contratista desde Visto Bueno).
     * I9: actor ADMINISTRATIVO devuelve el informe al Contratista.
     * Observacion obligatoria; accion = DEVOLUCION para trazabilidad.
     */
    public InformeDetalleDto devolverDesdeVistoBueno(Long informeId, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_VISTO_BUENO);
        requireObservation(observacion);
        observacionService.registrarConAccion(
            informe, RolObservacion.ADMINISTRATIVO, observacion, "DEVOLUCION");
        informe.setEstado(EstadoInforme.DEVUELTO);
        return saveAndBuildDetalle(informe);
    }

    private InformeDetalleDto saveAndBuildDetalle(Informe informe) {
        Informe saved = informeRepository.save(informe);
        return informeService.buildDetalle(saved);
    }

    private void assertSoporteUrlPorActividad(List<ActividadInforme> actividades) {
        for (ActividadInforme actividad : actividades) {
            boolean tieneSoporteUrl = soporteRepository.existsByActividadIdAndTipoAndActivoTrue(
                actividad.getId(),
                TipoSoporte.URL
            );
            if (!tieneSoporteUrl) {
                throw new SigconBusinessException(
                    ErrorCode.SOPORTE_INVALIDO,
                    "Cada actividad reportada debe tener un soporte URL",
                    HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private static void assertState(Informe informe, EstadoInforme... allowed) {
        if (informe.getEstado() == EstadoInforme.APROBADO) {
            throw new SigconBusinessException(
                ErrorCode.INFORME_NO_EDITABLE,
                "El informe aprobado es terminal",
                HttpStatus.CONFLICT
            );
        }
        for (EstadoInforme e : allowed) {
            if (informe.getEstado() == e) return;
        }
        throw new SigconBusinessException(
            ErrorCode.TRANSICION_INVALIDA,
            "Transición de estado inválida",
            HttpStatus.CONFLICT
        );
    }

    private static void assertAssignedEmail(Usuario usuario, String principalEmail) {
        if (usuario == null || !sameEmail(usuario.getEmail(), principalEmail)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "Acceso denegado",
                HttpStatus.FORBIDDEN
            );
        }
    }

    private static void requireObservation(String observacion) {
        if (!hasText(observacion)) {
            throw new SigconBusinessException(
                ErrorCode.OBSERVACION_REQUERIDA,
                "La observación es obligatoria para esta acción",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private static boolean sameEmail(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
