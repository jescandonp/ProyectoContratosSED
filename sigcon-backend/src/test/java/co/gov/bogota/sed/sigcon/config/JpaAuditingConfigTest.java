package co.gov.bogota.sed.sigcon.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigTest {

    private final AuditorAware<String> auditorAware = new JpaAuditingConfig().auditorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedPrincipalName() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "supervisor1@educacionbogota.edu.co",
                "n/a",
                Collections.emptyList()
            )
        );

        assertThat(auditorAware.getCurrentAuditor()).contains("supervisor1@educacionbogota.edu.co");
    }

    @Test
    void returnsExplicitFallbackForAnonymousContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", Collections.emptyList())
        );

        assertThat(auditorAware.getCurrentAuditor()).contains(JpaAuditingConfig.AUDITOR_NO_AUTENTICADO);
    }
}
