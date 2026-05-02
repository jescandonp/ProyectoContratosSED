package co.gov.bogota.sed.sigcon.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuracion para el servicio de email Office 365 / Microsoft Graph.
 * Prefix: sigcon.mail
 */
@Component
@ConfigurationProperties(prefix = "sigcon.mail")
public class MailProperties {

    /** Si false, los correos se simulan y se registran en logs (local-dev). */
    private boolean enabled = false;

    /** Direccion de envio. Ej: sigcon@educacionbogota.edu.co */
    private String from;

    /** Base URL de Microsoft Graph. Default: https://graph.microsoft.com/v1.0 */
    private String graphApiBaseUrl = "https://graph.microsoft.com/v1.0";

    /** Azure tenant ID para autenticacion client credentials. */
    private String tenantId;

    /** Client ID de la aplicacion registrada en Azure AD. */
    private String clientId;

    /** Client secret de la aplicacion registrada en Azure AD. */
    private String clientSecret;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getGraphApiBaseUrl() { return graphApiBaseUrl; }
    public void setGraphApiBaseUrl(String graphApiBaseUrl) { this.graphApiBaseUrl = graphApiBaseUrl; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
