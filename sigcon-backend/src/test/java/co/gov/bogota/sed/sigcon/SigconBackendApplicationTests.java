package co.gov.bogota.sed.sigcon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import co.gov.bogota.sed.sigcon.config.JpaAuditingConfig;

/**
 * Bootstrap smoke-test: verifies the Spring context loads without a live
 * Oracle DB or Azure AD tenant.  JPA auditing and data-source auto-configs
 * are excluded so the test remains self-contained.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = SigconBackendApplicationTests.TestConfig.class
)
@ActiveProfiles("local-dev")
class SigconBackendApplicationTests {

    /**
     * Minimal Spring Boot entry-point for tests.
     * Scans the production package but excludes:
     *  - SigconBackendApplication (avoids its own @ComponentScan triggering JPA auditing)
     *  - JpaAuditingConfig (avoids @EnableJpaAuditing requiring a JPA metamodel)
     * Also excludes all auto-configurations that require a running DB or OAuth2 issuer.
     */
    @org.springframework.boot.autoconfigure.SpringBootApplication
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    })
    @ComponentScan(
        basePackages = "co.gov.bogota.sed.sigcon",
        excludeFilters = {
            @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = { SigconBackendApplication.class, JpaAuditingConfig.class }
            )
        }
    )
    static class TestConfig {
    }

    @Test
    void contextLoads() {
        // Verifies Spring context starts correctly without requiring Oracle or Azure AD
    }
}
