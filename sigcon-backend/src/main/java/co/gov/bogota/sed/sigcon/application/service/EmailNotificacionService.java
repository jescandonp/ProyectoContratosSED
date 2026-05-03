package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.config.MailProperties;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Envia notificaciones por correo electronico via Microsoft Graph / Office 365.
 * En local-dev (mail.enabled=false) simula el envio con log de nivel INFO.
 * Los errores de envio NO propagan excepcion — se registran como ERROR y el flujo continua.
 */
@Service
public class EmailNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacionService.class);

    private final MailProperties mailProperties;
    private final RestTemplate restTemplate;

    public EmailNotificacionService(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envia (o simula) un correo al destinatario.
     *
     * @param destinatario usuario destinatario
     * @param evento       tipo de evento que dispara el correo
     * @param idInforme    ID del informe asociado (puede ser null)
     * @param descripcion  texto descriptivo del evento
     */
    public void enviar(Usuario destinatario, TipoEvento evento, Long idInforme, String descripcion) {
        if (!mailProperties.isEnabled()) {
            log.info("EMAIL SIMULADO [local-dev] evento={} destinatario={} idInforme={} desc={}",
                evento, destinatario.getEmail(), idInforme, descripcion);
            return;
        }
        try {
            String token = obtenerToken();
            String asunto = asuntoParaEvento(evento);
            String cuerpo = construirCuerpo(destinatario, evento, idInforme, descripcion);
            enviarViaGraph(destinatario.getEmail(), asunto, cuerpo, token);
            log.info("Email enviado evento={} destinatario={} idInforme={}", evento, destinatario.getEmail(), idInforme);
        } catch (Exception e) {
            log.error("Error enviando email evento={} destinatario={} idInforme={}: {}",
                evento, destinatario.getEmail(), idInforme, e.getMessage(), e);
            // NO relanzar — errores de email no revierten la transaccion de negocio
        }
    }

    private String obtenerToken() {
        String tokenUrl = "https://login.microsoftonline.com/" + mailProperties.getTenantId() + "/oauth2/v2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", mailProperties.getClientId());
        body.add("client_secret", mailProperties.getClientSecret());
        body.add("scope", "https://graph.microsoft.com/.default");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);
        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("No se pudo obtener token de Microsoft Graph");
        }
        return (String) response.get("access_token");
    }

    private void enviarViaGraph(String destinatarioEmail, String asunto, String cuerpo, String token) {
        String url = mailProperties.getGraphApiBaseUrl() + "/users/" + mailProperties.getFrom() + "/sendMail";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> message = new HashMap<>();
        message.put("subject", asunto);
        Map<String, Object> body = new HashMap<>();
        body.put("contentType", "HTML");
        body.put("content", cuerpo);
        message.put("body", body);

        Map<String, Object> emailAddress = new HashMap<>();
        emailAddress.put("address", destinatarioEmail);
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("emailAddress", emailAddress);
        message.put("toRecipients", Collections.singletonList(recipient));

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("saveToSentItems", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForObject(url, request, Void.class);
    }

    private static String asuntoParaEvento(TipoEvento evento) {
        switch (evento) {
            case INFORME_ENVIADO:    return "SIGCON — Informe enviado para revisión";
            case REVISION_APROBADA:  return "SIGCON — Informe listo para aprobación";
            case REVISION_DEVUELTA:  return "SIGCON — Informe devuelto por revisión";
            case INFORME_APROBADO:   return "SIGCON — Informe aprobado";
            case INFORME_DEVUELTO:   return "SIGCON — Informe devuelto por supervisor";
            default:                 return "SIGCON — Notificación de informe";
        }
    }

    private static String construirCuerpo(Usuario destinatario, TipoEvento evento, Long idInforme, String descripcion) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family:Arial,sans-serif;font-size:12pt;\">");
        sb.append("<p>Estimado/a <b>").append(destinatario.getNombre()).append("</b>,</p>");
        sb.append("<p>").append(descripcion != null ? descripcion : evento.name()).append("</p>");
        if (idInforme != null) {
            sb.append("<p>Por favor ingrese al sistema SIGCON para revisar el informe.</p>");
        }
        sb.append("<br/><p style=\"color:#555;font-size:10pt;\">");
        sb.append("Secretaría de Educación del Distrito — Sistema SIGCON<br/>");
        sb.append("Este correo es generado automáticamente. No responda a este mensaje.");
        sb.append("</p></body></html>");
        return sb.toString();
    }
}
