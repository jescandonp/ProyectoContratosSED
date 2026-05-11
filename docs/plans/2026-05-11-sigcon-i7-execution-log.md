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
| T4 | Backend documentos requeridos PDF/EML + FACTURA dinamica | COMPLETO | `c97c0de` |
| T5 | Validacion envio por documentos requeridos | COMPLETO | `613bb6e` |
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

### 2026-05-11 — T4 Backend documentos requeridos PDF/EML + FACTURA dinamica

**Revision del modelo existente:**
- `DocumentoCatalogo` / `DocumentoAdicional`: solo guardan referencia textual, no archivos binarios. No cubren el caso de archivo requerido por informe.
- `DocumentStorageService` / `LocalDocumentStorageService`: ya tienen `storeFile()` y `loadFile()` reutilizables.
- No existia entidad para archivo requerido por informe → se creo `DocumentoRequeridoInforme`.

**Archivos creados/modificados:**
- `db/00_setup.sql`: bloque I7 extendido con tabla `SGCN_DOCS_REQUERIDOS` (sequence, tabla, indices, trigger).
- `sigcon-backend/pom.xml`: agregada dependencia `spring-boot-starter-mail` (incluye `javax.mail` para parseo EML y sera usada en T7).
- `domain/entity/DocumentoRequeridoInforme.java`: entidad JPA con campos `claveLogica`, `nombreDisplay`, `nombreArchivo`, `contentType`, `extension`, `storagePath`, `tamanoBytes`, auditoria.
- `domain/repository/DocumentoRequeridoInformeRepository.java`: repositorio Spring Data JPA.
- `application/dto/informe/DocumentoRequeridoDto.java`: DTO de lista con `cargado`, `porIva`.
- `application/dto/informe/EmlPreviewDto.java`: DTO de preview EML con `asunto`, `remitente`, `destinatarios`, `fecha`, `cuerpoTexto`, `previewParcial`.
- `web/exception/ErrorCode.java`: nuevos codigos `DOCUMENTO_REQUERIDO_NO_ENCONTRADO`, `DOCUMENTO_REQUERIDO_FORMATO_INVALIDO`, `DOCUMENTO_REQUERIDO_NO_EDITABLE`, `DOCUMENTO_REQUERIDO_FALTANTE`.
- `application/service/DocumentoRequeridoInformeService.java`: servicio principal con:
  - `listar()`: lista requeridos + FACTURA dinamica si `responsableIva=true`.
  - `cargarArchivo()`: upload PDF/EML con validacion de extension, ownership y estado.
  - `descargarArchivo()`: descarga con validacion de acceso.
  - `previewEml()`: parseo EML con `javax.mail` (asunto, remitente, destinatarios, fecha, cuerpo texto, `previewParcial`).
  - `eliminarArchivo()`: soft-delete con validacion de estado.
  - `assertDocumentosRequeridosCompletos()`: hook para T5 — verifica FACTURA cargada si `responsableIva=true`.
- `web/controller/DocumentoRequeridoInformeController.java`: 5 endpoints bajo `/api/informes/{id}/documentos-requeridos`.
- `test/DocumentoRequeridoInformeServiceTest.java`: 15 tests unitarios con Mockito.
- `test/SigconBackendSecurityTest.java`: agregado `@MockBean DocumentoRequeridoInformeRepository` para contexto Spring.
- `test/InformeSecurityTest.java`: agregado `@MockBean DocumentoRequeridoInformeService` para contexto Spring.

**Decisiones de diseno:**
- `FACTURA` es requerido dinamico: no depende de catalogo manual. Si `informe.contrato.contratista.responsableIva=true`, se expone como pendiente en la lista aunque no exista registro en BD.
- Separacion clara de `/documentos-requeridos` vs `/documentos-adicionales` (documentos adicionales libres con referencia textual).
- Preview EML usa `javax.mail` (disponible via `spring-boot-starter-mail`). Si el EML es complejo, retorna `previewParcial=true` y conserva descarga del original.
- `assertDocumentosRequeridosCompletos()` es el hook que T5 invocara desde `InformeEstadoService.enviar()`.

**Validaciones ejecutadas:**
- `mvn test -Dtest=DocumentoRequeridoInformeServiceTest` — 15 tests, 0 fallos.
- `mvn test` (suite completa) — 152 tests, 0 fallos, 0 errores, BUILD SUCCESS.

