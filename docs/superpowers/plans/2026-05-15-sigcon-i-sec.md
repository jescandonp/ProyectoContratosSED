# SIGCON I-SEC — Implementation Plan
## Controles de Seguridad Baseline

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar headers HTTP de seguridad, CORS configurable, validación de tipo/tamaño en upload de soportes, y documentar las decisiones de CSRF y rate limiting, alcanzando cumplimiento N2 del checklist SED.

**Architecture:** Spring Security `headers()` y `cors()` centralizados en `SecurityConfig` (weblogic) y `DevSecurityConfig` (local-dev). La validación de archivos se agrega en `LocalDocumentStorageService.storeFile()`, único punto de entrada al storage. La documentación de decisiones va en `ARCHITECTURE.md` y en la spec I-SEC.

**Tech Stack:** Spring Boot 2.7.18 · Spring Security 5.7.x · Java 8 · Maven · Angular 20 (sin cambios en frontend)

---

## Mapa de Archivos

| Acción | Archivo |
|--------|---------|
| Modificar | `sigcon-backend/src/main/resources/application.yml` |
| Modificar | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java` |
| Modificar | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java` |
| Modificar | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java` |
| Modificar | `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageService.java` |
| Modificar | `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java` |
| Crear | `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageServiceTest.java` |
| Modificar | `docs/ARCHITECTURE.md` |
| Crear | `docs/specs/2026-05-15-sigcon-i-sec-spec.md` |
| Crear | `docs/plans/2026-05-15-sigcon-i-sec-execution-log.md` |

---

## T0 — Documentación base I-SEC

**Files:**
- Create: `docs/specs/2026-05-15-sigcon-i-sec-spec.md`
- Create: `docs/plans/2026-05-15-sigcon-i-sec-execution-log.md`

- [ ] **Step 1: Crear spec I-SEC**

Crear `docs/specs/2026-05-15-sigcon-i-sec-spec.md` con el siguiente contenido:

```markdown
# Spec Técnica — SIGCON I-SEC
## Controles de Seguridad Baseline — Cumplimiento N2 SED

> **Metodología:** Spec-Driven Development (SDD)
> **Versión:** 1.0 — **Fecha:** 2026-05-15
> **Rama:** `feat/sigcon-i-sec`
> **Referencia normativa:** Criterios de Aceptación de Seguridad APP WEB v1.0 — SED Bogotá
> **Nivel objetivo:** N2
> **Design doc:** `docs/superpowers/specs/2026-05-15-sigcon-i-sec-design.md`

---

## 1. Alcance

### Cambios de código

| ID | Brecha | Solución |
|----|--------|----------|
| SEC-01 | Headers HTTP de seguridad ausentes | `headers()` en Spring Security |
| SEC-03 | CORS no configurado | `CorsConfigurationSource` bean + env var |
| SEC-04 | Soportes sin validación de tipo/tamaño | `validateSoporte()` en `LocalDocumentStorageService` |
| SEC-06 | `Cache-Control` ausente | `cacheControl()` en Spring Security headers |

### Decisiones documentadas (sin cambio de código)

| ID | Decisión |
|----|----------|
| SEC-02 | CSRF: JWT Bearer en `Authorization` header elimina superficie CSRF. Ref: OWASP ASVS 4.0.3 §3.5.3 |
| SEC-05 | Rate limiting: delegado a infraestructura SED (WAF). Documentado en `ARCHITECTURE.md` |

### Fuera de alcance
- Logging centralizado / SIEM
- HSM / Vault para claves
- CI/CD con SAST/DAST
- Cambios en frontend Angular

---

## 2. Vectores SED Cubiertos

| Vector | Criterio | Solución |
|--------|----------|----------|
| V14.4 | Headers de seguridad HTTP | SEC-01 |
| V13.1 | CORS explícito | SEC-03 |
| V12.1 | Validación de tipo en upload | SEC-04 |
| V8.1  | Cache-Control en respuestas | SEC-06 |

