package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.config.MailProperties;
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
}
