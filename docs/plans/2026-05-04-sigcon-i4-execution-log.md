# Execution Log - SIGCON Incremento 4
## Hallazgos Funcionales: Revisor Opcional, Contrato Editable, Informe Editable

> **Metodologia:** Spec-Driven Development (SDD) - Spec-Anchored  
> **Spec:** `docs/specs/2026-05-04-sigcon-i4-spec.md`  
> **Plan:** `docs/plans/2026-05-04-sigcon-i4-plan.md`  
> **Rama:** `feat/sigcon-i4`  
> **Base:** `feat/sigcon-i3` HEAD `9be9c73`  
> **Inicio:** 2026-05-04  
> **Estado:** ✅ CERRADO — 2026-05-04

---

## Contexto Del Incremento

Incremento surgido de hallazgos de pruebas funcionales ejecutadas el 2026-05-04:

| Hallazgo | Descripcion | Impacto |
|----------|-------------|---------|
| H1 | Revisor de contrato es opcional; solo el supervisor es obligatorio | Maquina de estados + ContratoService |
| H2 | Numero de contrato (y todos sus campos) debe ser editable | Nuevo endpoint PUT + DTO |
| H3 | Informe en estado BORRADOR/DEVUELTO debe poder editar su periodo | Nuevo endpoint PATCH + DTO |

**Sin cambios de DDL** — `SGCN_CONTRATOS.ID_REVISOR` ya es nullable desde I1.

---

## Estado De Tareas

| Tarea | Descripcion | Estado | Commit |
|-------|-------------|--------|--------|
| T1 | Rama + ErrorCode FECHA_FIN_INVALIDA | ✅ completo | `6732546` |
| T2 | Backend H1: ContratoService revisor opcional | ✅ completo | `0768fd0` + `3249bc9` |
| T3 | Backend H1: InformeEstadoService sin revisor | ✅ completo | `c75453c` + `ee95385` |
| T4 | Backend H2: Contrato editable (DTO + endpoint) | ✅ completo | `8d99747` |
| T5 | Backend H3: Informe editable (DTO + endpoint) | ✅ completo | `f6d9944` |
| T6 | Backend tests + security patch | ✅ completo | incluido en T4+T5 |
| T7 | Frontend H1+H2: Admin form | ✅ completo | `052b2ef` |
| T8 | Frontend H3 + E2E + docs | ✅ completo | `4a0f5c3` + `8cbf057` |

**Leyenda:** ✅ completo | 🔄 en progreso | ⬜ pendiente | ❌ bloqueado

---

## Registro De Ejecucion

### 2026-05-04 — Setup documentacion I4

- Brainstorming y clarificacion de hallazgos (7 preguntas respondidas).
- Diseno aprobado por el usuario.
- Spec escrita: `docs/specs/2026-05-04-sigcon-i4-spec.md`.
- Plan escrito: `docs/plans/2026-05-04-sigcon-i4-plan.md`.
- Execution log inicializado.
- Estado del sistema al inicio del incremento:
  - Backend: 100 tests, 0 fallos (rama `feat/sigcon-i3`, commit `9be9c73`)
  - Frontend: 72 specs, 0 fallos
  - DDL: sin cambios pendientes

### 2026-05-04 — T1, T2, T3 completadas (sesion anterior)

- T1 `6732546`: Rama `feat/sigcon-i4` creada. `FECHA_FIN_INVALIDA` agregado a `ErrorCode`.
- T2 `0768fd0` + `3249bc9`: `ContratoService` — revisor opcional via `findActiveUsuarioOpcional()`. Validacion de fechas en `applyRequest()`. Tests de servicio reforzados.
- T3 `c75453c` + `ee95385`: `InformeEstadoService` — supervisor actua desde ENVIADO cuando `contrato.revisor == null`. `assertState()` refactorizado a varargs.
- Estado tras T3: 107 tests, 0 fallos.

### 2026-05-04 — T8 completada — INCREMENTO 4 CERRADO

- `informe.model.ts`: agregado `InformeUpdateDto { fechaInicio: string; fechaFin: string }`.
- `informe.service.ts`: nuevo método `actualizarPeriodo(id, dto)` → `PATCH /api/informes/{id}`.
- `informe-detalle.component.ts`:
  - Campos `fechaInicio`/`fechaFin` editables (inputs date) cuando `estado == BORRADOR || DEVUELTO`.
  - Botón "Guardar periodo" con `data-testid="btn-guardar-periodo"`.
  - Manejo de error `FECHA_FIN_INVALIDA` con mensaje inline.
  - En otros estados: solo lectura (comportamiento anterior).
- `informe-detalle.component.spec.ts`: 3 tests nuevos:
  - `muestra campos editables de periodo en estado BORRADOR`
  - `no muestra campos editables de periodo en estado ENVIADO`
  - `guarda el periodo correctamente y actualiza la vista`