---

## 3. Headers HTTP Configurados

| Header | Valor | Vector SED |
|--------|-------|-----------|
| `X-Frame-Options` | `DENY` | V14.4 |
| `X-Content-Type-Options` | `nosniff` | V14.4 |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'` | V14.4 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` (solo perfil weblogic) | V14.4 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | V14.4 |
| `Permissions-Policy` | `geolocation=(), camera=(), microphone=()` | V14.4 |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | V8.1 |

`unsafe-inline` en `style-src` es necesario porque Angular y PrimeNG inyectan estilos inline.
HSTS se omite en `DevSecurityConfig` (local-dev corre sobre HTTP).

---

## 4. CORS

Orígenes permitidos configurables via variable de entorno:

```yaml
# local-dev
sigcon.security.cors-allowed-origins: http://localhost:4200

# weblogic
sigcon.security.cors-allowed-origins: ${SIGCON_CORS_ALLOWED_ORIGINS:}
```

Métodos: GET, POST, PUT, PATCH, DELETE, OPTIONS.
Headers permitidos: Authorization, Content-Type, X-Requested-With.
`allowCredentials: false` (JWT Bearer, sin cookies de sesión).

---

## 5. Validación de Archivos (Soportes de Actividades)

Tipos permitidos en `LocalDocumentStorageService.storeFile()`:

| Extensión | Content-types aceptados |
|-----------|------------------------|
| `.pdf` | `application/pdf` |
| `.png` | `image/png` |
| `.jpg`, `.jpeg` | `image/jpeg` |
| `.eml` | `message/rfc822`, `application/octet-stream` |
| `.doc` | `application/msword` |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| `.xls` | `application/vnd.ms-excel` |
| `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

Tamaño máximo: 10 MB (configurable: `sigcon.storage.max-file-size-bytes`).
Errores: `SOPORTE_FORMATO_INVALIDO`, `SOPORTE_TAMANIO_EXCEDIDO`.

---

## 6. Criterios de Aceptación

- [ ] `X-Frame-Options: DENY` en todas las respuestas HTTP.
- [ ] `X-Content-Type-Options: nosniff` en todas las respuestas HTTP.
- [ ] `Content-Security-Policy` con `frame-ancestors 'none'`.
- [ ] `Strict-Transport-Security` con `max-age=31536000; includeSubDomains` (solo weblogic).
- [ ] `Referrer-Policy: strict-origin-when-cross-origin` en todas las respuestas.
- [ ] `Cache-Control: no-cache, no-store` en respuestas autenticadas.
- [ ] CORS responde solo para orígenes en `SIGCON_CORS_ALLOWED_ORIGINS`.
- [ ] Upload `.exe` → error `SOPORTE_FORMATO_INVALIDO`.
- [ ] Upload > 10 MB → error `SOPORTE_TAMANIO_EXCEDIDO`.
- [ ] Upload `.pdf` válido → almacenado correctamente.
- [ ] `ARCHITECTURE.md` contiene sección de seguridad.
- [ ] Suite backend: `mvn test` — 0 fallos.
```

- [ ] **Step 2: Crear execution log I-SEC**

Crear `docs/plans/2026-05-15-sigcon-i-sec-execution-log.md`:

