# Plan de Implementación — SIGCON Incremento 9
## Visto Bueno Administrativo en el Flujo de Informes

> **Para ejecución agente:** usar `superpowers:executing-plans` o `superpowers:subagent-driven-development` para implementar tarea a tarea.

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored
> **Versión:** 1.0 — **Fecha:** 2026-05-19
> **Spec de referencia:** `docs/specs/2026-05-19-sigcon-i9-spec.md`
> **Rama:** `main`
> **Estado:** Listo para ejecución

---

## Resumen Ejecutivo

Incremento de **14 tareas**. El orden prioriza base de datos → enums → servicios backend (en capas) → API → seguridad → notificaciones → frontend (modelos → vistas → admin → componentes compartidos) → validación final.

| Tarea | Área | Descripción |
|-------|------|-------------|
| T0 | Documentación | Abrir execution log I9 |
| T1 | BD | Scripts SQL: `SGCN_PARAMETROS` + columna `ACCION` en observaciones |
| T2 | Backend — Enums | `EN_VISTO_BUENO` en `EstadoInforme`; `ADMINISTRATIVO` en `Rol` |
| T3 | Backend — Entidad/Repo | Entidad `SgcnParametro` + repositorio JPA |
| T4 | Backend — ParametroService | `isVbActivo()` + `setVbActivo()` con migración automática |
| T5 | Backend — InformeService bifurcación | `enviar()` y `aprobarRevision()` consultan flag VB |
| T6 | Backend — InformeService acciones Admin | `darVistosBueno()`, `escalar()`, `devolver()` extendido |
| T7 | Backend — API | `InformeController` (endpoints VB) + `ParametroController` |
| T8 | Backend — Seguridad | Rol `ADMINISTRATIVO` en `SecurityConfig` + usuario de prueba local-dev |
| T9 | Backend — Notificaciones | Nuevos eventos VB en `NotificacionService` |
| T10 | Frontend — Modelos | Enums TypeScript `EstadoInforme` y `Rol` |
| T11 | Frontend — Feature visto-bueno | Cola + Detalle + Guard + Rutas |
| T12 | Frontend — Admin toggle VB | Sección parámetros + modal confirmación |
| T13 | Frontend — Componentes compartidos | Chip `EN_VISTO_BUENO` en badge + ítem menú lateral |
| T14 | Validación final | Suite completa + smoke test + cierre execution log |

---

## Mapa de Archivos

### Nuevos — Backend

| Archivo | Responsabilidad |
|---------|----------------|
| `db/06_sgcn_parametros.sql` | DDL tabla `SGCN_PARAMETROS` + INSERT inicial `VB_ACTIVO=S` |
| `db/07_observaciones_accion.sql` | `ALTER TABLE SGCN_OBSERVACIONES ADD ACCION VARCHAR2(20)` |
| `domain/entity/SgcnParametro.java` | Entidad JPA para `SGCN_PARAMETROS` |
| `domain/repository/SgcnParametroRepository.java` | Repositorio JPA |
| `application/service/ParametroService.java` | Lógica de negocio: leer/escribir parámetros + migración |
| `web/controller/ParametroController.java` | Endpoints `GET/PUT /api/admin/parametros` |
| `application/dto/parametro/ParametroVbDto.java` | DTO para request/response del toggle VB |

### Nuevos — Backend Tests

| Archivo | Qué verifica |
|---------|-------------|
| `test/.../ParametroServiceTest.java` | `isVbActivo()`, `setVbActivo(true/false)`, migración de estados |
| `test/.../InformeServiceVbTest.java` | Bifurcación en `enviar()` y `aprobarRevision()`; 3 acciones del Admin |
| `test/.../ParametroControllerTest.java` | Endpoints GET/PUT parámetros; autorización ADMIN |
| `test/.../InformeControllerVbTest.java` | Endpoints VB: cola, dar-visto-bueno, escalar; autorización ADMINISTRATIVO |
| `test/.../SigconBackendSecurityTest.java` | Extender — verificar que ADMINISTRATIVO no accede a rutas Supervisor |

### Modificados — Backend

