package co.gov.bogota.sed.sigcon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Separate config class for JPA Auditing so tests can exclude it
 * without triggering "JPA metamodel must not be empty" errors.
 * <p>
 * auditorProvider is a placeholder returning "SYSTEM" until Task 6 wires in
 * the authenticated principal (HTTP Basic in local-dev, JWT sub in weblogic).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Placeholder: returns "SYSTEM" until JWT/Basic principal extraction is implemented
        return () -> Optional.of("SYSTEM");
    }
}