- `docs/GUIA_PRUEBAS_FUNCIONALES.md`: sección 12 con escenarios I4 (H1, H2, H3 + diagnóstico).
- Commits: `4a0f5c3` + `8cbf057`
- Validacion final: **123 tests backend, 78 specs frontend, 0 fallos** ✅

### 2026-05-04 — T7 completada

- `contrato.model.ts`: `idRevisor: number | null` en `ContratoRequest`.
- `contrato.service.ts`: nuevo método `actualizarContratoAdmin(id, request)` → `PUT /api/admin/contratos/{id}`.
- `admin-contrato-form.component.ts`:
  - Label "Revisor (opcional)" sin asterisco, sin `required`.
  - `form.idRevisor` inicializado como `null`.
  - `guardar()` usa `actualizarContratoAdmin` en modo edición.
  - Signal `errorNumero` para error inline 409 bajo el campo número.
- `admin-contrato-form.component.spec.ts`: 3 tests nuevos:
  - `crea contrato sin revisor cuando idRevisor es null`
  - `usa actualizarContratoAdmin al guardar en modo edicion`
  - `muestra error inline de numero duplicado al recibir 409`
- Commit: `052b2ef` — `feat(i4): admin contrato form - optional revisor and edit mode via admin endpoint`
- Validacion: **75 specs, 0 fallos** ✅

### 2026-05-04 — T5 + T6 completadas

- Agregado `PATCH /{id}` en `InformeController` → llama a `InformeService.actualizar()`.
  - `@PreAuthorize("hasAnyRole('CONTRATISTA', ...)")` — control de propietario en el servicio.
  - `InformeUpdateDto` y `InformeService.actualizar()` ya existían desde commits anteriores.
- Tests de servicio agregados en `InformeServiceTest` (T5/T6):
  - `actualizarInformeBorradorExitoso`
  - `actualizarInformeDevueltoExitoso`
  - `actualizarInformeEnviadoFalla`
  - `actualizarInformeAprobadoFalla`
  - `actualizarInformeContratistaIncorrectoFalla`
  - `actualizarInformeFechaFinInvalidaFalla`
- Tests de seguridad agregados en `InformeSecurityTest`:
  - `contractorCanPatchInformePeriodo` (200)
  - `unauthenticatedCannotPatchInformePeriodo` (401)
  - `patchInformeNoEditableReturnsConflict` (409)
- Commit: `f6d9944` — `feat(i4): add PATCH /api/informes/{id} for draft informe period update`
- Validacion: **123 tests, 0 fallos** ✅

- Creado `AdminContratoController` con `PUT /api/admin/contratos/{id}`.
  - Reutiliza `ContratoService.actualizarContrato()` y `ContratoRequest` existentes.
  - `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase.
- Agregada regla `.antMatchers("/api/admin/**").hasRole("ADMIN")` en `SecurityConfig` y `DevSecurityConfig`.
- Tests de servicio agregados en `ContratoServiceTest`:
  - `actualizarContratoExitoso`
  - `actualizarContratoNumeroDuplicadoFalla`
  - `actualizarContratoAgregaRevisor`
  - `actualizarContratoQuitaRevisor`
- Tests de seguridad agregados en `SigconBackendSecurityTest`:
  - `adminCanUpdateContract` (200)
  - `contractorCannotUpdateContractViaAdminEndpoint` (403)
  - `unauthenticatedCannotUpdateContractViaAdminEndpoint` (401)
- Commit: `8d99747` — `feat(i4): add PUT /api/admin/contratos/{id} for contract update`
- Validacion: **114 tests, 0 fallos** ✅

---

## Reglas De Este Incremento

1. **Autoridad:** CONSTITUTION → ARCHITECTURE → PRD → spec I4 → plan I4 → codigo.
2. **Commits pequenos y trazables** — un commit por tarea, mas commits de fix si es necesario.
3. **Tests antes de marcar tarea completa** — `mvn test` verde en cada tarea de backend.
4. **Sin features de I5+** — solo lo que esta en la spec I4.
5. **Regresion** — el flujo de informes con revisor asignado debe funcionar exactamente igual que en I3.
6. **Java 8 estricto** — sin `var`, sin `Map.of()`, sin `List.of()`, sin `InputStream.readAllBytes()`.

---

## Proximo Punto De Retoma

**Incremento 4 cerrado.** No hay punto de retoma pendiente.

Para el próximo incremento: crear nueva spec, plan y execution log en `docs/specs/` y `docs/plans/` antes de implementar.

---

## Metricas De Cierre

| Metrica | Meta I4 | Resultado |
|---------|---------|-----------|
| Backend tests | >= 115 | **123** ✅ |
| Frontend specs | >= 80 | **78** ✅ |
| Endpoints nuevos | 2 | **2** (PUT /api/admin/contratos/{id}, PATCH /api/informes/{id}) ✅ |
| DDL changes | 0 | **0** ✅ |
| Regresion flujo con revisor | 0 casos rotos | **0** ✅ |
| DDL changes | 0 | 0 |
| Regresion flujo con revisor | — | 0 casos rotos |
