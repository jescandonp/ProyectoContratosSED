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
