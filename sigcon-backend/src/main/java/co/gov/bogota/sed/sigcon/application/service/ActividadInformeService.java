package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeRequest;
import co.gov.bogota.sed.sigcon.application.mapper.ActividadInformeMapper;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@Transactional
public class ActividadInformeService {

    private static final BigDecimal MIN_PORCENTAJE = BigDecimal.ZERO;
    private static final BigDecimal MAX_PORCENTAJE = new BigDecimal("100");

    private final ActividadInformeRepository actividadRepository;
    private final ObligacionRepository obligacionRepository;
    private final SoporteAdjuntoRepository soporteRepository;
    private final InformeService informeService;
    private final CurrentUserService currentUserService;
    private final ActividadInformeMapper actividadMapper;

    public ActividadInformeService(
        ActividadInformeRepository actividadRepository,
        ObligacionRepository obligacionRepository,
        SoporteAdjuntoRepository soporteRepository,
        InformeService informeService,
        CurrentUserService currentUserService,
        ActividadInformeMapper actividadMapper
    ) {
        this.actividadRepository = actividadRepository;
        this.obligacionRepository = obligacionRepository;
        this.soporteRepository = soporteRepository;
        this.informeService = informeService;
        this.currentUserService = currentUserService;
        this.actividadMapper = actividadMapper;
    }

    public ActividadInformeDto crear(Long informeId, ActividadInformeRequest request) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        validatePorcentaje(request.getPorcentaje());

        Obligacion obligacion = obligacionRepository.findById(request.getIdObligacion())
            .filter(o -> Boolean.TRUE.equals(o.getActivo())
                && o.getContrato() != null
                && o.getContrato().getId().equals(informe.getContrato().getId()))
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "La obligación no pertenece al contrato del informe",
                HttpStatus.BAD_REQUEST
            ));

        ActividadInforme actividad = new ActividadInforme();
        actividad.setInforme(informe);
        actividad.setObligacion(obligacion);
        actividad.setDescripcion(request.getDescripcion());
        actividad.setPorcentaje(request.getPorcentaje());
        actividad.setActivo(true);
        ActividadInforme saved = actividadRepository.save(actividad);
        return actividadMapper.toDto(saved, Collections.emptyList());
    }

    public ActividadInformeDto actualizar(Long informeId, Long actividadId, ActividadInformeRequest request) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        validatePorcentaje(request.getPorcentaje());

        ActividadInforme actividad = findActividadOfInforme(actividadId, informe.getId());
        actividad.setDescripcion(request.getDescripcion());
        actividad.setPorcentaje(request.getPorcentaje());
        ActividadInforme saved = actividadRepository.save(actividad);
        return actividadMapper.toDto(saved, soporteRepository.findByActividadIdAndActivoTrue(saved.getId()));
    }

    public void eliminar(Long informeId, Long actividadId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        ActividadInforme actividad = findActividadOfInforme(actividadId, informe.getId());
        actividad.setActivo(false);
        actividadRepository.save(actividad);
    }

    private ActividadInforme findActividadOfInforme(Long actividadId, Long informeId) {
        ActividadInforme actividad = actividadRepository.findByIdAndActivoTrue(actividadId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Actividad no encontrada",
                HttpStatus.NOT_FOUND
            ));
        if (actividad.getInforme() == null || !actividad.getInforme().getId().equals(informeId)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "La actividad no pertenece al informe",
                HttpStatus.FORBIDDEN
            );
        }
        return actividad;
    }

    private static void validatePorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null
            || porcentaje.compareTo(MIN_PORCENTAJE) < 0
            || porcentaje.compareTo(MAX_PORCENTAJE) > 0) {
            throw new SigconBusinessException(
                ErrorCode.PORCENTAJE_INVALIDO,
                "El porcentaje debe estar entre 0 y 100",
                HttpStatus.BAD_REQUEST
            );
        }
    }
}
