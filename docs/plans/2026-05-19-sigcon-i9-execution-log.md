# Execution Log — SIGCON I9
## Visto Bueno Administrativo en el Flujo de Informes

> **Spec:** `docs/specs/2026-05-19-sigcon-i9-spec.md`
> **Plan:** `docs/plans/2026-05-19-sigcon-i9-plan.md`
> **Rama:** `main`
> **Inicio:** 2026-05-20
> **Estado:** ABIERTO — en progreso (T8 parcial, T9–T14 pendientes)

---

## Contexto de Retoma

- I8 se encuentra cerrado en `docs/plans/2026-05-18-sigcon-i8-execution-log.md`.
- I9 inicia desde `main`, conforme a la spec y plan aprobados el 2026-05-19.
- Inconsistencia documentada: el plan nombra `InformeService.java` como archivo de la maquina de estados, pero el codigo real usa `InformeEstadoService.java`. Se aplica el codigo vigente (CONSTITUTION §autoridad).
- Inconsistencia documentada: el plan nombra `Rol.java`, pero el enum canonico es `RolUsuario.java`.
- `TipoEvento.java` ya contiene los 4 eventos VB (INFORME_EN_VISTO_BUENO, VB_DADO, VB_ESCALADO, VB_DEVUELTO) — fueron agregados en una sesion anterior. T9 debe implementar la logica de disparo en `EventoInformeService`.

---

## Checklist I9

### T0 — Documentacion base I9

- [x] Crear el execution log I9
- [x] Commit: `docs(i9): abrir execution log I9` — `6bf04bd`

### T1 — Base de Datos

- [x] Escribir `db/06_sgcn_parametros.sql`
- [x] Escribir `db/07_observaciones_accion.sql`
- [x] Verificar `db/03_reset_informes_local_dev.sql`
- [x] Ejecutar ambos scripts en BD local — 0 errores
- [x] Commit: `feat(i9): DDL SGCN_PARAMETROS y columna ACCION en observaciones` — `61a8955`

### T2 — Enums Java

- [x] Buscar `switch` sobre `EstadoInforme` — ninguno exhaustivo encontrado
- [x] Agregar `EN_VISTO_BUENO` a `EstadoInforme.java`
- [x] Agregar `ADMINISTRATIVO` a `RolUsuario.java`
- [x] Compilar — BUILD SUCCESS
- [x] Commit: `feat(i9): agregar EN_VISTO_BUENO y ADMINISTRATIVO a enums` — `34fd9b5`

### T3 — Entidad `SgcnParametro` y Repositorio

- [x] Crear `SgcnParametro.java`
- [x] Crear `SgcnParametroRepository.java`
- [x] Compilar — BUILD SUCCESS
- [x] Commit: `feat(i9): entidad SgcnParametro y repositorio` — `c6e7097`

### T4 — ParametroService

- [x] Escribir `ParametroServiceTest.java` — 5 tests RED
- [x] Crear `ParametroVbDto.java`
- [x] Implementar `ParametroService.java`
- [x] `mvn test -Dtest=ParametroServiceTest` — 5 GREEN
- [x] Commit: `feat(i9): ParametroService — isVbActivo y setVbActivo con migracion` — `103c010`

### T5 — InformeEstadoService: Bifurcacion VB en flujo de envio

- [x] Escribir `InformeServiceVbBifurcacionTest.java` — 4 tests
- [x] Modificar `InformeEstadoService.java`: inyectar `ParametroService`, bifurcar `enviar()` y `aprobarRevision()`
- [x] `mvn test -Dtest=InformeServiceVbBifurcacionTest` — 4 GREEN
- [x] `mvn test` — 193 tests, 0 fallos
- [x] Commit: `feat(i9): bifurcacion VB en enviar() y aprobarRevision()` — `00d159c`

### T6 — InformeEstadoService: Acciones del Actor Administrativo

