package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import org.springframework.stereotype.Component;

@Component
public class ObligacionMapper {

    public ObligacionDto toDto(Obligacion obligacion) {
        if (obligacion == null) {
            return null;
        }
        ObligacionDto dto = new ObligacionDto();
        dto.setId(obligacion.getId());
        dto.setDescripcion(obligacion.getDescripcion());
        dto.setOrden(obligacion.getOrden());
        return dto;
    }
}