```markdown
# Execution Log — SIGCON I-SEC
## Controles de Seguridad Baseline

> **Spec:** `docs/specs/2026-05-15-sigcon-i-sec-spec.md`
> **Plan:** `docs/superpowers/plans/2026-05-15-sigcon-i-sec.md`
> **Rama:** `feat/sigcon-i-sec`
> **Inicio:** 2026-05-15
> **Estado:** EN EJECUCIÓN

---

## Contexto

Incremento de seguridad derivado del análisis de cumplimiento contra el documento
"Criterios de Aceptación de Seguridad APP WEB v1.0" del equipo de Gobierno y
Seguridad Digital SED. Cubre SEC-01, SEC-03, SEC-04 y SEC-06 (código) y
documenta SEC-02 y SEC-05 (decisiones de arquitectura).

---

## Matriz de Tareas

| Tarea | Descripción | Estado | Commit |
|-------|-------------|--------|--------|
| T0 | Documentación base I-SEC | EN EJECUCIÓN | — |
| T1 | Sección seguridad en ARCHITECTURE.md | PENDIENTE | — |
| T2 | Headers + CORS + Cache-Control en SecurityConfig | PENDIENTE | — |
| T3 | Validación archivos en LocalDocumentStorageService | PENDIENTE | — |
| T4 | Validación final y cierre | PENDIENTE | — |

---

## Registro de Ejecución

### 2026-05-15 — Apertura I-SEC

- Análisis de cumplimiento N2 completado contra 67 criterios en 14 vectores.
- Design doc aprobado: `docs/superpowers/specs/2026-05-15-sigcon-i-sec-design.md`.
- Plan de implementación creado.
- Documentación base (spec + execution log) iniciada.
```

- [ ] **Step 3: Commit documentación base**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add docs/specs/2026-05-15-sigcon-i-sec-spec.md docs/plans/2026-05-15-sigcon-i-sec-execution-log.md
git commit -m "docs(i-sec): add spec and execution log for security baseline increment"
```

Resultado esperado: `1 commit creado, 2 archivos`.

---

## T1 — Sección de seguridad en ARCHITECTURE.md

**Files:**
- Modify: `docs/ARCHITECTURE.md` (insertar entre línea 1069 `---` y línea 1071 `## 8.`)

- [ ] **Step 1: Insertar sección de seguridad en ARCHITECTURE.md**

Buscar la línea `## 8. Infraestructura — WebLogic 12` en `docs/ARCHITECTURE.md` e insertar el siguiente bloque **antes** de esa línea (después del `---` que la precede):

```markdown
## 7-bis. Seguridad y Controles

> Referencia normativa: **Criterios de Aceptación de Seguridad APP WEB v1.0** — SED Bogotá.
> Nivel de cumplimiento objetivo: **N2** (datos confidenciales, transacciones de negocio).
> Implementado en: `feat/sigcon-i-sec` — `docs/specs/2026-05-15-sigcon-i-sec-spec.md`.

---

### Headers HTTP de Seguridad

Configurados en `SecurityConfig.java` (perfil `weblogic`) y `DevSecurityConfig.java` (perfil `local-dev`) mediante `http.headers()` de Spring Security 5.7.x.

| Header | Valor | Propósito |
|--------|-------|-----------|
| `X-Frame-Options` | `DENY` | Previene clickjacking |
| `X-Content-Type-Options` | `nosniff` | Previene MIME sniffing |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'` | Restringe recursos externos |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Fuerza HTTPS (solo perfil `weblogic`) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limita información en Referer |
| `Permissions-Policy` | `geolocation=(), camera=(), microphone=()` | Deshabilita APIs browser no usadas |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | Previene caché de respuestas sensibles |

`unsafe-inline` en `style-src` es requerido porque Angular y PrimeNG inyectan estilos inline en runtime.
HSTS se omite en `DevSecurityConfig` porque el perfil local-dev corre sobre HTTP.

---

### CORS

Configurado con `CorsConfigurationSource` bean en ambos perfiles. Los orígenes permitidos se definen por ambiente:

```yaml
# local-dev
sigcon.security.cors-allowed-origins: http://localhost:4200

# weblogic — configurar en infraestructura antes de desplegar
SIGCON_CORS_ALLOWED_ORIGINS=https://sigcon.educacionbogota.edu.co
```

Reglas: métodos GET/POST/PUT/PATCH/DELETE/OPTIONS; headers `Authorization`, `Content-Type`, `X-Requested-With`; `allowCredentials: false`; preflight cache 1 hora.

---

### CSRF — Decisión de Arquitectura