- [x] Agregar `ADMINISTRATIVO` a `RolObservacion.java`
- [x] Agregar campo `accion` a `Observacion.java` (columna ya existia en BD)
- [x] Agregar `registrarConAccion()` a `ObservacionService.java`
- [x] Escribir `InformeServiceVbAccionesTest.java` — 5 tests
- [x] Implementar `darVistosBueno()`, `escalar()`, `devolverDesdeVistoBueno()` en `InformeEstadoService.java`
- [x] `mvn test -Dtest=InformeServiceVbAccionesTest` — 5 GREEN
- [x] `mvn test` — 198 tests, 0 fallos
- [x] Commit: `feat(i9): acciones Admin darVistosBueno, escalar y devolver desde EN_VISTO_BUENO` — `0679cc7`

### T7 — API: Endpoints VB e InformeController / ParametroController

- [x] Agregar `findByEstadoAndActivoTrue()` a `InformeRepository.java`
- [x] Agregar `listarColaVistoBueno()` a `InformeService.java`
- [x] Extender `assertCanViewContrato()` para rol `ADMINISTRATIVO`
- [x] Agregar endpoints VB en `InformeController.java`: `GET /cola/visto-bueno`, `POST /{id}/dar-visto-bueno`, `POST /{id}/escalar`, extender `POST /{id}/devolver`
- [x] Crear `ParametroController.java`: `GET /api/admin/parametros`, `PUT /api/admin/parametros/vb-activo`
- [x] Escribir `InformeControllerVbTest.java` — 5 tests GREEN
- [x] Escribir `ParametroControllerTest.java` — 3 tests GREEN
- [x] `mvn test` — 206 tests, 0 fallos
- [x] Commit: `feat(i9): endpoints VB en InformeController, ParametroController y rol ADMINISTRATIVO en DevSecurityConfig` — `aa3f5b1`

### T8 — Seguridad

- [x] Agregar reglas de endpoints VB en `DevSecurityConfig.java` (perfil local-dev)
- [x] Agregar usuario `administrativo@educacionbogota.edu.co / admin123` en `DevSecurityConfig.java`
- [ ] **PENDIENTE:** Agregar rol `ADMINISTRATIVO` en `SecurityConfig.java` (perfil weblogic/produccion)
- [ ] **PENDIENTE:** Agregar 3 tests de seguridad en `SigconBackendSecurityTest.java`
- [ ] **PENDIENTE:** Commit: `feat(i9): rol ADMINISTRATIVO en SecurityConfig (weblogic)`

### T9 — Notificaciones

> **Nota:** `TipoEvento.java` ya tiene los 4 eventos VB. Solo falta implementar el disparo en `EventoInformeService` y `InformeEstadoService`.

- [ ] Extender `EventoInformeService.resolverDestinatario()` para los 4 eventos VB
- [ ] Extender `EventoInformeService.construirDescripcion()` para los 4 eventos VB
- [ ] Disparar `INFORME_EN_VISTO_BUENO` en `InformeEstadoService.enviar()` y `aprobarRevision()` cuando estado destino es `EN_VISTO_BUENO`
- [ ] Disparar `VB_DADO` en `darVistosBueno()`, `VB_ESCALADO` en `escalar()`, `VB_DEVUELTO` en `devolverDesdeVistoBueno()`
- [ ] Escribir tests de los 4 eventos en `EventoInformeServiceTest.java`
- [ ] `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): notificaciones para eventos de Visto Bueno`

### T10 — Frontend: Modelos TypeScript

- [ ] Agregar `EN_VISTO_BUENO` en `informe.model.ts`
- [ ] Agregar `ADMINISTRATIVO` en `usuario.model.ts`
- [ ] Actualizar mapeos exhaustivos (labels, colores, traducciones)
- [ ] `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): enums TypeScript EN_VISTO_BUENO y ADMINISTRATIVO`

### T11 — Frontend: Feature `visto-bueno`

- [ ] Crear `administrativo.guard.ts`
- [ ] Agregar rutas en `app.routes.ts`
- [ ] Crear `cola-visto-bueno.component.ts/.html`
- [ ] Crear `detalle-visto-bueno.component.ts/.html` con barra de 3 acciones
- [ ] `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): feature visto-bueno — cola, detalle, guard y rutas`

### T12 — Frontend: Admin Toggle VB