| Archivo | Cambio |
|---------|--------|
| `domain/entity/enums/EstadoInforme.java` | + `EN_VISTO_BUENO` |
| `domain/entity/enums/Rol.java` | + `ADMINISTRATIVO` |
| `application/service/InformeService.java` | Bifurcación VB en `enviar()` y `aprobarRevision()`; nuevos métodos `darVistosBueno()`, `escalar()`; extender `devolver()` |
| `web/controller/InformeController.java` | + `GET /cola/visto-bueno`; + `POST /{id}/dar-visto-bueno`; + `POST /{id}/escalar`; extender `devolver` para ADMINISTRATIVO |
| `config/SecurityConfig.java` | Rol `ADMINISTRATIVO` en cadena de seguridad weblogic |
| `config/DevSecurityConfig.java` | Usuario de prueba `administrativo/admin123` |
| `application/service/NotificacionService.java` | Eventos: `INFORME_EN_VISTO_BUENO`, `VB_DADO`, `VB_ESCALADO`, `VB_DEVUELTO` |

### Nuevos — Frontend

| Archivo | Responsabilidad |
|---------|----------------|
| `features/visto-bueno/cola-visto-bueno.component.ts` | Tabla de informes en `EN_VISTO_BUENO` (cola compartida) |
| `features/visto-bueno/cola-visto-bueno.component.html` | Template tabla PrimeNG — patrón cola Revisor |
| `features/visto-bueno/detalle-visto-bueno.component.ts` | Detalle readonly + 3 acciones (Dar VB / Escalar / Devolver) |
| `features/visto-bueno/detalle-visto-bueno.component.html` | Template detalle + barra de acciones |
| `core/guards/administrativo.guard.ts` | Guard de ruta — patrón `revisorGuard` |

### Modificados — Frontend

| Archivo | Cambio |
|---------|--------|
| `core/models/informe.model.ts` | + `EN_VISTO_BUENO` en enum `EstadoInforme` |
| `core/models/usuario.model.ts` | + `ADMINISTRATIVO` en enum `Rol` |
| `app.routes.ts` | + rutas `/visto-bueno` y `/visto-bueno/:id` con guard |
| `layout/sidebar/sidebar.component.ts` | + ítem "Visto Bueno" visible para `ADMINISTRATIVO` |
| `shared/components/estado-badge/` | + chip `EN_VISTO_BUENO` color `#FFB300` etiqueta "En Visto Bueno" |
| `features/admin/` | + sección "Parámetros del sistema" con toggle VB + modal confirmación |

---

## T0 — Documentación base I9

**Archivos:**
- Crear: `docs/plans/2026-05-19-sigcon-i9-execution-log.md`

- [ ] Crear el execution log I9 en `docs/plans/2026-05-19-sigcon-i9-execution-log.md` con encabezado: spec, plan, rama, inicio, estado ABIERTO y tareas T0–T14 sin marcar
- [ ] Commit: `docs(i9): abrir execution log I9`

---

## T1 — Base de Datos

**Archivos:**
- Crear: `db/06_sgcn_parametros.sql`
- Crear: `db/07_observaciones_accion.sql`
- Modificar: `db/03_reset_informes_local_dev.sql` — agregar DROP/TRUNCATE de `SGCN_PARAMETROS` si aplica al entorno local

**Descripción:** Dos scripts DDL independientes e idempotentes. El primero crea la tabla de parámetros con el INSERT inicial que activa el VB. El segundo agrega la columna `ACCION` a observaciones con `DEFAULT NULL` para no afectar registros existentes.

**Reglas:**
- `SGCN_PARAMETROS.CLAVE` es PK — sin duplicados posibles
- `SGCN_OBSERVACIONES.ACCION DEFAULT NULL` — retrocompatible con todas las observaciones anteriores
- Scripts deben ser ejecutables en Oracle 19c y en H2 (local-dev); verificar sintaxis H2 compatible o añadir guarda de perfil

**Gate:** Ejecutar ambos scripts contra la BD local de desarrollo sin errores antes de continuar.

- [ ] Escribir `db/06_sgcn_parametros.sql`
- [ ] Escribir `db/07_observaciones_accion.sql`
- [ ] Verificar que `db/03_reset_informes_local_dev.sql` incluye limpieza de `SGCN_PARAMETROS` si el entorno local lo requiere
- [ ] Ejecutar ambos scripts en BD local — 0 errores
- [ ] Commit: `feat(i9): DDL SGCN_PARAMETROS y columna ACCION en observaciones`

---

## T2 — Enums Java

**Archivos:**
- Modificar: `domain/entity/enums/EstadoInforme.java`
- Modificar: `domain/entity/enums/Rol.java`

