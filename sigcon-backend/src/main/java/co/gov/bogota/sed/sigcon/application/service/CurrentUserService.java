package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SigconBusinessException(ErrorCode.ACCESO_DENEGADO, "Usuario no autenticado", HttpStatus.FORBIDDEN);
        }
        return usuarioRepository.findByEmailAndActivoTrue(authentication.getName())
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.USUARIO_NO_ENCONTRADO,
                "Usuario autenticado no existe en SIGCON",
                HttpStatus.NOT_FOUND
            ));
    }
}
