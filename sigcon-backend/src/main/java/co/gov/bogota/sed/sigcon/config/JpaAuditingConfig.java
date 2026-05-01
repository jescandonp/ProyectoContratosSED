package co.gov.bogota.sed.sigcon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Separate config class for JPA Auditing so tests can exclude it
 * without triggering "JPA metamodel must not be empty" errors.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
}