**Descripción:** Agregar los dos valores nuevos a los enums existentes. No requiere tests propios — el compilador garantiza coherencia; las pruebas de servicio en T4–T6 los ejercerán.

**Precaución:** Verificar si hay `switch` sin `default` sobre `EstadoInforme` en el codebase — un valor nuevo puede producir comportamiento inesperado si algún `switch` no lo maneja.

- [ ] Buscar todos los `switch` sobre `EstadoInforme` en el proyecto backend
- [ ] Agregar `EN_VISTO_BUENO` a `EstadoInforme.java` — añadir rama en cualquier `switch` exhaustivo encontrado
- [ ] Agregar `ADMINISTRATIVO` a `Rol.java`
- [ ] Compilar: `mvn compile -pl sigcon-backend` — 0 errores
- [ ] Commit: `feat(i9): agregar EN_VISTO_BUENO y ADMINISTRATIVO a enums`

---

## T3 — Entidad `SgcnParametro` y Repositorio

**Archivos:**
- Crear: `domain/entity/SgcnParametro.java`
- Crear: `domain/repository/SgcnParametroRepository.java`

**Descripción:** Entidad JPA mapeada a `SGCN_PARAMETROS`. Clave primaria `String clave`. Campo `String valor`. Campo opcional `String descripcion`. Repositorio extiende `JpaRepository<SgcnParametro, String>` — no requiere métodos adicionales; `findById(clave)` es suficiente.

Seguir el patrón de entidades existentes en el proyecto: anotaciones `@Entity`, `@Table(name="SGCN_PARAMETROS")`, sin herencia de auditoría (esta tabla no requiere `CREATED_AT/UPDATED_AT`).

- [ ] Crear `SgcnParametro.java` con anotaciones JPA
- [ ] Crear `SgcnParametroRepository.java`
- [ ] Compilar: `mvn compile -pl sigcon-backend` — 0 errores
- [ ] Commit: `feat(i9): entidad SgcnParametro y repositorio`

---

## T4 — ParametroService

**Archivos:**
- Crear: `application/service/ParametroService.java`
- Crear: `application/dto/parametro/ParametroVbDto.java`
- Crear: `test/.../ParametroServiceTest.java`

**Descripción:** Servicio con dos métodos públicos:

1. `boolean isVbActivo()` — lee `SGCN_PARAMETROS` por clave `VB_ACTIVO`; retorna `true` si valor es `"S"`, `false` para cualquier otro valor o si la clave no existe (fail-safe: si no existe el parámetro, el flujo funciona como antes = VB inactivo)
2. `void setVbActivo(boolean activo)` — persiste el nuevo valor (`"S"` / `"N"`) y, si `activo = false`, ejecuta en la misma transacción `@Transactional` el UPDATE que mueve todos los `EN_VISTO_BUENO → EN_REVISION`

`ParametroVbDto` es el DTO de request/response para el endpoint: campo `boolean activo`.

**Tests requeridos (TDD — escribir antes de implementar):**
- `isVbActivo_cuandoParametroS_retornaTrue`
- `isVbActivo_cuandoParametroN_retornaFalse`
- `isVbActivo_cuandoParametroNoExiste_retornaFalse`
- `setVbActivo_desactivar_migraInformesEnVistoBueno`
- `setVbActivo_activar_noEjecutaMigracion`

**Gate:** `mvn test -Dtest=ParametroServiceTest` — 5 tests GREEN.

- [ ] Escribir `ParametroServiceTest.java` con los 5 tests (todos deben fallar — TDD)
- [ ] Ejecutar: confirmar que fallan por clase no encontrada
- [ ] Crear `ParametroVbDto.java`
- [ ] Implementar `ParametroService.java`
- [ ] Ejecutar: `mvn test -Dtest=ParametroServiceTest` — 5 GREEN
- [ ] Commit: `feat(i9): ParametroService — isVbActivo y setVbActivo con migracion`

---

## T5 — InformeService: Bifurcación VB en flujo de envío

**Archivos:**
- Modificar: `application/service/InformeService.java`
- Crear: `test/.../InformeServiceVbBifurcacionTest.java`

**Descripción:** Modificar los métodos que determinan el estado destino al avanzar el informe:

