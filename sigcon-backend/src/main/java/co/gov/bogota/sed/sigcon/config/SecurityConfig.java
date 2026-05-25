package co.gov.bogota.sed.sigcon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("weblogic")
public class SecurityConfig {

    // JWT Bearer en Authorization header elimina superficie CSRF.
    // Ref: OWASP ASVS 4.0.3 §3.5.3 — docs/superpowers/specs/2026-05-15-sigcon-i-sec-design.md
    @Value("${sigcon.security.cors-allowed-origins:}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .headers()
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity()
                    .maxAgeInSeconds(31536000).includeSubDomains(true).and()
                .cacheControl().and()
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'").and()
                .addHeaderWriter(new StaticHeadersWriter(
                    "Referrer-Policy", "strict-origin-when-cross-origin"))
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy", "geolocation=(), camera=(), microphone=()"))
                .and()
            .authorizeRequests()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/swagger-ui.html", "/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
                .antMatchers("/api/usuarios/me", "/api/usuarios/me/**").authenticated()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/contratos/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/informes/*/pdf").hasAnyRole("CONTRATISTA", "SUPERVISOR", "ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.GET, "/api/informes/cola/visto-bueno").hasAnyRole("ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.GET, "/api/informes/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar").hasRole("SUPERVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver").hasAnyRole("SUPERVISOR", "ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.POST, "/api/informes/*/dar-visto-bueno").hasAnyRole("ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.POST, "/api/informes/*/escalar").hasAnyRole("ADMIN", "ADMINISTRATIVO")
                .antMatchers(HttpMethod.POST, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers(HttpMethod.PUT, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers(HttpMethod.DELETE, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers("/api/actividades/**").hasRole("CONTRATISTA")
                .antMatchers("/api/notificaciones/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/documentos-catalogo/**").authenticated()
                .antMatchers("/api/documentos-catalogo/**").hasRole("ADMIN")
                .antMatchers("/api/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(azureRolesConverter());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (!corsAllowedOrigins.trim().isEmpty()) {
            config.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        }
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter azureRolesConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName("preferred_username");
        return authenticationConverter;
    }
}
