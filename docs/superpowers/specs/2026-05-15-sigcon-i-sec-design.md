# Design Doc — SIGCON Incremento de Seguridad (I-SEC)
## Controles de Seguridad Baseline — Cumplimiento N2 SED

> **Metodología:** Spec-Driven Development (SDD)
> **Fecha:** 2026-05-15
> **Rama:** `feat/sigcon-i-sec`
> **Base:** `feat/sigcon-i7` HEAD `8a417f4`
> **Referencia normativa:** Criterios de Aceptación de Seguridad APP WEB v1.0 — SED Bogotá
> **Nivel objetivo:** N2 (datos confidenciales, transacciones de negocio)

---

## 1. Contexto y Motivación

El equipo de Gobierno y Seguridad Digital de la SED entregó el documento "Criterios de Aceptación de Seguridad APP WEB v1.0" como checklist obligatorio para todos los aplicativos web de la entidad. SIGCON gestiona contratos, informes de supervisión, documentos PDF/EML, datos de seguridad social de contratistas y correos electrónicos institucionales, lo que lo clasifica como aplicación **Nivel 2** según la metodología del documento.

El análisis de cumplimiento identificó 6 brechas corregibles en código de aplicación (SEC-01 a SEC-06). Este incremento cierra las 4 que requieren cambios de código y documenta las 2 que se resuelven por diseño o infraestructura.

---

## 2. Alcance

### Dentro del alcance

| ID | Brecha | Tipo |
|----|--------|------|
| SEC-01 | Headers HTTP de seguridad ausentes | Código — `SecurityConfig.java` |
| SEC-02 | CSRF deshabilitado sin justificación documentada | Solo documentación |
| SEC-03 | CORS no configurado | Código — `SecurityConfig.java` |
| SEC-04 | Soportes de actividades sin validación de tipo ni tamaño | Código — `LocalDocumentStorageService.java` |
| SEC-05 | Rate limiting no implementado en aplicación | Solo documentación |
| SEC-06 | `Cache-Control` ausente en respuestas sensibles | Código — `SecurityConfig.java` |
| ARCH | Sección de seguridad en `ARCHITECTURE.md` | Documentación |

### Fuera del alcance de I-SEC

- Logging centralizado / SIEM (decisión institucional SED)
- Gestión de claves con HSM o Vault
- CI/CD con SAST/DAST automatizado
- Rate limiting en código (delegado a infraestructura SED)
- Clasificación formal de datos en Oracle (control de DBA/infraestructura)
- Cambios a la máquina de estados o lógica de negocio de informes

---

## 3. Vectores SED Cubiertos

| Vector SED | Criterios cubiertos |
|------------|---------------------|
| V1.6 — Arquitectura de logging | 1.6.1 (formato común — parcial) |
| V1.7 — Protección de datos | 1.7.1, 1.7.2 (documentación de clasificación) |
| V1.11 — Carga segura de archivos | 1.11.1 (CSP + validación de uploads) |
| V8.1 — Protección general de datos | Cache-Control en respuestas autenticadas |
| V12.1 — Carga de archivos | Validación de tipo y tamaño en soportes |
| V13.1 — Seguridad genérica de servicios web | CORS configurado explícitamente |
| V14.4 — Headers de seguridad HTTP | X-Frame-Options, X-Content-Type-Options, CSP, HSTS, Referrer-Policy, Permissions-Policy |

---

## 4. Decisiones de Arquitectura

### 4.1 CSRF — Decisión documentada (sin cambio de código)

SIGCON usa **JWT Bearer tokens** transmitidos en el header `Authorization`. Los navegadores no envían automáticamente headers de autorización en solicitudes cross-site, lo que elimina la superficie de ataque CSRF.

La referencia normativa es OWASP ASVS 4.0.3 §3.5.3: aplicaciones que usan tokens Bearer en headers no son vulnerables a CSRF por diseño.