- `enviar()` — actualmente mueve a `EN_REVISION` si no hay Revisor. Con VB: si `isVbActivo()` mueve a `EN_VISTO_BUENO`; si no, sigue a `EN_REVISION`
- `aprobarRevision()` (acción del Revisor) — actualmente mueve a `EN_REVISION`. Con VB: si `isVbActivo()` mueve a `EN_VISTO_BUENO`

La consulta a `ParametroService.isVbActivo()` se inyecta por constructor (patrón del proyecto).

**Tests requeridos:**
- `enviar_sinRevisor_vbActivo_pasaAEnVistoBueno`
- `enviar_sinRevisor_vbInactivo_pasaAEnRevision`
- `aprobarRevision_vbActivo_pasaAEnVistoBueno`
- `aprobarRevision_vbInactivo_pasaAEnRevision`

**Gate:** `mvn test -Dtest=InformeServiceVbBifurcacionTest` — 4 tests GREEN.

- [ ] Escribir `InformeServiceVbBifurcacionTest.java` — 4 tests (fallan por lógica no implementada)
- [ ] Modificar `InformeService.java`: inyectar `ParametroService`, actualizar `enviar()` y `aprobarRevision()`
- [ ] Ejecutar: `mvn test -Dtest=InformeServiceVbBifurcacionTest` — 4 GREEN
- [ ] Ejecutar suite completa: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): bifurcacion VB en enviar() y aprobarRevision()`

---

## T6 — InformeService: Acciones del Actor Administrativo

**Archivos:**
- Modificar: `application/service/InformeService.java`
- Crear: `test/.../InformeServiceVbAccionesTest.java`

**Descripción:** Tres métodos nuevos/extendidos:

1. `darVistosBueno(Long informeId, String observacion)` — valida estado `EN_VISTO_BUENO`, mueve a `EN_REVISION`, persiste observación con `autorRol = ADMINISTRATIVO` y `accion = VISTO_BUENO`; observación opcional
2. `escalar(Long informeId, String observacion)` — igual destino `EN_REVISION`, `accion = ESCALACION`; observación recomendada pero no bloqueante
3. `devolver()` extendido — aceptar actor `ADMINISTRATIVO` cuando estado es `EN_VISTO_BUENO`; `accion = DEVOLUCION`; observación obligatoria

**Tests requeridos:**
- `darVistosBueno_estadoCorrecto_pasaAEnRevision`
- `darVistosBueno_estadoIncorrecto_lanzaExcepcion`
- `escalar_persisteObservacionConAccionEscalacion`
- `devolver_desdeVistoBueno_actorAdministrativo_pasaADevuelto`
- `devolver_desdeVistoBueno_sinObservacion_lanzaExcepcion`

**Gate:** `mvn test -Dtest=InformeServiceVbAccionesTest` — 5 tests GREEN.

- [ ] Escribir `InformeServiceVbAccionesTest.java` — 5 tests
- [ ] Implementar `darVistosBueno()`, `escalar()` y extensión de `devolver()` en `InformeService.java`
- [ ] Ejecutar: `mvn test -Dtest=InformeServiceVbAccionesTest` — 5 GREEN
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): acciones Admin darVistosBueno, escalar y devolver desde EN_VISTO_BUENO`

---

## T7 — API: Endpoints VB e InformeController / ParametroController

**Archivos:**
- Modificar: `web/controller/InformeController.java`
- Crear: `web/controller/ParametroController.java`
- Crear: `test/.../InformeControllerVbTest.java`
- Crear: `test/.../ParametroControllerTest.java`

**Descripción:**

`InformeController` — agregar:
- `GET /api/informes/cola/visto-bueno` → `@PreAuthorize("hasRole('ADMINISTRATIVO')")` → llama a servicio que devuelve página de `InformeResumenDto` con estado `EN_VISTO_BUENO`
- `POST /api/informes/{id}/dar-visto-bueno` → `@PreAuthorize("hasRole('ADMINISTRATIVO')")` → body: `{ "observacion": "..." }` (opcional)
- `POST /api/informes/{id}/escalar` → `@PreAuthorize("hasRole('ADMINISTRATIVO')")` → body: `{ "observacion": "..." }` (recomendada)
- El endpoint `POST /api/informes/{id}/devolver` ya existe — extender `@PreAuthorize` para incluir `ADMINISTRATIVO`