**CSRF está deshabilitado intencionalmente** en SIGCON.

Justificación: SIGCON usa **JWT Bearer tokens** transmitidos en el header `Authorization`. Los navegadores **no envían automáticamente** headers de autorización en solicitudes cross-site. Por lo tanto, un atacante no puede forjar una solicitud autenticada sin acceso al token. Este es el modelo stateless REST descrito en OWASP ASVS 4.0.3 §3.5.3.

Si en algún momento se adoptan cookies de sesión en lugar de JWT Bearer, esta decisión debe revisarse e implementarse protección CSRF.

---

### Rate Limiting — Decisión de Infraestructura

El control de rate limiting y protección anti-fuerza-bruta está **delegado a la infraestructura SED** (WebLogic + WAF de red).

Si el despliegue cambia a un entorno sin WAF, implementar rate limiting a nivel de aplicación con Bucket4j o un `HandlerInterceptor` de Spring.

---

### Validación de Archivos en Upload

Punto de control: `LocalDocumentStorageService.storeFile()` — único punto de entrada al storage para soportes de actividades.

| Extensión | Content-types aceptados |
|-----------|------------------------|
| `.pdf` | `application/pdf` |
| `.png` | `image/png` |
| `.jpg`, `.jpeg` | `image/jpeg` |
| `.eml` | `message/rfc822`, `application/octet-stream` |
| `.doc` | `application/msword` |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| `.xls` | `application/vnd.ms-excel` |
| `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

Tamaño máximo configurable: `sigcon.storage.max-file-size-bytes` (default 10 MB).
Errores de validación: `SOPORTE_FORMATO_INVALIDO`, `SOPORTE_TAMANIO_EXCEDIDO`.

Los `DocumentoRequeridoInformeService` (facturas, documentos requeridos) tienen validación más restrictiva (solo PDF/EML) implementada desde I7.

---

```

- [ ] **Step 2: Verificar que el documento compila correctamente (sin errores de Markdown)**

Revisar visualmente que la nueva sección tiene encabezados correctos y que la tabla de contenidos en la línea 23 (`7. Seguridad y Autenticación`) sigue siendo coherente. La sección 7-bis no requiere entrada en el TOC.

- [ ] **Step 3: Commit ARCHITECTURE.md**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add docs/ARCHITECTURE.md
git commit -m "docs(i-sec): add security controls section to ARCHITECTURE.md"
```

---

## T2 — Headers HTTP + CORS + Cache-Control en SecurityConfig

**Files:**
- Modify: `sigcon-backend/src/main/resources/application.yml`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`

- [ ] **Step 1: Escribir el test de headers que debe fallar primero**

Abrir `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`.

Agregar este método al final de la clase (antes del cierre `}`):

```java
@Test
void securityHeaders_deSeguridad_presentesEnRespuesta() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().exists("Content-Security-Policy"))
        .andExpect(header().exists("Cache-Control"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().string("Permissions-Policy", "geolocation=(), camera=(), microphone=()"));
}
```

Agregar el import necesario si no existe:
```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
```

- [ ] **Step 2: Correr el test para confirmar que falla**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=SigconBackendSecurityTest#securityHeaders_deSeguridad_presentesEnRespuesta" -q
```

Resultado esperado: **FAIL** — `X-Frame-Options` ausente o no es `DENY`.

- [ ] **Step 3: Agregar propiedades en application.yml**

En `sigcon-backend/src/main/resources/application.yml`, dentro del bloque del perfil `local-dev` (después de `sigcon.storage:`), agregar:

```yaml
sigcon:
  storage:
    signatures-path: ${java.io.tmpdir}/sigcon
    max-file-size-bytes: 10485760   # 10 MB — nuevo en I-SEC
  security:                          # nuevo en I-SEC
    cors-allowed-origins: http://localhost:4200
  mail:
    # ... (sin cambio)
