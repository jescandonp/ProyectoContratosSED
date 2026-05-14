package co.gov.bogota.sed.sigcon.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class DevSecurityConfigTest {

    @Test
    void includesIvaContractorInLocalDevUsers() {
        UserDetails user = new DevSecurityConfig()
            .devUserDetailsService()
            .loadUserByUsername("aecheverry@educacionbogota.gov.co");

        assertThat(user.getUsername()).isEqualTo("aecheverry@educacionbogota.gov.co");
        assertThat(user.getAuthorities())
            .extracting("authority")
            .contains("ROLE_CONTRATISTA");
    }
}