`ParametroController` — nuevo:
- `GET /api/admin/parametros` → `@PreAuthorize("hasRole('ADMIN')")` → lista todos los parámetros
- `PUT /api/admin/parametros/vb-activo` → `@PreAuthorize("hasRole('ADMIN')")` → body: `ParametroVbDto { activo: true/false }`

**Tests requeridos — InformeControllerVbTest:**
- `colaVistoBueno_sinAutenticacion_retorna401`
- `colaVistoBueno_rolSupervisor_retorna403`
- `colaVistoBueno_rolAdministrativo_retorna200`
- `darVistosBueno_estadoCorrecto_retorna200`
- `escalar_retorna200`

**Tests requeridos — ParametroControllerTest:**
- `getParametros_rolAdmin_retorna200`
- `getParametros_rolAdministrativo_retorna403`
- `putVbActivo_desactivar_retorna200YMigraEstados`

**Gate:** `mvn test -Dtest=InformeControllerVbTest,ParametroControllerTest` — GREEN.

- [ ] Escribir `InformeControllerVbTest.java` y `ParametroControllerTest.java`
- [ ] Implementar endpoints en `InformeController.java`
- [ ] Crear `ParametroController.java`
- [ ] Ejecutar tests de controladores — GREEN
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): endpoints VB en InformeController y ParametroController`

---

## T8 — Seguridad

**Archivos:**
- Modificar: `config/SecurityConfig.java`
- Modificar: `config/DevSecurityConfig.java`
- Modificar: `test/.../SigconBackendSecurityTest.java`

**Descripción:**

`SecurityConfig` (perfil `weblogic`) — agregar `ADMINISTRATIVO` como rol válido en la cadena de Spring Security. Revisar la lista de roles permitidos globalmente y en reglas específicas de endpoints.

`DevSecurityConfig` (perfil `local-dev`) — agregar usuario en memoria:
- Username: `administrativo`
- Password: `admin123` (igual que los demás usuarios de prueba)
- Rol: `ADMINISTRATIVO`

**Tests a agregar en `SigconBackendSecurityTest`:**
- `administrativo_puedeAcceder_colaVistoBueno`
- `administrativo_noPuedeAcceder_colaRevision` (acceso a cola del Supervisor rechazado)
- `supervisor_noPuedeAcceder_colaVistoBueno`

**Gate:** `mvn test -Dtest=SigconBackendSecurityTest` — todos los tests existentes + nuevos GREEN.

- [ ] Agregar rol `ADMINISTRATIVO` en `SecurityConfig.java`
- [ ] Agregar usuario `administrativo/admin123` en `DevSecurityConfig.java`
- [ ] Agregar 3 tests de seguridad en `SigconBackendSecurityTest.java`
- [ ] Ejecutar: `mvn test -Dtest=SigconBackendSecurityTest` — GREEN
- [ ] Commit: `feat(i9): rol ADMINISTRATIVO en SecurityConfig y usuario local-dev`

---

## T9 — Notificaciones

**Archivos:**
- Modificar: `application/service/NotificacionService.java`
- Modificar: test de notificaciones existente o crear `NotificacionVbTest.java`

**Descripción:** Agregar los cuatro nuevos eventos de notificación siguiendo el patrón de los eventos ya existentes (INFORME_ENVIADO, REVISOR_DEVUELVE, SUPERVISOR_APRUEBA, etc.):

| Evento nuevo | Disparado en | Destinatario |
|-------------|-------------|-------------|
| `INFORME_EN_VISTO_BUENO` | Al entrar a `EN_VISTO_BUENO` | Todos los usuarios `ADMINISTRATIVO` (email + in-app) |
| `VB_DADO` | Al ejecutar `darVistosBueno()` | Supervisor del contrato (email + in-app) |
| `VB_ESCALADO` | Al ejecutar `escalar()` | Supervisor del contrato (email + in-app) |
| `VB_DEVUELTO` | Al ejecutar `devolver()` desde `EN_VISTO_BUENO` | Contratista (email + in-app) |

Para `INFORME_EN_VISTO_BUENO`: consultar todos los usuarios con rol `ADMINISTRATIVO` en `SGCN_USUARIOS` y notificar a cada uno.

**Gate:** `mvn test` — 0 fallos, nuevos eventos cubiertos.

- [ ] Identificar el patrón de notificación existente en el servicio (cómo se registran destinatarios y se envían emails)
- [ ] Agregar los 4 nuevos tipos de evento
- [ ] Implementar la consulta de usuarios ADMINISTRATIVO para el evento `INFORME_EN_VISTO_BUENO`
- [ ] Escribir tests de los 4 eventos
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): notificaciones para eventos de Visto Bueno`