```

Y en el bloque del perfil `weblogic` (dentro de la sección `sigcon:`), agregar:

```yaml
sigcon:
  security:                          # nuevo en I-SEC
    cors-allowed-origins: ${SIGCON_CORS_ALLOWED_ORIGINS:}
  mail:
    # ... (sin cambio)
```

- [ ] **Step 4: Reemplazar SecurityConfig.java completo**

Reemplazar el contenido completo de `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java` con:

```java
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
                .frameOptions().deny().and()
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
                .antMatchers(HttpMethod.GET, "/api/informes/*/pdf").hasAnyRole("CONTRATISTA", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.GET, "/api/informes/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar").hasRole("SUPERVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver").hasRole("SUPERVISOR")
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
```

- [ ] **Step 5: Reemplazar DevSecurityConfig.java completo**

Reemplazar el contenido completo de `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java` con:

```java
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
                .frameOptions().deny().and()
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
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/contratos/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/contratos/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/informes/*/pdf").hasAnyRole("CONTRATISTA", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.GET, "/api/informes/**").hasAnyRole("CONTRATISTA", "REVISOR", "SUPERVISOR", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver-revision").hasRole("REVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/aprobar").hasRole("SUPERVISOR")
                .antMatchers(HttpMethod.POST, "/api/informes/*/devolver").hasRole("SUPERVISOR")
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
            User.withUsername("aecheverry@educacionbogota.gov.co").password("{noop}contratista123").roles("CONTRATISTA").build(),
            User.withUsername("revisor1@educacionbogota.edu.co").password("{noop}revisor123").roles("REVISOR").build(),
            User.withUsername("supervisor1@educacionbogota.edu.co").password("{noop}supervisor123").roles("SUPERVISOR").build()
        );
    }
}
```

- [ ] **Step 6: Correr el test de headers para confirmar que pasa**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=SigconBackendSecurityTest#securityHeaders_deSeguridad_presentesEnRespuesta" -q
```

Resultado esperado: **BUILD SUCCESS** — 1 test, 0 fallos.

- [ ] **Step 7: Correr suite de seguridad completa para detectar regresiones**

```powershell
mvn test "-Dtest=SigconBackendSecurityTest,InformeSecurityTest" -q
```

Resultado esperado: BUILD SUCCESS, 0 fallos.

- [ ] **Step 8: Commit T2**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/resources/application.yml `
        sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java `
        sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java `
        sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java
git commit -m "feat(i-sec): add security headers, CORS and cache-control (SEC-01, SEC-03, SEC-06)"
```

---

## T3 — Validación de tipo y tamaño en LocalDocumentStorageService

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageService.java`
- Create: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageServiceTest.java`

- [ ] **Step 1: Escribir los 3 tests que deben fallar primero**

Crear `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageServiceTest.java`:

```java
package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalDocumentStorageService(tempDir.toString(), 10L * 1024L * 1024L);
    }

    @Test
    void storeFile_extensionNoPermitida_lanzaSoporteFormatoInvalido() {
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.storeFile("soportes/1/2/3", archivo))
            .isInstanceOf(SigconBusinessException.class)
            .satisfies(ex ->
                assertThat(((SigconBusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOPORTE_FORMATO_INVALIDO));
    }

    @Test
    void storeFile_tamanioExcedido_lanzaSoporteTamanioExcedido() {
        // Servicio con límite de 5 bytes para facilitar el test
        LocalDocumentStorageService serviceLimitado =
            new LocalDocumentStorageService(tempDir.toString(), 5L);
        byte[] contenido = new byte[10]; // 10 bytes > límite de 5 bytes
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "documento.pdf", "application/pdf", contenido
        );

        assertThatThrownBy(() -> serviceLimitado.storeFile("soportes/1/2/3", archivo))
            .isInstanceOf(SigconBusinessException.class)
            .satisfies(ex ->
                assertThat(((SigconBusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOPORTE_TAMANIO_EXCEDIDO));
    }

    @Test
    void storeFile_pdfValido_retornaReferenciaYAlmacena() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "informe.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        String referencia = service.storeFile("soportes/1/2/3", archivo);

        assertThat(referencia).startsWith("soportes/1/2/3/");
        assertThat(referencia).endsWith("_informe.pdf");
    }
}
```

