package co.gov.bogota.sed.sigcon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("local-dev")
public class DevSecurityConfig {

    @Value("${sigcon.security.cors-allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .headers()
                // HSTS omitido: local-dev corre sobre HTTP. Solo activo en SecurityConfig (weblogic).
                .frameOptions().deny()
                .contentTypeOptions().and()
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
                .antMatchers("/api/admin/**").access("@sigconAuthorization.isAdmin(authentication)")
                .antMatchers(HttpMethod.GET, "/api/contratos/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/contratos/**").access("@sigconAuthorization.isAdmin(authentication)")
                .antMatchers(HttpMethod.PUT, "/api/contratos/**").access("@sigconAuthorization.isAdmin(authentication)")
                .antMatchers(HttpMethod.PATCH, "/api/contratos/**").access("@sigconAuthorization.isAdmin(authentication)")
                .antMatchers(HttpMethod.DELETE, "/api/contratos/**").access("@sigconAuthorization.isAdmin(authentication)")
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
                .antMatchers("/api/documentos-catalogo/**").access("@sigconAuthorization.isAdmin(authentication)")
                .antMatchers("/api/usuarios/**").access("@sigconAuthorization.isAdmin(authentication)")
                .anyRequest().authenticated()
            .and()
            .httpBasic();
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
    public UserDetailsService devUserDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin@educacionbogota.edu.co").password("{noop}admin123").roles("ADMIN").build(),
            User.withUsername("juan.escandon@educacionbogota.edu.co").password("{noop}contratista123").roles("CONTRATISTA").build(),
            User.withUsername("aecheverry@educacionbogota.gov.co").password("{noop}contratista123").roles("CONTRATISTA", "ADMIN").build(),
            User.withUsername("revisor1@educacionbogota.edu.co").password("{noop}revisor123").roles("REVISOR").build(),
            User.withUsername("supervisor1@educacionbogota.edu.co").password("{noop}supervisor123").roles("SUPERVISOR").build(),
            // I9: usuario de prueba para el rol ADMINISTRATIVO
            User.withUsername("administrativo@educacionbogota.edu.co").password("{noop}admin123").roles("ADMINISTRATIVO").build()
        );
    }
}
