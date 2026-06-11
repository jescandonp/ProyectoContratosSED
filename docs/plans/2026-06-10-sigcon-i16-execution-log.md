# SIGCON I16 — Execution Log

**Fecha:** 2026-06-10
**Spec:** `docs/specs/2026-06-10-sigcon-i16-spec.md`
**Plan:** `docs/plans/2026-06-10-sigcon-i16-plan.md`
**Rama:** `main`

## Resumen

I16 implementa dos funcionalidades sobre el módulo de contratos:

1. **F1 — Bloqueo individual de carga de informes:** El ADMIN puede bloquear/desbloquear la creación de nuevos informes para un contrato específico. Cuando bloqueado, el botón "Nuevo Informe" desaparece para el contratista y el endpoint `crearInforme` lanza HTTP 403.
2. **F2 — Campo Plazo libre en contrato:** El ADMIN puede ingresar un texto libre de "Plazo" en el formulario de crear/editar contrato. Este texto reemplaza el texto estándar autogenerado en la Sección 1 del PDF formato 11-IF-023 V1. Si se deja vacío, el sistema usa el fallback calculado con fechas.

## Commits

| Commit | Descripción |
|--------|-------------|
| `c4856fd` | `docs(i16): spec técnica — bloqueo carga informes + campo plazo contrato` |
| `acf5d51` | `docs(i16): plan de implementación — 11 tasks TDD, backend + Angular` |
| `72527b5` | `feat(i16): DDL — columnas BLOQUEADO_CARGA_INFORME y PLAZO en SGCN_CONTRATOS` |
| `1ff3152` | `feat(i16): dominio — campos bloqueadoCargaInforme/plazo en Contrato, BloqueoInformeRequest, ErrorCode` |
| `57f370e` | `fix(i16): BloqueoInformeRequest — agregar constructor no-arg para deserialización Jackson` |
| `90d0fb6` | `feat(i16): ContratoMapper/Service — mapear plazo, actualizarBloqueoInforme()` |
| `de4259f` | `feat(i16): InformeService — bloquear crearInforme si contrato.bloqueadoCargaInforme=true` |
| `0046708` | `feat(i16): AdminContratoController — PATCH /{id}/bloqueo-informe` |
| `f017830` | `feat(i16): InformePdfTemplateService — usar plazo libre del contrato en Seccion1, fallback a texto calculado` |
| `1082908` | `feat(i16): Angular models/service — bloqueadoCargaInforme, plazo, actualizarBloqueoInforme()` |
| `0ea34b9` | `feat(i16): contrato-detalle — ocultar botón Nuevo Informe si bloqueado, mostrar campo Plazo` |
| `e57b44b` | `feat(i16): admin-contrato-form — campo Plazo en formulario crear/editar contrato` |
| `67c4592` | `feat(i16): toggle bloqueo en admin-contratos — ContratoResumenDto+mapper+Angular model+UI` |
| `1ba522a` | `fix(i16): ContratoResumenDto/DetalleDto — eliminar shadowing de bloqueadoCargaInforme, heredar desde Resumen` |

## Tareas

| Tarea | Estado | Evidencia |
|-------|--------|-----------|
| T1 — DDL: columnas BLOQUEADO_CARGA_INFORME y PLAZO | Cerrada | `V16__bloqueo_informe_y_plazo_contrato.sql` |
| T2 — Backend dominio: Contrato, DTOs, ErrorCode, BloqueoInformeRequest | Cerrada | `Contrato.java`, `ContratoDetalleDto`, `ContratoRequest`, `BloqueoInformeRequest`, `ErrorCode.INFORME_CARGA_BLOQUEADA` |
| T3 — ContratoMapper + actualizarBloqueoInforme en ContratoService | Cerrada | `ContratoMapper.fillResumen`, `ContratoService.actualizarBloqueoInforme()`, 2 tests verdes |
| T4 — Validación bloqueo en InformeService.crearInforme() | Cerrada | `InformeService` lanza 403 FORBIDDEN, test `contractorCannotCreateInformeOnBlockedContract` |
| T5 — Endpoint PATCH bloqueo en AdminContratoController | Cerrada | `PATCH /api/admin/contratos/{id}/bloqueo-informe` |
| T6 — PDF: campo plazo con fallback en InformePdfTemplateService | Cerrada | `appendSeccion1()` usa `c.getPlazo()` o fallback de fechas, 2 tests verdes |
| T7 — Angular: modelos y servicio | Cerrada | `ContratoResumen.bloqueadoCargaInforme`, `ContratoDetalle.plazo`, `actualizarBloqueoInforme()` |
| T8 — Angular: contrato-detalle (contratista) | Cerrada | Botón "Nuevo Informe" oculto si `bloqueadoCargaInforme`, campo Plazo visible si existe |
| T9 — Angular: formulario admin crear/editar contrato | Cerrada | Textarea "Plazo" con hint en sección "Datos Complementarios", precarga en edición |
| T10 — Angular: panel admin toggle bloqueo | Cerrada | Botón "Bloquear/Desbloquear" en lista admin, actualización optimista en tabla |
| T11 — Verificación final, execution log y README | En progreso | `mvn test`: 249/249 ✅, `ng build`: sin errores ✅ |

## Notas técnicas

- **Field shadowing corregido:** Al agregar `bloqueadoCargaInforme` a `ContratoResumenDto` (para la lista paginada), se detectó que `ContratoDetalleDto extends ContratoResumenDto` tenía su propio campo homónimo con tipo `Boolean` distinto al `boolean` del padre. Se resolvió moviendo el campo únicamente al padre con tipo `Boolean` y el hijo hereda el getter/setter.
- **Jackson no-arg constructor:** `BloqueoInformeRequest` requiere constructor sin argumentos explícito para deserialización correcta.
- **Mockito strict mode:** Los tests de `actualizarBloqueoInforme` no necesitan stub de `documentoCatalogoRepository` porque el servicio pasa `Collections.emptyList()` directamente.