- [ ] **Step 2: Correr los tests para confirmar que fallan**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=LocalDocumentStorageServiceTest" -q
```

Resultado esperado: **FAIL** — los 3 tests fallan porque:
- `LocalDocumentStorageService` no tiene constructor de 2 parámetros.
- No existen `SOPORTE_FORMATO_INVALIDO` ni `SOPORTE_TAMANIO_EXCEDIDO` en `ErrorCode`.

- [ ] **Step 3: Agregar códigos de error en ErrorCode.java**

Abrir `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java`.

Agregar al final del enum, antes del cierre `}`:

```java
    // I-SEC
    SOPORTE_FORMATO_INVALIDO,
    SOPORTE_TAMANIO_EXCEDIDO
```

El archivo completo queda:

```java
package co.gov.bogota.sed.sigcon.web.exception;

public enum ErrorCode {
    // I1
    USUARIO_NO_ENCONTRADO,
    CONTRATO_NO_ENCONTRADO,
    NUMERO_CONTRATO_DUPLICADO,
    EMAIL_DUPLICADO,
    ACCESO_DENEGADO,
    FORMATO_IMAGEN_INVALIDO,
    ESTADO_INVALIDO,

    // I2
    INFORME_NO_ENCONTRADO,
    INFORME_NO_EDITABLE,
    TRANSICION_INVALIDA,
    OBSERVACION_REQUERIDA,
    ACTIVIDAD_REQUERIDA,
    PORCENTAJE_INVALIDO,
    SOPORTE_INVALIDO,
    DOCUMENTO_ADICIONAL_REQUERIDO,
    CONTRATO_NO_ACTIVO,

    // I3
    PDF_NO_DISPONIBLE,
    PDF_GENERACION_FALLIDA,
    FIRMA_REQUERIDA,
    NOTIFICACION_NO_ENCONTRADA,
    EMAIL_NO_ENVIADO,

    // I4
    FECHA_FIN_INVALIDA,

    // I7
    DOCUMENTO_REQUERIDO_NO_ENCONTRADO,
    DOCUMENTO_REQUERIDO_FORMATO_INVALIDO,
    DOCUMENTO_REQUERIDO_NO_EDITABLE,
    DOCUMENTO_REQUERIDO_FALTANTE,

    // I-SEC
    SOPORTE_FORMATO_INVALIDO,
    SOPORTE_TAMANIO_EXCEDIDO
}
```

- [ ] **Step 4: Reemplazar LocalDocumentStorageService.java completo**

Reemplazar el contenido completo de `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageService.java` con:

```java
package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("local-dev")
public class LocalDocumentStorageService implements DocumentStorageService {

    private static final long MAX_SIGNATURE_BYTES = 2L * 1024L * 1024L;

