package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioDto toDto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        UsuarioDto dto = new UsuarioDto();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNombre(usuario.getNombre());
        dto.setCargo(usuario.getCargo());
        dto.setRol(usuario.getRol());
        dto.setFirmaImagen(usuario.getFirmaImagen());
        dto.setActivo(usuario.getActivo());
        return dto;
    }
}
