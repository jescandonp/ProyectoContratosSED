package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionDto;
import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionesCountDto;
import co.gov.bogota.sed.sigcon.application.mapper.NotificacionMapper;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.NotificacionService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.NotificacionRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private CurrentUserService currentUserService;

    private NotificacionService notificacionService;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService(
            notificacionRepository, new NotificacionMapper(), currentUserService
        );
    }

    @Test
    void crearGuardaNotificacionConDatosCorrectos() {
        Usuario destinatario = usuario(3L, RolUsuario.REVISOR);
        Informe informe = informe(50L);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion result = notificacionService.crear(destinatario, TipoEvento.INFORME_ENVIADO, informe, "desc");

        assertThat(result.getUsuario()).isEqualTo(destinatario);
        assertThat(result.getTipoEvento()).isEqualTo(TipoEvento.INFORME_ENVIADO);
        assertThat(result.getInforme()).isEqualTo(informe);
        assertThat(result.isLeida()).isFalse();
        assertThat(result.getTitulo()).isNotEmpty();
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void contarNoLeidasSoloDelUsuarioActual() {
        Usuario usuario = usuario(2L, RolUsuario.CONTRATISTA);
        when(currentUserService.getCurrentUser()).thenReturn(usuario);
        when(notificacionRepository.countByUsuarioAndLeidaFalse(usuario)).thenReturn(3L);

        NotificacionesCountDto dto = notificacionService.contarNoLeidas();

        assertThat(dto.getCount()).isEqualTo(3L);
        verify(notificacionRepository).countByUsuarioAndLeidaFalse(usuario);
    }

    @Test
    void marcarLeidaActualizaEstado() {
        Usuario usuario = usuario(2L, RolUsuario.CONTRATISTA);
        Notificacion notif = notificacion(10L, usuario);
        when(currentUserService.getCurrentUser()).thenReturn(usuario);
        when(notificacionRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.of(notif));
        when(notificacionRepository.save(notif)).thenReturn(notif);

        NotificacionDto dto = notificacionService.marcarLeida(10L);

        assertThat(dto.isLeida()).isTrue();
        verify(notificacionRepository).save(notif);
    }

    @Test
    void marcarLeidaDeOtroUsuarioLanzaAccesoDenegado() {
        Usuario usuario = usuario(2L, RolUsuario.CONTRATISTA);
        when(currentUserService.getCurrentUser()).thenReturn(usuario);
        when(notificacionRepository.findByIdAndUsuario(10L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(10L))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOTIFICACION_NO_ENCONTRADA));
    }

    @Test
    void listarRetornaNotificacionesDelUsuario() {
        Usuario usuario = usuario(2L, RolUsuario.CONTRATISTA);
        Notificacion notif = notificacion(10L, usuario);
        when(currentUserService.getCurrentUser()).thenReturn(usuario);
        when(notificacionRepository.findByUsuarioOrderByFechaDesc(eq(usuario), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(notif)));

        Page<NotificacionDto> page = notificacionService.listar(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(10L);
    }

    private static Usuario usuario(Long id, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@educacionbogota.edu.co");
        u.setNombre("Usuario " + id);
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private static Informe informe(Long id) {
        Informe i = new Informe();
        i.setId(id);
        Contrato c = new Contrato();
        c.setId(1L);
        i.setContrato(c);
        return i;
    }

    private static Notificacion notificacion(Long id, Usuario usuario) {
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setUsuario(usuario);
        n.setTitulo("Test");
        n.setDescripcion("Descripcion");
        n.setTipoEvento(TipoEvento.INFORME_ENVIADO);
        n.setLeida(false);
        return n;
    }
}