    private static final Map<String, List<String>> ALLOWED_TYPES;
    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(".pdf",  Arrays.asList("application/pdf"));
        m.put(".png",  Arrays.asList("image/png"));
        m.put(".jpg",  Arrays.asList("image/jpeg"));
        m.put(".jpeg", Arrays.asList("image/jpeg"));
        m.put(".eml",  Arrays.asList("message/rfc822", "application/octet-stream"));
        m.put(".doc",  Arrays.asList("application/msword"));
        m.put(".docx", Arrays.asList(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        m.put(".xls",  Arrays.asList("application/vnd.ms-excel"));
        m.put(".xlsx", Arrays.asList(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        ALLOWED_TYPES = Collections.unmodifiableMap(m);
    }

    private final Path rootPath;
    private final long maxFileSizeBytes;

    public LocalDocumentStorageService(
            @Value("${sigcon.storage.signatures-path:${java.io.tmpdir}/sigcon}") String rootPath,
            @Value("${sigcon.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.rootPath = Paths.get(rootPath);
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public String storeSignature(Long usuarioId, MultipartFile file) throws IOException {
        validateSignature(file);
        String extension = extensionFor(file.getContentType());
        String relativeReference = "firmas/" + usuarioId + "/" + UUID.randomUUID() + extension;
        Path target = rootPath.resolve(relativeReference).normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return relativeReference;
    }

    @Override
    public String storeFile(String subdir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Archivo vacío o ausente",
                HttpStatus.BAD_REQUEST
            );
        }
        if (subdir == null || subdir.trim().isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Subdirectorio requerido para almacenar el soporte",
                HttpStatus.BAD_REQUEST
            );
        }
        validateSoporte(file);
        String safeName = sanitize(file.getOriginalFilename());
        String relativeReference = subdir + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = rootPath.resolve(relativeReference).normalize();
        if (!target.startsWith(rootPath)) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Ruta de soporte inválida",
                HttpStatus.BAD_REQUEST
            );
        }
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return relativeReference;
    }

    @Override
    public String storeBytes(String subdir, String filename, byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new SigconBusinessException(
                ErrorCode.PDF_GENERACION_FALLIDA,
                "Contenido del PDF vacío",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        String safeFilename = sanitize(filename);
        String relativeReference = subdir + "/" + safeFilename;
        Path target = rootPath.resolve(relativeReference).normalize();
        if (!target.startsWith(rootPath)) {
            throw new SigconBusinessException(
                ErrorCode.PDF_GENERACION_FALLIDA,
                "Ruta de PDF inválida",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return relativeReference;
    }

    @Override
    public InputStream loadFile(String relativePath) throws IOException {
        Path target = rootPath.resolve(relativePath).normalize();
        if (!target.startsWith(rootPath) || !Files.exists(target)) {
            throw new IOException("Archivo no encontrado: " + relativePath);
        }
        return new FileInputStream(target.toFile());
    }

    private void validateSoporte(MultipartFile file) {
        if (file.getSize() > maxFileSizeBytes) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_TAMANIO_EXCEDIDO,
                "El archivo supera el tamaño máximo de " + (maxFileSizeBytes / 1024 / 1024) + " MB",
                HttpStatus.BAD_REQUEST
            );
        }
        String name = file.getOriginalFilename() != null
            ? file.getOriginalFilename().toLowerCase() : "";
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        List<String> allowedContentTypes = ALLOWED_TYPES.get(ext);
        if (allowedContentTypes == null) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_FORMATO_INVALIDO,
                "Extensión de archivo no permitida: " + (ext.isEmpty() ? "(sin extensión)" : ext),
                HttpStatus.BAD_REQUEST
            );
        }
        String contentType = file.getContentType();
        if (contentType != null && !allowedContentTypes.contains(contentType)) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_FORMATO_INVALIDO,
                "Tipo de contenido no permitido para la extensión indicada",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private String sanitize(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "soporte.bin";
        }
        String trimmed = name.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        return trimmed.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void validateSignature(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIGNATURE_BYTES) {
            throw invalidFormat();
        }
        String contentType = file.getContentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw invalidFormat();
        }
    }

    private String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }

    private SigconBusinessException invalidFormat() {
        return new SigconBusinessException(
            ErrorCode.FORMATO_IMAGEN_INVALIDO,
            "La firma debe ser una imagen JPG o PNG de máximo 2MB",
            HttpStatus.BAD_REQUEST
        );
    }
}
```

- [ ] **Step 5: Correr los 3 tests para confirmar que pasan**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test "-Dtest=LocalDocumentStorageServiceTest" -q
```

Resultado esperado: **BUILD SUCCESS** — 3 tests, 0 fallos.

- [ ] **Step 6: Correr suite de soportes para detectar regresiones**

```powershell
mvn test "-Dtest=SoporteAdjuntoServiceTest" -q
```

