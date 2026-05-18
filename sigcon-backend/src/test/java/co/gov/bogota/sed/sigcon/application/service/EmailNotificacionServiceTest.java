package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.config.MailProperties;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailNotificacionServiceTest {

    @Test
    void obtenerTokenEnviaFormUrlEncodedSeguroParaSecretosConCaracteresReservados() {
        MailProperties props = mailProperties();
        EmailNotificacionService service = new EmailNotificacionService(props);
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://login.microsoftonline.com/tenant-123/oauth2/v2.0/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("client_id=client%2Bid")))
            .andExpect(content().string(containsString("client_secret=a%2Bb%26c%3Dd")))
            .andRespond(withSuccess("{\"access_token\":\"token-123\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/sigcon@educacionbogota.edu.co/sendMail"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        service.enviar(usuario(), TipoEvento.INFORME_APROBADO, 50L, "Informe aprobado");

        server.verify();
    }

    // -----------------------------------------------------------------------
    // T7 — notificarAprobacionAdmin: solo admin configurable (el email al contratista lo maneja publicar())
    // -----------------------------------------------------------------------

    @Test
    void notificarAprobacionSimulaEnvioEnLocalDevSinAdmin() {
        MailProperties props = new MailProperties();
        props.setEnabled(false);
        // adminEmail no configurado
        EmailNotificacionService service = new EmailNotificacionService(props);

        Informe informe = sampleInforme();

        // No debe lanzar excepcion — solo log
        service.notificarAprobacionAdmin(informe);
    }

    @Test
    void notificarAprobacionSimulaEnvioEnLocalDevConAdmin() {
        MailProperties props = new MailProperties();
        props.setEnabled(false);
        props.setAdminEmail("admin@educacionbogota.edu.co");
        EmailNotificacionService service = new EmailNotificacionService(props);

        Informe informe = sampleInforme();

        // No debe lanzar excepcion — solo log x1 (solo admin — el email al contratista lo maneja publicar())
        service.notificarAprobacionAdmin(informe);
    }

    @Test
    void notificarAprobacionNoLanzaExcepcionSiEmailFalla() {
        // Simula fallo de red: enabled=true pero sin servidor real
        MailProperties props = mailProperties();
        EmailNotificacionService service = new EmailNotificacionService(props);
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        // El token falla — simula error de red
        server.expect(requestTo(containsString("/oauth2/v2.0/token")))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON)); // sin access_token

        Informe informe = sampleInforme();

        // No debe propagar excepcion — el fallo de email es no critico
        service.notificarAprobacionAdmin(informe);
    }

    private static MailProperties mailProperties() {
        MailProperties props = new MailProperties();
        props.setEnabled(true);
        props.setFrom("sigcon@educacionbogota.edu.co");
        props.setGraphApiBaseUrl("https://graph.microsoft.com/v1.0");
        props.setTenantId("tenant-123");
        props.setClientId("client+id");
        props.setClientSecret("a+b&c=d");
        return props;
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setEmail("contratista@educacionbogota.edu.co");
        usuario.setNombre("Contratista SIGCON");
        return usuario;
    }

    private static Informe sampleInforme() {
        Usuario contratista = new Usuario();
        contratista.setId(1L);
        contratista.setEmail("contratista@educacionbogota.edu.co");
        contratista.setNombre("Contratista SIGCON");

        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setNumero("OPS-2026-001");
        contrato.setContratista(contratista);

        Informe informe = new Informe();
        informe.setId(50L);
        informe.setNumero(3);
        informe.setContrato(contrato);
        informe.setFechaInicio(java.time.LocalDate.of(2026, 5, 1));
        informe.setFechaFin(java.time.LocalDate.of(2026, 5, 31));
        informe.setFechaAprobacion(java.time.LocalDateTime.of(2026, 5, 11, 10, 0));
        return informe;
    }
}