`http.csrf().disable()` permanece en `SecurityConfig.java` con comentario que referencia esta decisión y el presente documento.

### 4.2 Rate Limiting — Decisión documentada (sin cambio de código)

La infraestructura SED (WebLogic + WAF) gestiona throttling y protección anti-fuerza-bruta a nivel de red. No se implementa rate limiting en la aplicación para evitar duplicidad de controles.

Si en algún momento el despliegue cambia a un entorno sin WAF, esta decisión debe revisarse e implementarse a nivel de aplicación (Bucket4j o filtro Spring).

---

## 5. Diseño de Cambios de Código

### 5.1 `SecurityConfig.java` — Headers HTTP (SEC-01 + SEC-06)

Se activa `headers()` en la configuración de Spring Security con:

```java
http.headers()
    .frameOptions().deny()                          // X-Frame-Options: DENY
    .contentTypeOptions()                           // X-Content-Type-Options: nosniff
    .httpStrictTransportSecurity()
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true)
        .and()
    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
    .and()
    .permissionsPolicy(policy -> policy
        .policy("geolocation=(), camera=(), microphone=()"))
    .and()
    .contentSecurityPolicy(
        "default-src 'self'; " +
        "script-src 'self'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data:; " +
        "frame-ancestors 'none'")
    .and()
    .cacheControl();                                // Cache-Control: no-store
```

`unsafe-inline` en `style-src` es necesario porque Angular y PrimeNG inyectan estilos inline en runtime. Todo lo demás usa `'self'`.

### 5.2 `SecurityConfig.java` — CORS (SEC-03)

