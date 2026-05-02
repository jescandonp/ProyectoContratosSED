package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ActividadInformeMapper {

    private final SoporteAdjuntoMapper soporteAdjuntoMapper;

    public ActividadInformeMapper(SoporteAdjuntoMapper soporteAdjuntoMapper) {
        this.soporteAdjuntoMapper = soporteAdjuntoMapper;
    }

    public ActividadInformeDto toDto(ActividadInforme entity, List<SoporteAdjunto> soportes) {
        ActividadInformeDto dto = new ActividadInformeDto();
        dto.setId(entity.getId());
        if (entity.getObligacion() != null) {
            dto.setIdObligacion(entity.getObligacion().getId());
            dto.setOrdenObligacion(entity.getObligacion().getOrden());
            dto.setDescripcionObligacion(entity.getObligacion().getDescripcion());
        }
        dto.setDescripcion(entity.getDescripcion());
        dto.setPorcentaje(entity.getPorcentaje());
        dto.setSoportes(soportes == null
            ? java.util.Collections.emptyList()
            : soportes.stream().map(soporteAdjuntoMapper::toDto).collect(Collectors.toList()));
        return dto;
    }
}