- [ ] Identificar componente de administracion existente
- [ ] Agregar seccion "Parametros del sistema" con `p-inputSwitch`
- [ ] Implementar carga desde `GET /api/admin/parametros`
- [ ] Implementar modal de confirmacion al desactivar
- [ ] `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): toggle VB en panel de administracion`

### T13 — Frontend: Componentes Compartidos

- [ ] Agregar chip `EN_VISTO_BUENO` (fondo `#FFB300`, texto `#281900`, etiqueta "En Visto Bueno")
- [ ] Agregar item "Visto Bueno" en sidebar con visibilidad condicional para `ADMINISTRATIVO`
- [ ] `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): chip EN_VISTO_BUENO y menu lateral ADMINISTRATIVO`

### T14 — Validacion Final y Cierre

- [ ] `mvn test` — 0 fallos
- [ ] `ng build` — 0 errores TypeScript
- [ ] Smoke test funcional en `local-dev`
- [ ] Actualizar `docs/ARRANQUE.md` — usuario `administrativo/admin123`
- [ ] Actualizar `docs/GUIA_PRUEBAS_FUNCIONALES.md` — seccion I9
- [ ] Cerrar execution log — estado CERRADO
- [ ] Commit final: `docs(i9): cierre — ejecucion log, arranque y guia de pruebas`

---

## Registro de Ejecucion

### 2026-05-20 — T0 apertura

- Se revisaron `README.md`, `docs/CONSTITUTION.md`, `docs/ARCHITECTURE.md`, `docs/TECNOLOGIAS.md`, `docs/specs/`, `docs/plans/` y el execution log vigente I8.
- Se confirmo que I8 esta cerrado y que I9 es el incremento activo aprobado con plan listo para ejecucion.
- Commit T0: `6bf04bd` — `docs(i9): abrir execution log I9`.

### 2026-05-20 — T1 Base de Datos

- Creados `db/06_sgcn_parametros.sql` y `db/07_observaciones_accion.sql` como scripts incrementales idempotentes Oracle 19c.
- Gate T1 completado: ambos scripts ejecutados contra BD local sin errores.
- Commit T1: `61a8955` — `feat(i9): DDL SGCN_PARAMETROS y columna ACCION en observaciones`.

### 2026-05-20 — T2 Enums Java

- Inconsistencia documentada: spec/plan nombran `Rol.java`, codigo vigente usa `RolUsuario.java`.
- Agregados `EN_VISTO_BUENO` a `EstadoInforme.java` y `ADMINISTRATIVO` a `RolUsuario.java`.
- Commit T2: `34fd9b5` — `feat(i9): agregar EN_VISTO_BUENO y ADMINISTRATIVO a enums`.

### 2026-05-20 — T3 Entidad y Repositorio

- Creados `SgcnParametro.java` y `SgcnParametroRepository.java`.
- Commit T3: `c6e7097` — `feat(i9): entidad SgcnParametro y repositorio`.

### 2026-05-20 — T4 ParametroService

- TDD: 5 tests RED → GREEN.
- Commit T4: `103c010` — `feat(i9): ParametroService — isVbActivo y setVbActivo con migracion`.

### 2026-05-20 — T5 InformeEstadoService: Bifurcacion VB

- Inconsistencia documentada: plan nombra `InformeService`, codigo real es `InformeEstadoService`.
- TDD: 4 tests RED → GREEN. Actualizados 7 tests existentes (constructor nuevo).
- Suite: 193 tests, 0 fallos.
- Commit T5: `00d159c` — `feat(i9): bifurcacion VB en enviar() y aprobarRevision()`.

### 2026-05-20 — T6 Acciones del Actor Administrativo

- Agregados `ADMINISTRATIVO` a `RolObservacion`, campo `accion` a `Observacion`, metodo `registrarConAccion()` a `ObservacionService`.
- Implementados `darVistosBueno()`, `escalar()`, `devolverDesdeVistoBueno()` en `InformeEstadoService`.
- TDD: 5 tests RED → GREEN. Suite: 198 tests, 0 fallos.
- Commit T6: `0679cc7` — `feat(i9): acciones Admin darVistosBueno, escalar y devolver desde EN_VISTO_BUENO`.

### 2026-05-20 — T7 API Endpoints VB