---

## T10 — Frontend: Modelos TypeScript

**Archivos:**
- Modificar: `core/models/informe.model.ts`
- Modificar: `core/models/usuario.model.ts`

**Descripción:** Agregar los dos valores nuevos a los enums TypeScript existentes. Buscar también en el frontend cualquier `switch` o condicional exhaustivo sobre `EstadoInforme` — añadir rama para `EN_VISTO_BUENO` donde haga falta (ej: traducciones, colores, labels).

- [ ] Buscar usos de `EstadoInforme` y `Rol` en el frontend (guards, pipes, templates)
- [ ] Agregar `EN_VISTO_BUENO` en `informe.model.ts`
- [ ] Agregar `ADMINISTRATIVO` en `usuario.model.ts`
- [ ] Actualizar cualquier mapeo exhaustivo (labels, colores, traducciones) para incluir el nuevo estado
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): enums TypeScript EN_VISTO_BUENO y ADMINISTRATIVO`

---

## T11 — Frontend: Feature `visto-bueno`

**Archivos:**
- Crear: `features/visto-bueno/cola-visto-bueno.component.ts`
- Crear: `features/visto-bueno/cola-visto-bueno.component.html`
- Crear: `features/visto-bueno/detalle-visto-bueno.component.ts`
- Crear: `features/visto-bueno/detalle-visto-bueno.component.html`
- Crear: `core/guards/administrativo.guard.ts`
- Modificar: `app.routes.ts`

**Descripción:**

**Cola (`cola-visto-bueno`):** tabla PrimeNG con columnas contratista, número de contrato, período, fecha de llegada a VB. Cada fila navega a `/visto-bueno/:id`. Seguir el patrón exacto de la cola del Revisor existente.

**Detalle (`detalle-visto-bueno`):** vista de solo lectura del informe completo (actividades, soportes, documentos adicionales). Barra de acciones fija en la parte inferior con tres botones siguiendo el design system `Prototipo/DESIGN.md`:
- **Dar Visto Bueno** — Primary `#0B3D91` — sin campo de texto obligatorio
- **Escalar a Supervisor** — Secondary `#FFB300` borde dorado — campo de texto recomendado (dialog de confirmación)
- **Devolver al Contratista** — Danger `#92032E` — campo de texto obligatorio (dialog de confirmación)

Los diálogos de Escalar y Devolver usan `p-dialog` de PrimeNG — patrón ya usado en el proyecto para confirmaciones de Revisor/Supervisor.

**Guard:** `administrativoGuard` — retorna `true` si el rol del usuario autenticado es `ADMINISTRATIVO`; redirige a `/acceso-denegado` si no. Mismo patrón que `revisorGuard`.

**Rutas:**
```
/visto-bueno          → ColaVistoBuenoComponent (canActivate: administrativoGuard)
/visto-bueno/:id      → DetalleVistoBuenoComponent (canActivate: administrativoGuard)
```

- [ ] Crear `administrativo.guard.ts` (patrón revisorGuard)
- [ ] Agregar rutas en `app.routes.ts`
- [ ] Crear `cola-visto-bueno.component.ts/.html` (patrón cola Revisor)
- [ ] Crear `detalle-visto-bueno.component.ts/.html` con barra de 3 acciones
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): feature visto-bueno — cola, detalle, guard y rutas`

---

## T12 — Frontend: Admin Toggle VB

**Archivos:**
- Modificar: `features/admin/` (sección o componente de configuración existente; identificar el archivo exacto al implementar)

**Descripción:** Nueva sección **"Parámetros del sistema"** dentro de la vista de administración ya existente. Contiene:

- `p-inputSwitch` de PrimeNG con label "Visto Bueno Administrativo"
- El switch muestra el estado actual cargado desde `GET /api/admin/parametros`
- Al intentar desactivar (de `true` a `false`): mostrar `p-dialog` de confirmación con mensaje:
  > *"Los informes en espera de Visto Bueno serán enviados automáticamente al Supervisor. ¿Desea continuar?"*
  - Botón **Cancelar** (Secondary) — revierte el switch sin llamar al API
  - Botón **Confirmar** (Primary `#0B3D91`) — llama `PUT /api/admin/parametros/vb-activo` con `{ activo: false }`
