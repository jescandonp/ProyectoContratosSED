# Execution Log — SIGCON I-SEC
## Controles de Seguridad Baseline

> **Spec:** `docs/specs/2026-05-15-sigcon-i-sec-spec.md`
> **Plan:** `docs/superpowers/plans/2026-05-15-sigcon-i-sec.md`
> **Rama:** `feat/sigcon-i-sec`
> **Inicio:** 2026-05-15
> **Estado:** COMPLETO

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
| T0 | Documentación base I-SEC | COMPLETO | 52c2367 |
| T1 | Sección seguridad en ARCHITECTURE.md | COMPLETO | ccaff5a |
| T2 | Headers + CORS + Cache-Control en SecurityConfig | COMPLETO | ba04934 |
| T3 | Validación archivos en LocalDocumentStorageService | COMPLETO | 28370b1 |
| T4 | Validación final y cierre | COMPLETO | — |

---

## Registro de Ejecución

### 2026-05-15 — Apertura I-SEC

- Análisis de cumplimiento N2 completado contra 67 criterios en 14 vectores.
- Design doc aprobado: `docs/superpowers/specs/2026-05-15-sigcon-i-sec-design.md`.
- Plan de implementación creado.
- Documentación base (spec + execution log) iniciada.

---

### 2026-05-15 — T1 Sección de seguridad en ARCHITECTURE.md

**Cambios:**
- `docs/ARCHITECTURE.md`: sección `## 7-bis. Seguridad y Controles` insertada antes de `## 8. Infraestructura`.
- Subsecciones: Headers HTTP, CORS, CSRF (decisión documentada), Rate Limiting (decisión documentada), Validación de Archivos.

**Commit:** ccaff5a

---

### 2026-05-15 — T2 Headers + CORS + Cache-Control

**Cambios:**
- `SecurityConfig.java`: headers(), CORS bean, HSTS incluido (perfil weblogic).
- `DevSecurityConfig.java`: headers() sin HSTS (perfil local-dev), CORS bean.
- `application.yml`: `sigcon.security.cors-allowed-origins` en ambos perfiles; `sigcon.storage.max-file-size-bytes: 10485760` en local-dev.
- `SigconBackendSecurityTest`: test `securityHeaders_deSeguridad_presentesEnRespuesta`.

**Validaciones:**
- `mvn test -Dtest=SigconBackendSecurityTest` — 15 tests, 0 fallos.

**Commit:** ba04934

---

### 2026-05-15 — T3 Validación de archivos

**Cambios:**
- `ErrorCode.java`: `SOPORTE_FORMATO_INVALIDO`, `SOPORTE_TAMANIO_EXCEDIDO`.
- `LocalDocumentStorageService.java`: constructor con `maxFileSizeBytes`, constante `ALLOWED_TYPES` (9 extensiones), método `validateSoporte()`.
- `LocalDocumentStorageServiceTest.java`: 3 tests nuevos (extensión inválida, tamaño excedido, PDF válido).

**Validaciones:**
- `mvn test -Dtest=LocalDocumentStorageServiceTest,SoporteAdjuntoServiceTest` — 5 tests, 0 fallos.

**Commit:** 28370b1

---

### 2026-05-15 — T4 Validación final

**Suite completa:**
- `mvn test` — 182 tests, 0 fallos, BUILD SUCCESS.

**Estado:** COMPLETO

## Próximo Punto de Retoma

I-SEC cerrado. SIGCON cumple controles de seguridad N2 corregibles en código de aplicación.
Controles pendientes de decisión institucional: logging centralizado / SIEM, escaneo de dependencias SAST/DAST automatizado en CI/CD.
