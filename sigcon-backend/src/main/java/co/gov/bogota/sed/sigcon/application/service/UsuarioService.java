package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.usuario.PerfilUpdateRequest;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioRequest;
import co.gov.bogota.sed.sigcon.application.mapper.UsuarioMapper;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CurrentUserService currentUserService;
    private final DocumentStorageService documentStorageService;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        CurrentUserService currentUserService,
        DocumentStorageService documentStorageService,
        UsuarioMapper usuarioMapper
    ) {
        this.usuarioRepository = usuarioRepository;
        this.currentUserService = currentUserService;
        this.documentStorageService = documentStorageService;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPerfilActual() {
        return usuarioMapper.toDto(currentUserService.getCurrentUser());
    }

    public UsuarioDto actualizarPerfilActual(PerfilUpdateRequest request) {
        Usuario usuario = currentUserService.getCurrentUser();
        usuario.setNombre(request.getNombre());
        usuario.setCargo(request.getCargo());
        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    public UsuarioDto actualizarFirmaActual(MultipartFile file) throws IOException {
        Usuario usuario = currentUserService.getCurrentUser();
        usuario.setFirmaImagen(documentStorageService.storeSignature(usuario.getId(), file));
        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDto> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findByActivoTrue(pageable).map(usuarioMapper::toDto);
    }

    @Transactional(readOnly = true)
    public java.util.List<UsuarioDto> listarUsuariosPorRol(RolUsuario rol) {
        return usuarioRepository.findByRolAndActivoTrue(rol)
            .stream()
            .map(usuarioMapper::toDto)
            .collect(java.util.stream.Collectors.toList());
    }

    public UsuarioDto actualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = findActiveUsuario(id);
        usuario.setNombre(request.getNombre());
        usuario.setCargo(request.getCargo());
        usuario.setRol(request.getRol());
        return usuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    public void cambiarEstado(Long id, boolean activo) {
        Usuario usuario = findActiveUsuario(id);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    private Usuario findActiveUsuario(Long id) {
        return usuarioRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.USUARIO_NO_ENCONTRADO,
                "Usuario no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }
}
