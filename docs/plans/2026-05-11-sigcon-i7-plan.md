# Plan de Implementacion — SIGCON Incremento 7
## Usuario IVA, Documentos Requeridos, Email de Aprobacion y Busqueda Administrativa

> **Metodologia:** Spec-Driven Development (SDD) — Spec-Anchored
> **Version:** 1.0 — **Fecha:** 2026-05-11
> **Spec de referencia:** `docs/specs/2026-05-11-sigcon-i7-spec.md`
> **Rama:** `feat/sigcon-i7` (base: `feat/sigcon-i6`)
> **Estado:** Listo para ejecucion

---

## Resumen Ejecutivo

Incremento de **9 tareas**. El orden prioriza estabilizacion, modelo de usuario, documentos requeridos/factura, email y busqueda administrativa.

| Tarea | Scope | Descripcion |
|-------|-------|-------------|
| T0 | Estabilizacion | Registrar fix heredado de pruebas: `PORCENTAJE DEFAULT 0`, `BigDecimal.ZERO` al crear actividad y reset local de informes |
| T1 | Documentacion base | Crear spec, plan y execution log I7; actualizar estado documental si aplica |
| T2 | Backend — Usuario IVA | DDL + dominio/DTO/service para `responsableIva` |
| T3 | Frontend — Usuario | Campo Responsable IVA + confirmaciones crear/editar usuario |
| T4 | Backend — Documentos requeridos | Entidad/servicio/endpoints para carga, descarga y preview PDF/EML; FACTURA dinamica |
| T5 | Backend — Validacion envio | Bloquear envio de informe si faltan documentos requeridos, incluida FACTURA por IVA |
| T6 | Frontend — Documentos requeridos | Seccion Documentos Requeridos en informe con carga/preview/descarga |
| T7 | Backend — Email aprobacion | Servicio email configurable y disparo al aprobar informe |
| T8 | Backend/Frontend — Busqueda admin | Endpoint y pantalla global por texto + rango de periodo de informe |
| T9 | Validacion y cierre | Tests, guia funcional, execution log, commits y punto de retoma |

---

## T0 — Estabilizacion heredada de pruebas funcionales

**Estado al iniciar I7:** ya incorporado en la base de rama `feat/sigcon-i7` desde commit `3c8accf fix: harden SIGCON I6 informe activity reset`.

**Alcance:**

- `db/00_setup.sql`: `SGCN_ACTIVIDADES.PORCENTAJE NUMBER(5,2) DEFAULT 0 NOT NULL`.
- `ActividadInformeService.crear()`: asigna `BigDecimal.ZERO` internamente al crear actividad.
- `ActividadInformeServiceTest`: valida default interno.
- `db/03_reset_informes_local_dev.sql`: script local para resetear informes y dependencias en pruebas.

**Acciones I7:**

- No reimplementar salvo regresion.
- Referenciar como antecedente en execution log I7.
- Mantener fuera de produccion el script `03_reset_informes_local_dev.sql`.

**Validacion:** confirmar que el commit base existe y que backend compila en la primera ronda de pruebas.

---

## T1 — Documentacion base I7

**Archivos:**

- `docs/specs/2026-05-11-sigcon-i7-spec.md`
- `docs/plans/2026-05-11-sigcon-i7-plan.md`
- `docs/plans/2026-05-11-sigcon-i7-execution-log.md`

**Acciones:**

1. Crear rama `feat/sigcon-i7` desde `feat/sigcon-i6`.
2. Crear spec tecnica con alcance aprobado.
3. Crear plan de implementacion.
4. Crear execution log inicial.
5. Registrar inconsistencias documentales detectadas:
   - `README.md` aun menciona I5 como ultimo cerrado.
   - `docs/ARRANQUE.md` puede tener SHAs de I6 previos a `3c8accf`/`feat/sigcon-i7`.

**Validacion:** `git status --short --branch` muestra rama `feat/sigcon-i7`; documentos I7 existen.

**Commit sugerido:** `docs(i7): formalize SIGCON increment 7 scope and plan`

---

## T2 — Backend: Usuario responsable IVA

**Archivos previstos:**

- `db/00_setup.sql`
- `sigcon-backend/src/main/java/.../domain/entity/Usuario.java`
- DTOs/request/response de usuario
- `UsuarioService`
- tests de usuario

**Acciones:**

1. Agregar DDL `RESPONSABLE_IVA NUMBER(1) DEFAULT 0 NOT NULL` en `SGCN_USUARIOS`.
2. Mapear `responsableIva` como boolean/Boolean compatible Java 8.
3. Extender request/response de administracion de usuarios.
4. Default backend: `false` si el request omite el campo.
5. Tests:
   - creacion default false
   - actualizacion a true
   - no rompe perfiles existentes

**Validacion:** tests backend focalizados de usuario.

**Commit sugerido:** `feat(i7): add responsable IVA flag to users`

---

## T3 — Frontend: Usuario IVA + confirmaciones

**Archivos previstos:**

- `sigcon-angular/src/app/core/models/usuario.model.ts`
- `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.ts`
- `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.html`
- spec del componente

**Acciones:**

1. Agregar campo "Responsable de IVA" con default `No`.
2. Incluir el campo en create/update request.
3. Corregir feedback visual:
   - "Usuario creado correctamente."
   - "Usuario actualizado correctamente."
   - error claro si falla backend.
4. Tests de create/update y default visual.

**Validacion:** specs Angular focalizados de admin usuarios.

**Commit sugerido:** `feat(i7): surface user save confirmations and IVA flag`

---

## T4 — Backend: Documentos requeridos PDF/EML + FACTURA dinamica

