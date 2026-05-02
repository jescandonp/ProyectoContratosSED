package co.gov.bogota.sed.sigcon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuracion JPA Auditing.
 * I3: auditorProvider retorna el email del principal autenticado
 * (HTTP Basic en local-dev, JWT sub en weblogic).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    static final String AUDITOR_NO_AUTENTICADO = "AUDITOR_NO_AUTENTICADO";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of(AUDITOR_NO_AUTENTICADO);
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof String && "anonymousUser".equals(principal)) {
                return Optional.of(AUDITOR_NO_AUTENTICADO);
            }
            // auth.getName() retorna el email (HTTP Basic) o el sub del JWT
            String name = auth.getName();
            return Optional.of(name != null && !name.isEmpty() ? name : AUDITOR_NO_AUTENTICADO);
        };
    }
}
