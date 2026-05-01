package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.usuario.EstadoUsuarioRequest;
import co.gov.bogota.sed.sigcon.application.dto.usuario.PerfilUpdateRequest;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioRequest;
import co.gov.bogota.sed.sigcon.application.service.UsuarioService;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioDto obtenerPerfilActual() {
        return usuarioService.obtenerPerfilActual();
    }

    @PutMapping("/me")
    public UsuarioDto actualizarPerfilActual(@Valid @RequestBody PerfilUpdateRequest request) {
        return usuarioService.actualizarPerfilActual(request);
    }

    @PostMapping("/me/firma")
    public UsuarioDto actualizarFirmaActual(@RequestPart("file") MultipartFile file) throws IOException {
        return usuarioService.actualizarFirmaActual(file);
    }

    @GetMapping
    public Page<UsuarioDto> listarUsuarios(@RequestParam(required = false) RolUsuario rol, Pageable pageable) {
        if (rol == null) {
            return usuarioService.listarUsuarios(pageable);
        }
        List<UsuarioDto> usuarios = usuarioService.listarUsuariosPorRol(rol);
        return new PageImpl<>(usuarios, pageable, usuarios.size());
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(request));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.actualizarUsuario(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest request) {
        usuarioService.cambiarEstado(id, request.getActivo());
        return ResponseEntity.noContent().build();
    }
}
