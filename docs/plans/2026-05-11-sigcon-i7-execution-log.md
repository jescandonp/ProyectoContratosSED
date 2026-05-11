# Execution Log — SIGCON Incremento 7
## Usuario IVA, Documentos Requeridos, Email de Aprobacion y Busqueda Administrativa

> **Metodologia:** Spec-Driven Development (SDD) — Spec-Anchored
> **Spec:** `docs/specs/2026-05-11-sigcon-i7-spec.md`
> **Plan:** `docs/plans/2026-05-11-sigcon-i7-plan.md`
> **Rama:** `feat/sigcon-i7`
> **Base:** `feat/sigcon-i6` HEAD `3c8accf`
> **Inicio:** 2026-05-11
> **Estado:** EN PREPARACION

---

## Contexto del Incremento

I7 se abre como incremento formal posterior a I6 a partir de hallazgos de pruebas funcionales:

1. Faltan mensajes de confirmacion al crear/editar usuario.
2. Se requiere identificar contratistas responsables de IVA y exigir FACTURA por cada informe cuando aplique.
3. La seccion Documentos Requeridos debe permitir adjuntar, visualizar y descargar los documentos requeridos del informe, exclusivamente PDF y `.eml`.
4. Al aprobar informe se debe notificar por email al contratista y a un correo administrador configurable por ambiente.
5. El administrador requiere busqueda global por contratista, contrato e informe, con rango de fechas aplicado al periodo del informe.

---

## Inconsistencias Documentales Detectadas Antes de I7

- `README.md` todavia declara I5 como ultimo incremento cerrado al 2026-05-06, aunque I6 ya esta cerrado y `origin/main` esta en estado I6 posterior.
- `docs/ARRANQUE.md` documenta I6 cerrado, pero su tabla GitHub puede contener SHAs anteriores al estado base actual `3c8accf`.
- Se resolveran durante T9 si I7 llega a cierre, o antes si afectan handoff.

---

## Estado del Sistema al Inicio del Incremento

- Rama creada: `feat/sigcon-i7`.
- Base: `feat/sigcon-i6` HEAD `3c8accf`.
- T0 de estabilizacion ya incluido en base:
  - `db/00_setup.sql`: `SGCN_ACTIVIDADES.PORCENTAJE DEFAULT 0 NOT NULL`.
  - `ActividadInformeService.crear()`: asigna `BigDecimal.ZERO`.
  - `ActividadInformeServiceTest`: valida default interno.
  - `db/03_reset_informes_local_dev.sql`: reset local de informes para pruebas funcionales.

---

## Matriz de Tareas

| Tarea | Descripcion | Estado | Commit |
|-------|-------------|--------|--------|
| T0 | Estabilizacion heredada I6/I7 | COMPLETO EN BASE | `3c8accf` |
| T1 | Spec, plan y execution log I7 | COMPLETO | `31a5381` |
| T2 | Backend usuario responsable IVA | COMPLETO | `b4f717f` |
| T3 | Frontend usuario IVA + confirmaciones | COMPLETO | `2f2cb82` |
| T4 | Backend documentos requeridos PDF/EML + FACTURA dinamica | PENDIENTE | pendiente |
| T5 | Validacion envio por documentos requeridos | PENDIENTE | pendiente |
| T6 | Frontend Documentos Requeridos | PENDIENTE | pendiente |
| T7 | Email aprobacion contratista + admin configurable | PENDIENTE | pendiente |
| T8 | Busqueda administrativa global | PENDIENTE | pendiente |
| T9 | Validacion, docs y cierre | PENDIENTE | pendiente |

---

## Registro de Ejecucion

### 2026-05-11 — Apertura I7

- Se acordo tratar los hallazgos como **Incremento 7 formal**.
- Se aprobo incluir T0 como estabilizacion heredada de pruebas funcionales.
- Se definio alcance I7:
  - confirmaciones usuario
  - responsable IVA
  - FACTURA por informe para responsables IVA
  - documentos requeridos PDF/EML con preview/descarga
  - email de aprobacion a contratista y admin configurable
  - busqueda administrativa global con rango por periodo de informe

### 2026-05-11 — T2 Backend usuario responsable IVA