- Al activar (de `false` a `true`): llamar directamente sin confirmación
- Manejar error del API: mostrar mensaje de error (`p-toast`) y revertir el switch

- [ ] Identificar el componente de administración donde agregar la sección
- [ ] Agregar sección "Parámetros del sistema" con `p-inputSwitch`
- [ ] Implementar carga del estado actual desde el API
- [ ] Implementar modal de confirmación al desactivar
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): toggle VB en panel de administración`

---

## T13 — Frontend: Componentes Compartidos

**Archivos:**
- Modificar: `shared/components/estado-badge/` (o equivalente en el proyecto — verificar nombre exacto al implementar)
- Modificar: `layout/sidebar/sidebar.component.ts` (o equivalente)

**Descripción:**

**Chip de estado** — agregar `EN_VISTO_BUENO` siguiendo la convención de `Chips & Status Indicators` del `Prototipo/DESIGN.md`:

| Estado | Color fondo | Color texto | Etiqueta |
|--------|------------|-------------|----------|
| `EN_VISTO_BUENO` | `#FFB300` (Institutional Gold) | `#281900` (on-secondary-fixed) | En Visto Bueno |

El dorado institucional es el token semántico correcto para "requiere atención" sin urgencia del rojo (`DEVUELTO`).

**Menú lateral** — agregar ítem **"Visto Bueno"** (con ícono apropiado de PrimeNG) visible únicamente cuando el rol del usuario autenticado es `ADMINISTRATIVO`. Seguir el patrón de visibilidad condicional ya usado para los ítems de Revisor y Supervisor.

- [ ] Agregar chip `EN_VISTO_BUENO` en el componente de badge de estado
- [ ] Agregar ítem "Visto Bueno" en el sidebar con visibilidad condicional para `ADMINISTRATIVO`
- [ ] Verificar que el chip aparece correctamente en todas las vistas donde se muestra el estado del informe
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): chip EN_VISTO_BUENO y menu lateral ADMINISTRATIVO`

---

## T14 — Validación Final y Cierre

**Descripción:** Verificación end-to-end del incremento completo antes de declarar I9 cerrado.

**Gate backend — suite completa:**
- `mvn test` — 0 fallos, todas las tareas cubiertas por tests

**Gate frontend — build limpio:**
- `ng build` — 0 errores TypeScript, 0 advertencias críticas

**Smoke test funcional (perfil local-dev):**
- [ ] Iniciar backend con perfil `local-dev`
- [ ] Login como `contratista/contratista123` — crear y enviar informe
- [ ] Login como `administrativo/admin123` — verificar que el informe aparece en cola VB
- [ ] Dar Visto Bueno — verificar que pasa a cola del Supervisor
- [ ] Repetir: enviar otro informe y ejecutar Devolver — verificar que vuelve al contratista con observación
- [ ] Repetir: enviar otro informe y ejecutar Escalar — verificar que pasa al Supervisor con `accion = ESCALACION`
- [ ] Login como `admin/admin123` — desactivar VB en parámetros — confirmar migración automática si hay informes en VB
- [ ] Reactivar VB — verificar que el flujo vuelve a incluir el paso administrativo

**Documentación de cierre:**
- [ ] Actualizar `docs/ARRANQUE.md` — agregar usuario de prueba `administrativo/admin123` en tabla de credenciales
- [ ] Actualizar `docs/GUIA_PRUEBAS_FUNCIONALES.md` — agregar sección I9 con casos de prueba del flujo VB
- [ ] Cerrar execution log: marcar todas las tareas completadas, registrar SHA de commits, estado → CERRADO
- [ ] Commit final: `docs(i9): cierre — ejecucion log, arranque y guia de pruebas`

---

## Punto de Retoma

Al iniciar la sesión de implementación:

1. Leer `docs/plans/2026-05-19-sigcon-i9-execution-log.md` — verificar última tarea completada
2. Continuar desde la primera tarea sin marcar
3. Antes de cada tarea: `mvn compile` (backend) o `ng build` (frontend) para confirmar base limpia
4. Respetar la rama `main` — commits pequeños y trazables por tarea

---

*Plan generado mediante SDD — SIGCON — Incremento 9 — 2026-05-19.*
