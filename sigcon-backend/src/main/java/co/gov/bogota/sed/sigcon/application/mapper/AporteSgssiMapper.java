package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiDto;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import org.springframework.stereotype.Component;

@Component
public class AporteSgssiMapper {

    public AporteSgssiDto toDto(AporteSgssi entity) {
        AporteSgssiDto dto = new AporteSgssiDto();
        dto.setId(entity.getId());
        dto.setItem(entity.getItem());
        dto.setFechaPago(entity.getFechaPago());
        dto.setValorAportado(entity.getValorAportado());
        dto.setEntidad(entity.getEntidad());
        return dto;
    }
}
