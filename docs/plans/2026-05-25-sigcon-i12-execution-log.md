# SIGCON I12 — Execution Log

**Incremento:** I12 — Doble perfil, porcentaje editable, bloqueo de carga y contratos OPS/PRO
**Inicio:** 2026-05-25
**Estado:** CERRADO
**Spec:** `docs/specs/2026-05-25-sigcon-i12-spec.md`
**Plan:** `docs/plans/2026-05-25-sigcon-i12-plan.md`

---

## Resumen de tareas

| Task | Descripcion | Estado |
|------|-------------|--------|
| T1 | R4 backend: `TipoContrato.PRO` y encabezado PDF diferencial | Completado |
| T2 | R4 frontend: selector OPS/PRO en administracion de contratos | Completado |
| T3 | R2 backend: PATCH de porcentaje de ejecucion para revision/VB | Completado |
| T4 | R2 seguridad: acceso PDF para ADMIN/ADMINISTRATIVO | Completado |
| T5 | R2 frontend: edicion de porcentaje desde detalle de informe | Completado |
| T6 | R3 dominio/SQL: parametro `CARGA_INFORMES_ACTIVA` y evento masivo | Completado |
| T7 | R3 backend: endpoint admin de carga y bloqueo 423 al crear informe | Completado |
| T8 | R3 notificaciones/email: aviso masivo al desactivar carga | Completado |
| T9 | R3 frontend: toggle admin y mensaje 423 en nuevo informe | Completado |
| T10 | R1 backend: `Usuario.esAdmin`, migracion y reset seed | Completado |
| T11 | R1 seguridad: autorizacion admin para contratista dual | Completado |
| T12 | R1 frontend: contexto de rol activo y guards | Completado |
| T13 | R1 shell: selector de rol y menu lateral por modo activo | Completado |
| T14 | R1 usuarios admin: checkbox e indicador de contratista admin | Completado |
| T15 | Verificacion final y cierre documental | Completado |

---

## Commits del incremento

| Commit | Alcance |
|--------|---------|
| `973e0fc` | R4 PDF: tipo PRO y encabezado diferencial |
| `977265d` | R4 Angular: selector PRO en contratos admin |
| `67d0f33` | R2 backend: porcentaje editable por revisor/admin |
| `c6073dd` | R2 seguridad: PDF para rol VB |
| `e41cd5c` | R2 Angular: edicion de porcentaje en detalle |
| `f352ae4` | R3 backend: parametro carga, bloqueo y notificaciones |
| `1e9d77c` | R3 Angular: toggle carga y warning 423 |
| `d45113d` | R1 backend: contratista con autoridad admin |
| `1198e39` | R1 Angular: contexto dual y selector de rol |
| `443187c` | Ajuste tests backend para suite completa |
| `fe85a45` | Ajuste specs Angular de integracion |

---

## Verificacion

| Fecha/hora | Comando | Resultado |
|------------|---------|-----------|
| 2026-05-25 09:47 America/Bogota | `mvn test "-Dtest=UsuarioServiceTest,DevSecurityConfigTest,SigconBackendSecurityTest"` | BUILD SUCCESS — 24 tests, 0 failures, 0 errors |
| 2026-05-25 09:53 America/Bogota | `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/shared/components/sidebar/sidebar.component.spec.ts --include src/app/features/admin/usuarios/admin-usuarios.component.spec.ts` | SUCCESS — 10 specs |
| 2026-05-25 09:57 America/Bogota | `mvn test` | BUILD SUCCESS — 241 tests, 0 failures, 0 errors |
| 2026-05-25 09:58 America/Bogota | `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false` | SUCCESS — 166 specs |
| 2026-05-25 09:50 America/Bogota | `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build` | Build Angular exitoso |

---

## Notas de cierre

- No se ejecuto verificacion manual con navegador contra backend local y Oracle; el cierre se basa en suites automatizadas backend/frontend y build Angular.
- `db/10_i12_usuario_es_admin.sql` es la migracion incremental para ambientes existentes; `db/00_setup.sql` y `db/08_reset_datos_prueba.sql` quedaron alineados para instalaciones limpias y datos local-dev.
- El usuario local-dev dual es `aecheverry@educacionbogota.gov.co / contratista123`.
- Durante la primera corrida completa de Angular fallaron specs desactualizados de rutas y dashboard; se actualizaron y la segunda corrida completa quedo verde.
