package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class InformeEstadoService {

    private final InformeRepository informeRepository;
    private final ActividadInformeRepository actividadRepository;
    private final InformeService informeService;
    private final ObservacionService observacionService;

    public InformeEstadoService(
        InformeRepository informeRepository,
        ActividadInformeRepository actividadRepository,
        InformeService informeService,
        ObservacionService observacionService
    ) {
        this.informeRepository = informeRepository;
        this.actividadRepository = actividadRepository;
        this.informeService = informeService;
        this.observacionService = observacionService;
    }

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
        return saveAndBuildDetalle(informe);
    }

    public InformeDetalleDto aprobarRevision(Long informeId, String revisorEmail, String observacionOpcional) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.ENVIADO);
        assertAssignedEmail(informe.getContrato().getRevisor(), revisorEmail);
        if (hasText(observacionOpcional)) {
            observacionService.registrar(informe, RolObservacion.REVISOR, observacionOpcional);
        }
        informe.setEstado(EstadoInforme.EN_REVISION);
        return saveAndBuildDetalle(informe);
    }

    public InformeDetalleDto devolverRevision(Long informeId, String revisorEmail, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.ENVIADO);
        assertAssignedEmail(informe.getContrato().getRevisor(), revisorEmail);
        requireObservation(observacion);
        observacionService.registrar(informe, RolObservacion.REVISOR, observacion);
        informe.setEstado(EstadoInforme.DEVUELTO);
        return saveAndBuildDetalle(informe);
    }

    public InformeDetalleDto devolverEnRevision(Long informeId, String revisorEmail, String observacion) {
        return devolverRevision(informeId, revisorEmail, observacion);
    }

    public InformeDetalleDto aprobar(Long informeId, String supervisorEmail) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_REVISION);
        assertAssignedEmail(informe.getContrato().getSupervisor(), supervisorEmail);
        informe.setEstado(EstadoInforme.APROBADO);
        informe.setFechaAprobacion(LocalDateTime.now());
        informe.setPdfRuta(null);
        return saveAndBuildDetalle(informe);
    }

    public InformeDetalleDto devolver(Long informeId, String supervisorEmail, String observacion) {
        Informe informe = informeService.findActiveInforme(informeId);
        assertState(informe, EstadoInforme.EN_REVISION);
        assertAssignedEmail(informe.getContrato().getSupervisor(), supervisorEmail);
        requireObservation(observacion);
        observacionService.registrar(informe, RolObservacion.SUPERVISOR, observacion);
        informe.setEstado(EstadoInforme.DEVUELTO);
        return saveAndBuildDetalle(informe);
    }

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
