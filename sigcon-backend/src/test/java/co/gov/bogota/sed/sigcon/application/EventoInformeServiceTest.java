package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.EmailNotificacionService;
import co.gov.bogota.sed.sigcon.application.service.EventoInformeService;
import co.gov.bogota.sed.sigcon.application.service.NotificacionService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoInformeServiceTest {

    @Mock private NotificacionService notificacionService;
    @Mock private EmailNotificacionService emailService;

    private EventoInformeService eventoService;

    @BeforeEach
    void setUp() {
        eventoService = new EventoInformeService(notificacionService, emailService);
    }

    @Test
    void informeEnviadoCreaNotificacionParaRevisor() {
        Usuario revisor = usuario(3L, RolUsuario.REVISOR);
        Informe informe = informe(50L, usuario(2L, RolUsuario.CONTRATISTA), revisor, usuario(4L, RolUsuario.SUPERVISOR));
        when(notificacionService.crear(any(), any(), any(), any())).thenReturn(new Notificacion());

        eventoService.publicar(TipoEvento.INFORME_ENVIADO, informe, null);

        verify(notificacionService).crear(eq(revisor), eq(TipoEvento.INFORME_ENVIADO), eq(informe), any());
        verify(emailService).enviar(eq(revisor), eq(TipoEvento.INFORME_ENVIADO), eq(50L), any());
    }

    @Test
    void informeAprobadoCreaNotificacionParaContratista() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contratista, usuario(3L, RolUsuario.REVISOR), usuario(4L, RolUsuario.SUPERVISOR));
        when(notificacionService.crear(any(), any(), any(), any())).thenReturn(new Notificacion());

        eventoService.publicar(TipoEvento.INFORME_APROBADO, informe, null);

        verify(notificacionService).crear(eq(contratista), eq(TipoEvento.INFORME_APROBADO), eq(informe), any());
        verify(emailService).enviar(eq(contratista), eq(TipoEvento.INFORME_APROBADO), eq(50L), any());
    }

    @Test
    void emailSimuladoNoBloquea() {
        // EmailNotificacionService con enabled=false debe ejecutarse sin lanzar excepcion
        // Este test verifica que el metodo de email es llamado (la simulacion es interna al servicio)
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contratista, usuario(3L, RolUsuario.REVISOR), usuario(4L, RolUsuario.SUPERVISOR));
        when(notificacionService.crear(any(), any(), any(), any())).thenReturn(new Notificacion());

        // No debe lanzar ninguna excepcion aunque emailService este mockeado
        eventoService.publicar(TipoEvento.REVISION_DEVUELTA, informe, "Observacion de prueba");

        verify(emailService).enviar(eq(contratista), eq(TipoEvento.REVISION_DEVUELTA), eq(50L), any());
    }

    @Test
    void errorEnNotificacionNoPropaganExcepcion() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contratista, usuario(3L, RolUsuario.REVISOR), usuario(4L, RolUsuario.SUPERVISOR));
        when(notificacionService.crear(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("BD caida"));

        // No debe propagar la excepcion — efectos secundarios no son criticos
        eventoService.publicar(TipoEvento.INFORME_APROBADO, informe, null);
        // Si llego aqui sin excepcion, el test pasa
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

    private static Informe informe(Long id, Usuario contratista, Usuario revisor, Usuario supervisor) {
        Contrato c = new Contrato();
        c.setId(1L);
        c.setNumero("OPS-2026-001");
        c.setObjeto("Objeto");
        c.setTipo(TipoContrato.OPS);
        c.setValorTotal(BigDecimal.valueOf(18000000));
        c.setFechaInicio(LocalDate.of(2026, 1, 15));
        c.setFechaFin(LocalDate.of(2026, 12, 31));
        c.setEstado(EstadoContrato.EN_EJECUCION);
        c.setContratista(contratista);
        c.setRevisor(revisor);
        c.setSupervisor(supervisor);
        c.setActivo(true);

        Informe i = new Informe();
        i.setId(id);
        i.setNumero(1);
        i.setContrato(c);
        i.setFechaInicio(LocalDate.of(2026, 1, 1));
        i.setFechaFin(LocalDate.of(2026, 1, 31));
        i.setEstado(EstadoInforme.EN_REVISION);
        i.setActivo(true);
        return i;
    }
}