Resultado esperado: BUILD SUCCESS, 0 fallos.

- [ ] **Step 7: Commit T3**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java `
        sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageService.java `
        sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/service/LocalDocumentStorageServiceTest.java
git commit -m "feat(i-sec): validate file type and size in storeFile (SEC-04)"
```

---

## T4 — Validación final y cierre

**Files:**
- Modify: `docs/plans/2026-05-15-sigcon-i-sec-execution-log.md`

- [ ] **Step 1: Correr suite completa del backend**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn test -q
```

Resultado esperado: BUILD SUCCESS, 0 fallos, 0 errores.
Si falla: revisar el output de Maven para identificar qué test rompe y por qué.

- [ ] **Step 2: Verificar headers en respuesta real (opcional, si el backend puede levantarse localmente)**

```powershell
# Levantar el backend
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend"
mvn spring-boot:run "-Dspring-boot.run.profiles=local-dev"

# En otra terminal, verificar headers
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET |
    Select-Object -ExpandProperty Headers
```

Resultado esperado: ver `X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy`, `Cache-Control` en la respuesta.

- [ ] **Step 3: Actualizar execution log con resultados**

Abrir `docs/plans/2026-05-15-sigcon-i-sec-execution-log.md` y agregar al final:

```markdown
### 2026-05-15 — T2 Headers + CORS + Cache-Control

**Cambios:**
- `SecurityConfig.java`: headers(), CORS bean, HSTS incluido.
- `DevSecurityConfig.java`: headers() sin HSTS, CORS bean.
- `application.yml`: `sigcon.security.cors-allowed-origins` en ambos perfiles;
  `sigcon.storage.max-file-size-bytes: 10485760` en local-dev.
- `SigconBackendSecurityTest`: test `securityHeaders_deSeguridad_presentesEnRespuesta`.

**Validaciones:**
- `mvn test -Dtest=SigconBackendSecurityTest,InformeSecurityTest` — N tests, 0 fallos.

**Commit:** [SHA T2]

---

### 2026-05-15 — T3 Validación de archivos

**Cambios:**
- `ErrorCode.java`: `SOPORTE_FORMATO_INVALIDO`, `SOPORTE_TAMANIO_EXCEDIDO`.
- `LocalDocumentStorageService.java`: constructor con `maxFileSizeBytes`,
  constante `ALLOWED_TYPES`, método `validateSoporte()`.
- `LocalDocumentStorageServiceTest.java`: 3 tests nuevos.

**Validaciones:**
- `mvn test -Dtest=LocalDocumentStorageServiceTest,SoporteAdjuntoServiceTest` — N tests, 0 fallos.

**Commit:** [SHA T3]

---

### 2026-05-15 — T4 Validación final

**Suite completa:**
- `mvn test` — N tests, 0 fallos, BUILD SUCCESS.

**Estado:** COMPLETO

## Próximo Punto de Retoma

I-SEC cerrado. SIGCON cumple controles de seguridad N2 corregibles en código de aplicación.
Controles pendientes de decisión institucional: logging centralizado (SEC-07),
escaneo de dependencias (SEC-08), eventos de seguridad en logs (SEC-09).
```

- [ ] **Step 4: Commit cierre**

```powershell
Set-Location "C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED"
git add docs/plans/2026-05-15-sigcon-i-sec-execution-log.md
git commit -m "docs(i-sec): close security baseline increment execution log"
```

---

## Resumen de Commits Esperados

| Commit | Mensaje |
|--------|---------|
| T0 | `docs(i-sec): add spec and execution log for security baseline increment` |
| T1 | `docs(i-sec): add security controls section to ARCHITECTURE.md` |
| T2 | `feat(i-sec): add security headers, CORS and cache-control (SEC-01, SEC-03, SEC-06)` |
| T3 | `feat(i-sec): validate file type and size in storeFile (SEC-04)` |
| T4 | `docs(i-sec): close security baseline increment execution log` |