- Agregado `findByEstadoAndActivoTrue()` a `InformeRepository`.
- Agregado `listarColaVistoBueno()` a `InformeService` con acceso para `ADMINISTRATIVO`.
- Nuevos endpoints en `InformeController`: `GET /cola/visto-bueno`, `POST /{id}/dar-visto-bueno`, `POST /{id}/escalar`, `POST /{id}/devolver` extendido.
- Creado `ParametroController` con `GET /api/admin/parametros` y `PUT /api/admin/parametros/vb-activo`.
- Tests: 8 GREEN (5 InformeControllerVbTest + 3 ParametroControllerTest).
- Suite: 206 tests, 0 fallos.
- Commit T7: `aa3f5b1` — `feat(i9): TipoEvento VB, InformeRepository cola VB, InformeService listarColaVistoBueno, InformeController endpoints VB, ParametroController, DevSecurityConfig rol ADMINISTRATIVO`.

### 2026-05-20 — T8 Seguridad (parcial)

- Agregadas reglas de endpoints VB en `DevSecurityConfig.java` (perfil local-dev).
- Agregado usuario `administrativo@educacionbogota.edu.co / admin123` en `DevSecurityConfig.java`.
- **Pendiente:** `SecurityConfig.java` (perfil weblogic) y tests de seguridad en `SigconBackendSecurityTest`.

---

## Punto de Retoma — HANDOFF 2026-05-20

**Estado del backend:** T0–T7 completos. T8 parcial (solo local-dev). T9 pendiente (logica de disparo de eventos).

**Estado del frontend:** T10–T13 pendientes.

**Suite backend actual:** 206 tests, 0 fallos, 0 errores. `mvn test` pasa limpio.

**Commits I9 en main (no pusheados a origin):**
```
aa3f5b1  feat(i9): TipoEvento VB, InformeRepository cola VB, InformeController endpoints VB, ParametroController, DevSecurityConfig
46b1f67  feat(i9): endpoints VB en InformeController, ParametroController y rol ADMINISTRATIVO en DevSecurityConfig
51bdb19  feat(i9): endpoints VB en InformeController y ParametroController; rol ADMINISTRATIVO en DevSecurityConfig
0679cc7  feat(i9): acciones Admin darVistosBueno, escalar y devolver desde EN_VISTO_BUENO
00d159c  feat(i9): bifurcacion VB en enviar() y aprobarRevision()
```

**Proximas tareas en orden:**

1. **T8 (completar):** Agregar `ADMINISTRATIVO` en `SecurityConfig.java` (perfil weblogic). Agregar 3 tests en `SigconBackendSecurityTest`.
2. **T9:** Extender `EventoInformeService` para disparar los 4 eventos VB. Disparar desde `InformeEstadoService`. Tests en `EventoInformeServiceTest`.
3. **T10–T13:** Frontend Angular — modelos, feature visto-bueno, toggle admin, chip de estado.
4. **T14:** Validacion final, smoke test, documentacion de cierre.

**Archivos clave modificados en I9 (backend):**
- `InformeEstadoService.java` — maquina de estados con bifurcacion VB y acciones admin
- `InformeService.java` — cola VB, acceso ADMINISTRATIVO
- `InformeController.java` — endpoints VB
- `ParametroController.java` — nuevo
- `ParametroService.java` — nuevo
- `ObservacionService.java` — registrarConAccion()
- `Observacion.java` — campo accion
- `RolObservacion.java` — + ADMINISTRATIVO
- `EstadoInforme.java` — + EN_VISTO_BUENO
- `RolUsuario.java` — + ADMINISTRATIVO
- `TipoEvento.java` — + 4 eventos VB (ya presentes)
- `DevSecurityConfig.java` — reglas VB + usuario administrativo
- `InformeRepository.java` — findByEstadoAndActivoTrue, migrarEnVistoBuenoAEnRevision
- `SgcnParametro.java`, `SgcnParametroRepository.java` — nuevos
- `db/06_sgcn_parametros.sql`, `db/07_observaciones_accion.sql` — nuevos

**Advertencia para la proxima sesion:** Hay commits locales no pusheados a `origin/main`. Ejecutar `git push` antes de iniciar nueva sesion para sincronizar.
