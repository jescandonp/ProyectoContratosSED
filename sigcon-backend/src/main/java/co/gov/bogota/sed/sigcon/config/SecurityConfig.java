package co.gov.bogota.sed.sigcon.config;

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

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("weblogic")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/swagger-ui.html", "/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
                .antMatchers("/api/usuarios/me", "/api/usuarios/me/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/contratos/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/informes/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar").hasRole("SUPERVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver").hasRole("SUPERVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers(HttpMethod.PUT, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers(HttpMethod.DELETE, "/api/informes/**").hasRole("CONTRATISTA")
                .antMatchers("/api/actividades/**").hasRole("CONTRATISTA")
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
