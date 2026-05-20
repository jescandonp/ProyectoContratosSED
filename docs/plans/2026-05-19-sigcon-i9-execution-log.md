# Execution Log — SIGCON I9
## Visto Bueno Administrativo en el Flujo de Informes

> **Spec:** `docs/specs/2026-05-19-sigcon-i9-spec.md`
> **Plan:** `docs/plans/2026-05-19-sigcon-i9-plan.md`
> **Rama:** `main`
> **Inicio:** 2026-05-20
> **Estado:** ABIERTO

---

## Contexto de Retoma

- I8 se encuentra cerrado en `docs/plans/2026-05-18-sigcon-i8-execution-log.md`.
- I9 inicia desde `main`, conforme a la spec y plan aprobados el 2026-05-19.
- La primera tarea ejecutable del plan activo es T0: abrir este execution log.
- Se detecta una tension operativa menor: `superpowers:executing-plans` recomienda no iniciar implementacion sobre `main`, pero la spec y el plan I9 fijan `Rama: main`; por autoridad documental SIGCON se conserva `main` como rama activa.
- Cambios locales/no versionados existentes al inicio no hacen parte de I9 salvo que el plan activo los toque explicitamente.

## Checklist I9

## T0 — Documentacion base I9

- [x] Crear el execution log I9 en `docs/plans/2026-05-19-sigcon-i9-execution-log.md` con encabezado: spec, plan, rama, inicio, estado ABIERTO y tareas T0-T14 sin marcar
- [x] Commit: `docs(i9): abrir execution log I9` — `6bf04bd`

## T1 — Base de Datos

- [x] Escribir `db/06_sgcn_parametros.sql`
- [x] Escribir `db/07_observaciones_accion.sql`
- [x] Verificar que `db/03_reset_informes_local_dev.sql` incluye limpieza de `SGCN_PARAMETROS` si el entorno local lo requiere
- [x] Ejecutar ambos scripts en BD local — 0 errores
- [x] Commit: `feat(i9): DDL SGCN_PARAMETROS y columna ACCION en observaciones` — `61a8955`

## T2 — Enums Java

- [x] Buscar todos los `switch` sobre `EstadoInforme` en el proyecto backend
- [x] Agregar `EN_VISTO_BUENO` a `EstadoInforme.java` — anadir rama en cualquier `switch` exhaustivo encontrado
- [x] Agregar `ADMINISTRATIVO` a `Rol.java`
- [x] Compilar: `mvn compile -pl sigcon-backend` — 0 errores
- [x] Commit: `feat(i9): agregar EN_VISTO_BUENO y ADMINISTRATIVO a enums` — `34fd9b5`

## T3 — Entidad `SgcnParametro` y Repositorio

- [ ] Crear `SgcnParametro.java` con anotaciones JPA
- [ ] Crear `SgcnParametroRepository.java`
- [ ] Compilar: `mvn compile -pl sigcon-backend` — 0 errores
- [ ] Commit: `feat(i9): entidad SgcnParametro y repositorio`

## T4 — ParametroService

- [ ] Escribir `ParametroServiceTest.java` con los 5 tests
- [ ] Ejecutar: confirmar que fallan por clase no encontrada
- [ ] Crear `ParametroVbDto.java`
- [ ] Implementar `ParametroService.java`
- [ ] Ejecutar: `mvn test -Dtest=ParametroServiceTest` — 5 GREEN
- [ ] Commit: `feat(i9): ParametroService — isVbActivo y setVbActivo con migracion`

## T5 — InformeService: Bifurcacion VB en flujo de envio

- [ ] Escribir `InformeServiceVbBifurcacionTest.java` — 4 tests
- [ ] Modificar `InformeService.java`: inyectar `ParametroService`, actualizar `enviar()` y `aprobarRevision()`
- [ ] Ejecutar: `mvn test -Dtest=InformeServiceVbBifurcacionTest` — 4 GREEN
- [ ] Ejecutar suite completa: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): bifurcacion VB en enviar() y aprobarRevision()`

## T6 — InformeService: Acciones del Actor Administrativo

- [ ] Escribir `InformeServiceVbAccionesTest.java` — 5 tests
- [ ] Implementar `darVistosBueno()`, `escalar()` y extension de `devolver()` en `InformeService.java`
- [ ] Ejecutar: `mvn test -Dtest=InformeServiceVbAccionesTest` — 5 GREEN
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): acciones Admin darVistosBueno, escalar y devolver desde EN_VISTO_BUENO`

## T7 — API: Endpoints VB e InformeController / ParametroController

- [ ] Escribir `InformeControllerVbTest.java` y `ParametroControllerTest.java`
- [ ] Implementar endpoints en `InformeController.java`
- [ ] Crear `ParametroController.java`
- [ ] Ejecutar tests de controladores — GREEN
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): endpoints VB en InformeController y ParametroController`

## T8 — Seguridad

- [ ] Agregar rol `ADMINISTRATIVO` en `SecurityConfig.java`
- [ ] Agregar usuario `administrativo/admin123` en `DevSecurityConfig.java`
- [ ] Agregar 3 tests de seguridad en `SigconBackendSecurityTest.java`
- [ ] Ejecutar: `mvn test -Dtest=SigconBackendSecurityTest` — GREEN
- [ ] Commit: `feat(i9): rol ADMINISTRATIVO en SecurityConfig y usuario local-dev`

## T9 — Notificaciones

- [ ] Identificar el patron de notificacion existente en el servicio
- [ ] Agregar los 4 nuevos tipos de evento
- [ ] Implementar la consulta de usuarios ADMINISTRATIVO para el evento `INFORME_EN_VISTO_BUENO`
- [ ] Escribir tests de los 4 eventos
- [ ] Ejecutar: `mvn test` — 0 regresiones
- [ ] Commit: `feat(i9): notificaciones para eventos de Visto Bueno`

## T10 — Frontend: Modelos TypeScript

- [ ] Buscar usos de `EstadoInforme` y `Rol` en el frontend
- [ ] Agregar `EN_VISTO_BUENO` en `informe.model.ts`
- [ ] Agregar `ADMINISTRATIVO` en `usuario.model.ts`
- [ ] Actualizar cualquier mapeo exhaustivo para incluir el nuevo estado
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): enums TypeScript EN_VISTO_BUENO y ADMINISTRATIVO`

