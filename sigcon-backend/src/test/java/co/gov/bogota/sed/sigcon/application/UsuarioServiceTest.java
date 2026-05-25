package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioDto;
import co.gov.bogota.sed.sigcon.application.dto.usuario.UsuarioRequest;
import co.gov.bogota.sed.sigcon.application.mapper.UsuarioMapper;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.UsuarioService;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private DocumentStorageService documentStorageService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
            usuarioRepository,
            currentUserService,
            documentStorageService,
            new UsuarioMapper()
        );
    }

    @Test
    void crearUsuarioDefaultsResponsableIvaToFalseWhenRequestOmitsValue() {
        UsuarioRequest request = usuarioRequest("contratista@sed.gov.co");
        when(usuarioRepository.findByEmailAndActivoTrue("contratista@sed.gov.co")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });

        UsuarioDto result = usuarioService.crearUsuario(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getResponsableIva()).isFalse();
        assertThat(result.getResponsableIva()).isFalse();
    }

    @Test
    void crearUsuarioPersistsResponsableIvaWhenRequestSetsTrue() {
        UsuarioRequest request = usuarioRequest("responsable@sed.gov.co");
        request.setResponsableIva(true);
        request.setEsAdmin(true);
        when(usuarioRepository.findByEmailAndActivoTrue("responsable@sed.gov.co")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(11L);
            return usuario;
        });

        UsuarioDto result = usuarioService.crearUsuario(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getResponsableIva()).isTrue();
        assertThat(captor.getValue().getEsAdmin()).isTrue();
        assertThat(result.getResponsableIva()).isTrue();
        assertThat(result.getEsAdmin()).isTrue();
    }

    @Test
    void crearUsuarioClearsEsAdminWhenRoleIsNotContractor() {
        UsuarioRequest request = usuarioRequest("revisor-admin@sed.gov.co");
        request.setRol(RolUsuario.REVISOR);
        request.setEsAdmin(true);
        when(usuarioRepository.findByEmailAndActivoTrue("revisor-admin@sed.gov.co")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario usuario = inv.getArgument(0);
            usuario.setId(12L);
            return usuario;
        });

        UsuarioDto result = usuarioService.crearUsuario(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getEsAdmin()).isFalse();
        assertThat(result.getEsAdmin()).isFalse();
    }

    private UsuarioRequest usuarioRequest(String email) {
        UsuarioRequest request = new UsuarioRequest();
        request.setEmail(email);
        request.setNombre("Contratista Prueba");
        request.setCargo("Contratista");
        request.setRol(RolUsuario.CONTRATISTA);
        return request;
    }
}
