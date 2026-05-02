package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import org.springframework.stereotype.Component;

@Component
public class NotificacionMapper {

    public NotificacionDto toDto(Notificacion n) {
        NotificacionDto dto = new NotificacionDto();
        dto.setId(n.getId());
        dto.setTitulo(n.getTitulo());
        dto.setDescripcion(n.getDescripcion());
        dto.setTipoEvento(n.getTipoEvento());
        dto.setIdInforme(n.getInforme() != null ? n.getInforme().getId() : null);
        dto.setLeida(n.isLeida());
        dto.setFecha(n.getFecha());
        return dto;
    }
}
