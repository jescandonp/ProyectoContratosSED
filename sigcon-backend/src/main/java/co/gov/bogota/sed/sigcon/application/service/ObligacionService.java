package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionDto;
import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionRequest;
import co.gov.bogota.sed.sigcon.application.mapper.ObligacionMapper;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ObligacionService {

    private final ContratoRepository contratoRepository;
    private final ObligacionRepository obligacionRepository;
    private final ObligacionMapper obligacionMapper;

    public ObligacionService(
        ContratoRepository contratoRepository,
        ObligacionRepository obligacionRepository,
        ObligacionMapper obligacionMapper
    ) {
        this.contratoRepository = contratoRepository;
        this.obligacionRepository = obligacionRepository;
        this.obligacionMapper = obligacionMapper;
    }

    @Transactional(readOnly = true)
    public List<ObligacionDto> listarPorContrato(Long contratoId) {
        ensureContratoExists(contratoId);
        return obligacionRepository.findByContratoIdAndActivoTrueOrderByOrdenAsc(contratoId)
            .stream()
            .map(obligacionMapper::toDto)
            .collect(Collectors.toList());
    }

    public ObligacionDto crear(Long contratoId, ObligacionRequest request) {
        Contrato contrato = ensureContratoExists(contratoId);
        Obligacion obligacion = new Obligacion();
        obligacion.setContrato(contrato);
        applyRequest(obligacion, request);
        return obligacionMapper.toDto(obligacionRepository.save(obligacion));
    }

    public ObligacionDto actualizar(Long contratoId, Long id, ObligacionRequest request) {
        ensureContratoExists(contratoId);
        Obligacion obligacion = findActiveObligacion(id);
        applyRequest(obligacion, request);
        return obligacionMapper.toDto(obligacionRepository.save(obligacion));
    }

    public void eliminar(Long contratoId, Long id) {
        ensureContratoExists(contratoId);
        Obligacion obligacion = findActiveObligacion(id);
        obligacion.setActivo(false);
        obligacionRepository.save(obligacion);
    }

    private void applyRequest(Obligacion obligacion, ObligacionRequest request) {
        obligacion.setDescripcion(request.getDescripcion());
        obligacion.setOrden(request.getOrden());
        obligacion.setActivo(true);
    }

    private Contrato ensureContratoExists(Long contratoId) {
        return contratoRepository.findByIdAndActivoTrue(contratoId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ENCONTRADO,
                "Contrato no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }

    private Obligacion findActiveObligacion(Long id) {
        return obligacionRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ENCONTRADO,
                "Obligación no encontrada",
                HttpStatus.NOT_FOUND
            ));
    }
}