**Archivos previstos:**

- entidad/repository si el modelo actual no cubre archivo por documento requerido
- service de documentos requeridos
- controller de documentos requeridos
- DTOs de lista, archivo y preview EML
- configuracion de almacenamiento existente

**Acciones:**

1. Revisar modelo actual de catalogo/documentos.
2. Implementar lista de requeridos por informe.
3. Resolver `FACTURA` dinamica cuando `informe.contrato.contratista.responsableIva = true`.
4. Implementar upload/reemplazo PDF/EML.
5. Implementar download.
6. Implementar preview:
   - PDF: stream visualizable.
   - EML: DTO con asunto, remitente, destinatarios, fecha, cuerpo texto, `previewParcial`.
7. Validar extension/content type.
8. Validar ownership y estados editables.

**Validacion:** tests backend de service/controller/seguridad.

**Commit sugerido:** `feat(i7): add required document upload and preview`

---

## T5 — Backend: Validacion de envio por documentos requeridos

**Archivos previstos:**

- servicio de estados de informe (`InformeEstadoService` o equivalente)
- tests de flujo de envio

**Acciones:**

1. Antes de `BORRADOR/DEVUELTO -> ENVIADO`, verificar documentos requeridos.
2. Si falta FACTURA por IVA, responder mensaje claro.
3. Si faltan otros requeridos, listar nombres faltantes.
4. Garantizar que no se modifica la maquina de estados.

**Validacion:** tests de envio con responsable IVA true/false.

**Commit sugerido:** `feat(i7): require invoice document for IVA contractors`

---

## T6 — Frontend: Seccion Documentos Requeridos

**Archivos previstos:**

- modelos/servicio Angular de documentos requeridos
- `InformeFormComponent`
- `InformeDetalleComponent`
- templates/specs

**Acciones:**

1. Mostrar seccion "Documentos Requeridos".
2. En `BORRADOR`/`DEVUELTO`: cargar/reemplazar/eliminar + visualizar/descargar.
3. En otros estados: visualizar/descargar.
4. Mostrar `FACTURA` como requerida por IVA.
5. Bloquear seleccion distinta de PDF/EML en frontend.
6. Preview PDF y EML.
7. Tests de UI.

**Validacion:** specs Angular focalizados de informe.

**Commit sugerido:** `feat(i7): manage required documents from informe UI`

---

## T7 — Backend: Email al aprobar informe

**Archivos previstos:**

- `application.yml` / `application-local-dev.yml` / properties equivalentes
- `EmailNotificationService`
- extension de flujo aprobar informe
- tests

**Acciones:**

1. Agregar propiedades:
   - `sigcon.notifications.admin-email`
   - `sigcon.notifications.email-enabled`
2. Implementar servicio con comportamiento local tolerante.
3. Al aprobar informe, enviar a contratista y admin configurable.
4. Fallo de email no revierte aprobacion.
5. Tests de invocacion y tolerancia a fallo.

**Validacion:** tests backend de aprobacion/email.

**Commit sugerido:** `feat(i7): notify contractor and admin on informe approval`

---

## T8 — Busqueda administrativa global

**Backend:**

- endpoint `GET /api/admin/busqueda?q=&fechaInicio=&fechaFin=`
- service/repository queries
- DTO agrupado
- seguridad ADMIN

**Frontend:**

- ruta `/admin/busqueda`
- input texto libre
- rango de fechas sobre periodo del informe
- resultados agrupados por Contratistas, Contratos, Informes
- navegacion a detalle

**Validacion:**

- backend: busqueda por cada grupo, filtro rango, seguridad
- frontend: request correcto y render de grupos

**Commit sugerido:** `feat(i7): add admin global search`

---

## T9 — Validacion, documentacion y cierre

**Acciones:**

1. Ejecutar pruebas focalizadas backend.
2. Ejecutar pruebas focalizadas frontend.
3. Actualizar `docs/GUIA_PRUEBAS_FUNCIONALES.md` con escenarios I7:
   - usuario IVA
   - factura por informe
   - documentos requeridos PDF/EML
   - email aprobacion
   - busqueda global
4. Actualizar `README.md` y `docs/ARRANQUE.md` con I7 si se cierra.
5. Actualizar execution log con pruebas, commits, resultados y punto de retoma.

**Validacion final minima:**

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=UsuarioServiceTest,InformeEstadoServiceTest,*Documento*Test,*Busqueda*Test"
```

Frontend:

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Si las pruebas Angular fallan por `spawn EPERM`, reintentar fuera del sandbox antes de cambiar codigo.

**Commit sugerido:** `docs(i7): close increment 7 execution log`

---

## Orden de Ejecucion

```text
T0 -> T1
T2 -> T3
T4 -> T5 -> T6
T7
T8
T9
```

T7 y T8 pueden ejecutarse despues de T2 sin depender de T4, pero se recomienda cerrar documentos/factura antes de busqueda para evitar mezclar validaciones funcionales.

---

## Riesgos y Controles

| Riesgo | Control |
|--------|---------|
| FACTURA dinamica se duplica con catalogo existente | Resolver por clave logica `FACTURA`; si existe catalogo, reutilizarlo; si no, exponer virtual |
| `.eml` complejo no se previsualiza completo | Preview parcial + descarga original obligatoria |
| Email falla en ambiente local | Config `email-enabled=false` y log sin rollback |
| Busqueda amplia se vuelve lenta | Limitar resultados y paginar si el patron existente lo exige |
| Documentos requeridos se mezclan con soportes | Servicios/rutas separadas y copy UI explicito |

