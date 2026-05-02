package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteAdjuntoDto;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import org.springframework.stereotype.Component;

@Component
public class SoporteAdjuntoMapper {

    public SoporteAdjuntoDto toDto(SoporteAdjunto entity) {
        SoporteAdjuntoDto dto = new SoporteAdjuntoDto();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setNombre(entity.getNombre());
        dto.setReferencia(entity.getReferencia());
        return dto;
    }
}
