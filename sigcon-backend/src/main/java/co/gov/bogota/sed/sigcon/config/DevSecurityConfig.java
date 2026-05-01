package co.gov.bogota.sed.sigcon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("local-dev")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
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
                .antMatchers(HttpMethod.GET, "/api/documentos-catalogo/**").authenticated()
                .antMatchers("/api/documentos-catalogo/**").hasRole("ADMIN")
                .antMatchers("/api/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .httpBasic();
        return http.build();
    }

    @Bean
    public UserDetailsService devUserDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin@educacionbogota.edu.co").password("{noop}admin123").roles("ADMIN").build(),
            User.withUsername("juan.escandon@educacionbogota.edu.co").password("{noop}contratista123").roles("CONTRATISTA").build(),
            User.withUsername("revisor1@educacionbogota.edu.co").password("{noop}revisor123").roles("REVISOR").build(),
            User.withUsername("supervisor1@educacionbogota.edu.co").password("{noop}supervisor123").roles("SUPERVISOR").build()
        );
    }
}