- Se aplico TDD para `UsuarioService`.
- RED: `mvn test -Dtest=UsuarioServiceTest` fallo por ausencia de `responsableIva` en `Usuario`, `UsuarioRequest` y `UsuarioDto`.
- GREEN:
  - `db/00_setup.sql`: bloque I7 con `SGCN_USUARIOS.RESPONSABLE_IVA NUMBER(1) DEFAULT 0 NOT NULL`.
  - `Usuario`: nuevo campo `responsableIva` con default `false`.
  - `UsuarioRequest`: nuevo campo opcional `responsableIva`.
  - `UsuarioDto`: expone `responsableIva`.
  - `UsuarioMapper`: mapea `responsableIva`.
  - `UsuarioService.applyRequest()`: persiste `Boolean.TRUE.equals(request.getResponsableIva())`, conservando default `false` si el request omite el valor.
  - `UsuarioServiceTest`: valida default `false` y persistencia `true`.

### 2026-05-11 — T3 Frontend usuario IVA + confirmaciones

- Se aplico TDD sobre `AdminUsuariosComponent`.
- RED: el spec focalizado fallo inicialmente por ausencia de `responsableIva` en el modelo Angular y por ausencia de `mensajeExito`.
- GREEN:
  - `usuario.model.ts`: `Usuario.responsableIva` y `UsuarioRequest.responsableIva`.
  - `AdminUsuariosComponent`: default `responsableIva=false` al crear usuario.
  - `AdminUsuariosComponent`: checkbox "Responsable de IVA" en formulario.
  - `AdminUsuariosComponent`: mensaje posterior a guardado exitoso:
    - "Usuario creado correctamente."
    - "Usuario actualizado correctamente."
  - Se conservaron mensajes de error existentes para fallo de backend.
- Compatibilidad de compilacion:
  - Modelos `Usuario`, `ContratoDetalle` e `InformeDetalle` aceptan campos I6 opcionales para tolerar mocks legacy y respuestas parciales.
  - Se retiraron referencias obsoletas a `porcentaje` por actividad en specs Angular que quedaron desalineados con I6.
  - `InformeDetalleComponent` tolera `aportesSgssi` ausente como lista vacia.

---

## Validaciones Ejecutadas

- `mvn test -Dtest=UsuarioServiceTest` — 2 tests, 0 fallos.
- `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build` desde `sigcon-angular` — exitoso.
- `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include=src/app/features/admin/usuarios/admin-usuarios.component.spec.ts` desde `sigcon-angular`, fuera del sandbox — 4 specs, 0 fallos.

---

## Proximo Punto de Retoma

Continuar con **T4 Backend documentos requeridos PDF/EML + FACTURA dinamica**:

1. Revisar modelo actual de catalogo/documentos y almacenamiento.
2. Definir entidad/servicio minimo para archivo requerido por informe si no existe.
3. Implementar lista de documentos requeridos por informe con `FACTURA` dinamica para responsables IVA.
4. Implementar upload/download/preview PDF/EML con pruebas backend.

---

## Handoff Para Siguiente Herramienta

Estado listo para retomar:

- Rama activa esperada: `feat/sigcon-i7`.
- HEAD documentado antes del handoff: `3be818d`.
- Tareas cerradas: T0, T1, T2, T3.
- Siguiente tarea: T4.
- No iniciar T5/T6 hasta que T4 deje estable el contrato backend de documentos requeridos.

Contexto tecnico util para T4:

- Ya existen `DocumentoCatalogo`, `DocumentoAdicional`, `DocumentoAdicionalInformeService`, `DocumentoAdicionalInformeController`.
- Los documentos adicionales actuales solo guardan `referencia` textual; no cubren archivo requerido por informe ni preview PDF/EML.
- Ya existe `DocumentStorageService` con `storeFile(...)` y `loadFile(...)`.
- El nuevo modelo debe mantener separada la seccion **Documentos Requeridos** de soportes de actividad y documentos adicionales libres.
- `FACTURA` debe resolverse como requerido dinamico si `informe.contrato.contratista.responsableIva == true`.
- Estados editables para carga/eliminacion: `BORRADOR` y `DEVUELTO`.
- Estados solo lectura: `ENVIADO`, `EN_REVISION`, `APROBADO`.
- Formatos permitidos: PDF y `.eml`.

Archivos no versionados presentes y no relacionados con T2/T3:

- `.agents/`
- `.claude/`
- `.kiro/`
- `Notas_ProyectoContratos/`
- `skills-lock.json`

No limpiar ni revertir esos archivos salvo instruccion explicita.
