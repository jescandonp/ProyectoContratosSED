package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.ObservacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import org.springframework.stereotype.Component;

@Component
public class ObservacionMapper {

    public ObservacionDto toDto(Observacion entity) {
        ObservacionDto dto = new ObservacionDto();
        dto.setId(entity.getId());
        dto.setTexto(entity.getTexto());
        dto.setAutorRol(entity.getAutorRol());
        dto.setFecha(entity.getFecha());
        return dto;
    }
}