## T11 — Frontend: Feature `visto-bueno`

- [ ] Crear `administrativo.guard.ts`
- [ ] Agregar rutas en `app.routes.ts`
- [ ] Crear `cola-visto-bueno.component.ts/.html`
- [ ] Crear `detalle-visto-bueno.component.ts/.html` con barra de 3 acciones
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): feature visto-bueno — cola, detalle, guard y rutas`

## T12 — Frontend: Admin Toggle VB

- [ ] Identificar el componente de administracion donde agregar la seccion
- [ ] Agregar seccion "Parametros del sistema" con `p-inputSwitch`
- [ ] Implementar carga del estado actual desde el API
- [ ] Implementar modal de confirmacion al desactivar
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): toggle VB en panel de administracion`

## T13 — Frontend: Componentes Compartidos

- [ ] Agregar chip `EN_VISTO_BUENO` en el componente de badge de estado
- [ ] Agregar item "Visto Bueno" en el sidebar con visibilidad condicional para `ADMINISTRATIVO`
- [ ] Verificar que el chip aparece correctamente en todas las vistas donde se muestra el estado del informe
- [ ] Ejecutar: `ng build` — 0 errores TypeScript
- [ ] Commit: `feat(i9): chip EN_VISTO_BUENO y menu lateral ADMINISTRATIVO`

## T14 — Validacion Final y Cierre

- [ ] Ejecutar `mvn test` — 0 fallos
- [ ] Ejecutar `ng build` — 0 errores TypeScript, 0 advertencias criticas
- [ ] Ejecutar smoke test funcional en `local-dev`
- [ ] Actualizar `docs/ARRANQUE.md` — usuario de prueba `administrativo/admin123`
- [ ] Actualizar `docs/GUIA_PRUEBAS_FUNCIONALES.md` — seccion I9
- [ ] Cerrar execution log: marcar tareas completadas, registrar SHA de commits, estado CERRADO
- [ ] Commit final: `docs(i9): cierre — ejecucion log, arranque y guia de pruebas`

---

## Registro de Ejecucion

### 2026-05-20 — T0 apertura

- Se revisaron `README.md`, `docs/CONSTITUTION.md`, `docs/ARCHITECTURE.md`, `docs/TECNOLOGIAS.md`, `docs/specs/`, `docs/plans/` y el execution log vigente I8.
- Se confirmo que I8 esta cerrado y que I9 es el incremento activo aprobado con plan listo para ejecucion.
- Se abre este execution log en `main`, siguiendo la rama definida por la spec/plan I9.
- Commit T0: `6bf04bd` — `docs(i9): abrir execution log I9`.

### 2026-05-20 — T1 Base de Datos en progreso

- Creados `db/06_sgcn_parametros.sql` y `db/07_observaciones_accion.sql` como scripts incrementales idempotentes Oracle 19c.
- `db/06_sgcn_parametros.sql` crea `SGCN_PARAMETROS` si no existe e inserta `VB_ACTIVO=S` solo cuando falta la clave; si ya existe, conserva el valor operativo actual.
- `db/07_observaciones_accion.sql` agrega `SGCN_OBSERVACIONES.ACCION VARCHAR2(20) DEFAULT NULL` solo si la columna no existe.
- Actualizado `db/03_reset_informes_local_dev.sql` para normalizar `VB_ACTIVO=S` durante reset local cuando `SGCN_PARAMETROS` ya existe, sin romper esquemas pre-I9.
- Validacion disponible: `sqlplus -V` retorna SQL*Plus 21.3 instalado.
- Gate T1 completado por ejecucion manual confirmada: ambos scripts fueron ejecutados contra la BD local sin errores.
- Commit T1: `61a8955` — `feat(i9): DDL SGCN_PARAMETROS y columna ACCION en observaciones`.

### 2026-05-20 — T2 Enums Java en progreso

- Inconsistencia documentada: la spec/plan nombran `Rol.java`, pero el codigo vigente usa `RolUsuario.java` como enum canonico de usuario.
- No se encontraron `switch` sobre `EstadoInforme`; los `switch` existentes son sobre eventos de notificacion o items SGSSI.
- Se detecto mapeo exhaustivo de prioridad por estado en `BusquedaAdminService`; se incluye `EN_VISTO_BUENO` con prioridad entre `EN_REVISION` y `ENVIADO`.
- Se agrega `EN_VISTO_BUENO` a `EstadoInforme.java`.
- Se agrega `ADMINISTRATIVO` a `RolUsuario.java`.
- Validacion: `mvn compile -pl sigcon-backend` no aplica en la raiz porque el repo no es reactor Maven. Se ejecuto `mvn compile` desde `sigcon-backend` con resultado `BUILD SUCCESS` (124 source files, 0 errores).
- Commit T2: `34fd9b5` — `feat(i9): agregar EN_VISTO_BUENO y ADMINISTRATIVO a enums`.

## Punto de Retoma

Continuar con T3 — Entidad `SgcnParametro` y Repositorio:

1. Crear `SgcnParametro.java` con anotaciones JPA.
2. Crear `SgcnParametroRepository.java`.
3. Compilar backend antes del commit T3.
