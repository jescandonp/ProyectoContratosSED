package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.ObservacionDto;
import co.gov.bogota.sed.sigcon.application.mapper.ObservacionMapper;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ObservacionService {

    private final ObservacionRepository observacionRepository;
    private final ObservacionMapper observacionMapper;

    public ObservacionService(ObservacionRepository observacionRepository, ObservacionMapper observacionMapper) {
        this.observacionRepository = observacionRepository;
        this.observacionMapper = observacionMapper;
    }

    /**
     * Registra una observacion como parte de una transicion (DEVUELTO).
     * Llamado por InformeEstadoService (Task 5).
     */
    public Observacion registrar(Informe informe, RolObservacion autor, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.OBSERVACION_REQUERIDA,
                "La observación es obligatoria para esta acción",
                HttpStatus.BAD_REQUEST
            );
        }
        Observacion observacion = new Observacion();
        observacion.setInforme(informe);
        observacion.setTexto(texto.trim());
        observacion.setAutorRol(autor);
        observacion.setActivo(true);
        return observacionRepository.save(observacion);
    }

    @Transactional(readOnly = true)
    public List<ObservacionDto> listarPorInforme(Long informeId) {
        return observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(informeId)
            .stream().map(observacionMapper::toDto).collect(Collectors.toList());
    }
}
