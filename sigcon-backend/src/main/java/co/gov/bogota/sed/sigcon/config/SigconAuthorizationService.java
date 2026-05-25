package co.gov.bogota.sed.sigcon.config;

import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("sigconAuthorization")
public class SigconAuthorizationService {

    private final UsuarioRepository usuarioRepository;

    public SigconAuthorizationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return true;
        }
        return usuarioRepository.findByEmailAndActivoTrue(authentication.getName())
            .map(this::isDualAdmin)
            .orElse(false);
    }

    private boolean isDualAdmin(Usuario usuario) {
        return usuario.getRol() == RolUsuario.CONTRATISTA && Boolean.TRUE.equals(usuario.getEsAdmin());
    }
}