### 2026-05-11 — T5 Backend validacion de envio por documentos requeridos

**Cambios:**
- `InformeEstadoService`: inyectado `DocumentoRequeridoInformeService` via constructor (10° parametro).
- `InformeEstadoService.enviar()`: llamada a `documentoRequeridoInformeService.assertDocumentosRequeridosCompletos(informe)` despues de `assertDocumentosAdicionalesCompletos(informe)`.
- `InformeEstadoServiceTest`: mock de `DocumentoRequeridoInformeService` agregado; constructor actualizado; 3 tests nuevos:
  - `enviarConResponsableIvaYFacturaCargadaPermiteEnvio` — verifica que el hook se invoca y el envio procede.
  - `enviarConResponsableIvaYFacturaFaltanteBloquea` — verifica `DOCUMENTO_REQUERIDO_FALTANTE` y que el estado no cambia.
  - `enviarSinResponsableIvaNoExigeFactura` — verifica que el hook se invoca pero no lanza excepcion.
- `InformeEstadoServiceI3Test` y `InformeEstadoServiceSinRevisorTest`: constructores actualizados con el nuevo parametro.

**Validaciones ejecutadas:**
- `mvn test -Dtest=InformeEstadoServiceTest,InformeEstadoServiceI3Test,InformeEstadoServiceSinRevisorTest` — 28 tests, 0 fallos.
- `mvn test` (suite completa) — 155 tests, 0 fallos, 0 errores, BUILD SUCCESS.

**Commit:** `613bb6e feat(i7): require invoice document for IVA contractors (T5)`

---

---

## Validaciones Ejecutadas

- `mvn test -Dtest=UsuarioServiceTest` — 2 tests, 0 fallos.
- `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build` desde `sigcon-angular` — exitoso.
- `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include=src/app/features/admin/usuarios/admin-usuarios.component.spec.ts` desde `sigcon-angular`, fuera del sandbox — 4 specs, 0 fallos.

---

## Proximo Punto de Retoma

Continuar con **T6 Frontend Documentos Requeridos**:

1. Crear modelo Angular `DocumentoRequerido` y `EmlPreview` en `core/models/`.
2. Crear servicio `DocumentoRequeridoService` con los 5 endpoints.
3. Agregar seccion "Documentos Requeridos" en `InformeDetalleComponent` / `InformeFormComponent`.
4. En `BORRADOR`/`DEVUELTO`: cargar/reemplazar/eliminar + visualizar/descargar.
5. En otros estados: solo visualizar/descargar.
6. Mostrar `FACTURA` como requerida por IVA con indicador visual.
7. Validar extension PDF/EML antes de enviar al backend.
8. Preview PDF (visor embebido o nueva pestaña) y EML (mostrar metadatos + cuerpo texto).
9. Tests Angular focalizados.

---

## Handoff Para Siguiente Herramienta

Estado listo para retomar:

- Rama activa esperada: `feat/sigcon-i7`.
- HEAD documentado antes del handoff: `613bb6e`.
- Tareas cerradas: T0, T1, T2, T3, T4, T5.
- Siguiente tarea: T6.

Contexto tecnico util para T6:

- Endpoints backend disponibles bajo `/api/informes/{id}/documentos-requeridos`.
- `GET /` — lista con `cargado`, `porIva`, `claveLogica`, `nombreDisplay`.
- `POST /{claveLogica}/archivo` — upload multipart, solo CONTRATISTA, solo BORRADOR/DEVUELTO.
- `GET /{documentoId}/archivo` — descarga, cualquier usuario con acceso.
- `GET /{documentoId}/preview` — preview EML, retorna `EmlPreviewDto`.
- `DELETE /{documentoId}/archivo` — soft-delete, solo CONTRATISTA, solo BORRADOR/DEVUELTO.
- El modelo `DocumentoRequeridoDto` tiene: `id`, `claveLogica`, `nombreDisplay`, `cargado`, `nombreArchivo`, `contentType`, `extension`, `tamanoBytes`, `porIva`.
- El modelo `EmlPreviewDto` tiene: `asunto`, `remitente`, `destinatarios`, `fecha`, `cuerpoTexto`, `previewParcial`.

Archivos no versionados presentes y no relacionados con T5:

- `.agents/`
- `.claude/`
- `.kiro/`
- `Notas_ProyectoContratos/`
- `skills-lock.json`

No limpiar ni revertir esos archivos salvo instruccion explicita.