Se agrega un `CorsConfigurationSource` bean:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(
        Arrays.asList(corsAllowedOrigins.split(",")));
    config.setAllowedMethods(
        Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(
        Arrays.asList("Authorization","Content-Type","X-Requested-With"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Propiedad en `application.yml`:

```yaml
# local-dev
sigcon:
  security:
    cors-allowed-origins: http://localhost:4200

# weblogic
sigcon:
  security:
    cors-allowed-origins: ${SIGCON_CORS_ALLOWED_ORIGINS:}
```

`@Value("${sigcon.security.cors-allowed-origins:}")` inyectado en `SecurityConfig`.

Si `SIGCON_CORS_ALLOWED_ORIGINS` está vacío en producción, no se permite ningún origen externo. El operador de infraestructura SED configura la variable al desplegar.

### 5.3 `LocalDocumentStorageService.java` — Validación de archivos (SEC-04)

Se agrega validación en `storeFile()` antes de escribir en disco:

**Extensiones y content-types permitidos para soportes:**

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

**Límite de tamaño:** 10 MB configurable via `sigcon.storage.max-file-size-bytes` (default: `10485760`).

**Nuevos códigos de error en `ErrorCode.java`:**
- `SOPORTE_FORMATO_INVALIDO` — extensión o content-type no permitido
- `SOPORTE_TAMANIO_EXCEDIDO` — archivo supera el límite configurado

La validación de `DocumentoRequeridoInformeService` (PDF/EML) permanece sin cambio — es más restrictiva y correcta para su contexto.

### 5.4 `DevSecurityConfig.java`

Recibe la misma configuración de CORS y todos los headers de seguridad que `SecurityConfig.java`, **excepto HSTS**. HSTS no se aplica en local-dev porque el perfil corre sobre HTTP (`localhost`); activarlo haría que el browser rechace conexiones HTTP futuras al mismo host, rompiendo el ambiente de desarrollo.

El resto de headers (`X-Frame-Options`, `X-Content-Type-Options`, `CSP`, `Referrer-Policy`, `Permissions-Policy`, `Cache-Control`) sí se aplican en local-dev para detectar problemas de CSP o headers durante pruebas funcionales antes de llegar a producción.

---

## 6. Tests

### Backend

Tests nuevos en `LocalDocumentStorageServiceTest`:

| Test | Descripción |
|------|-------------|
| `storeFile_extensionNoPermitida_lanzaSoporteFormatoInvalido` | Archivo `.exe` → `SigconBusinessException(SOPORTE_FORMATO_INVALIDO)` |
| `storeFile_tamanioExcedido_lanzaSoporteTamanioExcedido` | Archivo > 10 MB → `SigconBusinessException(SOPORTE_TAMANIO_EXCEDIDO)` |
| `storeFile_pdfValido_almacenaCorrectamente` | Archivo `.pdf` válido → almacenado sin excepción |

Tests de regresión esperados sin cambio:
- `SigconBackendSecurityTest` — headers de seguridad presentes en respuestas
- Suite completa `mvn test` — 0 fallos

### Frontend

No hay cambios de código Angular en este incremento. La validación de tipo ya implementada en `DocumentoRequeridoService` permanece. El `SoporteAdjuntoService` sube el archivo al backend que ahora valida — no requiere cambio de tests Angular.

---

## 7. Tareas

| Tarea | Descripción | Archivos |
|-------|-------------|---------|
| T0 | Documentación base: spec, plan, execution log I-SEC | `docs/specs/`, `docs/plans/` |
| T1 | Sección de seguridad en `ARCHITECTURE.md` | `docs/ARCHITECTURE.md` |
| T2 | Headers + CORS + Cache-Control en `SecurityConfig.java` y `DevSecurityConfig.java` + `application.yml` | `config/SecurityConfig.java`, `config/DevSecurityConfig.java`, `src/main/resources/application.yml` |
| T3 | Validación tipo y tamaño en `LocalDocumentStorageService` + `ErrorCode` + tests | `LocalDocumentStorageService.java`, `ErrorCode.java`, `LocalDocumentStorageServiceTest.java` |
| T4 | Validación final, `mvn test` completo, cierre de execution log | Ejecución |

**Orden:** `T0 → T1 → T2 → T3 → T4`

---

## 8. Criterios de Aceptación

- [ ] `X-Frame-Options: DENY` presente en todas las respuestas HTTP.
- [ ] `X-Content-Type-Options: nosniff` presente en todas las respuestas HTTP.
- [ ] `Content-Security-Policy` configurado con `frame-ancestors 'none'` y fuentes restringidas a `'self'`.
- [ ] `Strict-Transport-Security` con `max-age=31536000; includeSubDomains` presente.
- [ ] `Referrer-Policy: strict-origin-when-cross-origin` presente.
- [ ] `Cache-Control: no-cache, no-store` presente en respuestas de endpoints autenticados.
- [ ] CORS responde con `Access-Control-Allow-Origin` solo para los orígenes configurados en `SIGCON_CORS_ALLOWED_ORIGINS`.
- [ ] Upload de soporte con extensión `.exe` retorna error `SOPORTE_FORMATO_INVALIDO`.
- [ ] Upload de soporte con archivo > 10 MB retorna error `SOPORTE_TAMANIO_EXCEDIDO`.
- [ ] Upload de soporte `.pdf` válido se almacena correctamente.
- [ ] `ARCHITECTURE.md` contiene sección de seguridad con headers, CORS, justificación CSRF y rate limiting.
- [ ] Suite completa backend: 0 fallos.

---

## 9. Riesgos y Controles

| Riesgo | Control |
|--------|---------|
| CSP rompe estilos Angular/PrimeNG | `unsafe-inline` en `style-src` para estilos; validar en local-dev antes de cerrar T2 |
| HSTS bloquea HTTP en local-dev | Aplicar HSTS solo en perfil `weblogic`; omitir en `DevSecurityConfig` |
| CORS vacío bloquea peticiones en producción | Variable `SIGCON_CORS_ALLOWED_ORIGINS` debe configurarse antes de desplegar; documentado en `ARRANQUE.md` |
| Límite de 10 MB rompe uploads legítimos | Valor configurable; ajustable sin redespliegue si cambia la necesidad |
