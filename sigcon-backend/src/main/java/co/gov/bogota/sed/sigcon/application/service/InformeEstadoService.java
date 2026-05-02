package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Maquina de estados del informe.
 * I3: genera PDF y publica eventos de notificacion en cada transicion.
 */
@Service
@Transactional
public class InformeEstadoService {

    private final InformeRepository informeRepository;
    private final ActividadInformeRepository actividadRepository;
    private final InformeService informeService;
    private final ObservacionService observacionService;
    private final PdfInformeService pdfInformeService;
    private final EventoInformeService eventoInformeService;

    public InformeEstadoService(
        InformeRepository informeRepository,
        ActividadInformeRepository actividadRepository,
        InformeService informeService,
        ObservacionService observacionService,
        PdfInformeService pdfInformeService,
        EventoInformeService eventoInformeService
    ) {
        this.informeRepository = informeRepository;
        this.actividadRepository = actividadRepository;
        this.informeService = informeService;
        this.observacionService = observacionService;
        this.pdfInformeService = pdfInformeService;
        this.eventoInformeService = eventoInformeService;
    }

    /**
     * BORRADOR | DEVUELTO -> ENVIADO
     * Requiere al menos una actividad.
     * I3: publica INFORME_ENVIADO al revisor.
     */
    public InformeDetalleDto enviar(Long informeId, String contratistaEmail) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.BORRADOR, EstadoInforme.DEVUELTO);
        assertAssignedEmail(informe.getContrato().getContratista(), contratistaEmail);
        Integer actividades = actividadRepository.countByInformeIdAndActivoTrue(informe.getId());
        if (actividades == null || actividades.intValue() == 0) {
            throw new SigconBusinessException(
                ErrorCode.ACTIVIDAD_REQUERIDA,
                "El informe debe tener al menos una actividad para ser enviado",
                HttpStatus.BAD_REQUEST
            );
        }
        informe.setEstado(EstadoInforme.ENVIADO);
        informe.setFechaUltimoEnvio(LocalDateTime.now());
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);
        eventoInformeService.publicar(TipoEvento.INFORME_ENVIADO, informe, null);
        return detalle;
    }

    /**
     * ENVIADO -> EN_REVISION (aprobacion del revisor).
     * I3: publica REVISION_APROBADA al supervisor.
     */
    public InformeDetalleDto aprobarRevision(Long informeId, String revisorEmail, String observacionOpcional) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.ENVIADO);
        assertAssignedEmail(informe.getContrato().getRevisor(), revisorEmail);
        if (hasText(observacionOpcional)) {
            observacionService.registrar(informe, RolObservacion.REVISOR, observacionOpcional);
        }
        informe.setEstado(EstadoInforme.EN_REVISION);
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
        assertState(informe, EstadoInforme.EN_REVISION);
        assertAssignedEmail(informe.getContrato().getSupervisor(), supervisorEmail);

        // Paso 2: generar PDF antes de cambiar estado (puede fallar)
        pdfInformeService.generarYPersistir(informe);

        // Paso 3: persistir estado solo si PDF exitoso
        informe.setEstado(EstadoInforme.APROBADO);
        informe.setFechaAprobacion(LocalDateTime.now());
        InformeDetalleDto detalle = saveAndBuildDetalle(informe);

        // Paso 4: efectos secundarios no criticos
        eventoInformeService.publicar(TipoEvento.INFORME_APROBADO, informe, null);
        return detalle;
    }

    /**
     * EN_REVISION -> DEVUELTO (devolucion por el supervisor).
     * I3: publica INFORME_DEVUELTO al contratista.
     */
    public InformeDetalleDto devolver(Long informeId, String supervisorEmail, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_REVISION);
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

    private InformeDetalleDto saveAndBuildDetalle(Informe informe) {
        Informe saved = informeRepository.save(informe);
        return informeService.buildDetalle(saved);
    }

    private static void assertState(Informe informe, EstadoInforme expected) {
        if (informe.getEstado() == EstadoInforme.APROBADO) {
            throw new SigconBusinessException(
                ErrorCode.INFORME_NO_EDITABLE,
                "El informe aprobado es terminal",
                HttpStatus.CONFLICT
            );
        }
        if (informe.getEstado() != expected) {
            throw new SigconBusinessException(
                ErrorCode.TRANSICION_INVALIDA,
                "Transición de estado inválida",
                HttpStatus.CONFLICT
            );
        }
    }

    private static void assertState(Informe informe, EstadoInforme expectedOne, EstadoInforme expectedTwo) {
        if (informe.getEstado() == EstadoInforme.APROBADO) {
            throw new SigconBusinessException(
                ErrorCode.INFORME_NO_EDITABLE,
                "El informe aprobado es terminal",
                HttpStatus.CONFLICT
            );
        }
        if (informe.getEstado() != expectedOne && informe.getEstado() != expectedTwo) {
            throw new SigconBusinessException(
                ErrorCode.TRANSICION_INVALIDA,
                "Transición de estado inválida",
                HttpStatus.CONFLICT
            );
        }
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
