# Plan de Implementacion — SIGCON Incremento 7
## Usuario IVA, Documentos Requeridos, Email de Aprobacion y Busqueda Administrativa

> **Metodologia:** Spec-Driven Development (SDD) — Spec-Anchored
> **Version:** 1.6 — **Fecha:** 2026-05-14
> **Spec de referencia:** `docs/specs/2026-05-11-sigcon-i7-spec.md`
> **Rama:** `feat/sigcon-i7` (base: `feat/sigcon-i6`)
> **Estado:** Listo para ejecucion

---

## Resumen Ejecutivo

Incremento de **15 tareas**. El orden prioriza estabilizacion, modelo de usuario, documentos requeridos/factura, email, busqueda administrativa y ajustes post-pruebas con validacion.

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
| T10 | Correccion funcional post-revision | Hallazgos 12/05/2026: borrador preserva relaciones, una sola seccion Documentos Requeridos, permisos por rol, soportes como Abrir y acciones supervisor |
| T11 | Mejora funcional post-pruebas | Busqueda global con filtros combinados/paginacion y correccion integral de informes `DEVUELTO` |
| T12 | Usabilidad busqueda global | Boton `Limpiar` para restablecer filtros, pagina, errores y resultados sin ejecutar nueva busqueda |
| T13 | Acceso local-dev IVA | Agregar `aecheverry@educacionbogota.gov.co` como opcion de contratista responsable IVA en ingreso local-dev |
| T14 | Correccion DEVUELTO editable | Asegurar que Contratista pueda entrar a correccion, modificar datos y reenviar informe `DEVUELTO` |
| T15 | Correccion DEVUELTO solo contratista | Asegurar que Revisor/Supervisor no puedan editar un informe ya devuelto al contratista |

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

## T10 — Correccion funcional post-revision 2026-05-12

**Origen:** revision funcional del 2026-05-12 sobre I7 publicado para pruebas.

**Hallazgos cubiertos:**

1. Guardar borrador perdia o reconstruia relaciones de actividades reportadas, soportes y documentos.
2. La UI mostraba dos bloques documentales: "Documentos requeridos" y "Documentos adicionales".
3. El contratista veia acciones que no corresponden despues de enviar.
4. Revisor/supervisor veian soportes URL con presentacion incorrecta.
5. Supervisor quedaba sin acciones funcionales para aprobar/devolver desde el detalle.
6. Documentos requeridos configurados por Admin no estaban completamente integrados como adjuntables/visualizables.

**Acciones implementadas:**

1. Backend:
   - `DocumentoRequeridoInformeService` lista y valida documentos configurados por Admin como requeridos del informe.
   - `InformeEstadoService.enviar()` deja de validar documentos adicionales y delega la exigencia documental a documentos requeridos.
2. Frontend:
   - Retirar secciones de documentos adicionales de nuevo/correccion/detalle/preview.
   - Mantener solo **Documentos Requeridos**.
   - Mostrar soportes URL como nombre + `Abrir`.
   - Controlar acciones por rol: revisor solo revisa en `ENVIADO`; supervisor aprueba/devuelve en `EN_REVISION`; contratista en `ENVIADO` solo consulta/vista previa.
   - En correccion, precargar el soporte URL existente y solo reemplazarlo si cambia.
3. Tests:
   - Ajustar specs Angular de informe nuevo, detalle y correccion.
   - Ajustar tests backend de documentos requeridos y estado de informe.

**Validacion requerida:**

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=DocumentoRequeridoInformeServiceTest,InformeEstadoServiceTest,InformeEstadoServiceI3Test,InformeEstadoServiceSinRevisorTest"
```

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/features/informes/detalle/informe-detalle.component.spec.ts --include src/app/features/informes/nuevo/informe-form.component.spec.ts --include src/app/features/informes/corregir/corregir-informe.component.spec.ts
```

**Commit asociado:** `3581409 fix: correct SIGCON informe workflow findings`

---

## T11 — Mejora funcional post-pruebas 2026-05-12

**Origen:** pruebas funcionales posteriores al ajuste `3581409`.

**Regla de secuencia SDD:**

1. Levantar hallazgos y decisiones funcionales.
2. Actualizar spec.
3. Actualizar plan.
4. Registrar punto de partida en execution log.
5. Solo despues corregir codigo y ejecutar validaciones.

**Hallazgos/mejoras cubiertas:**

1. Busqueda global requiere filtros combinados ademas de texto libre opcional.
2. Busqueda global debe soportar paginacion para volumen alto de registros.
3. Busqueda global debe retornar contratos e informes asociados que cumplan filtros.
4. Informe en estado `DEVUELTO` no permite al contratista modificar actividades, soportes ni aportes de seguridad social.
5. Al diligenciar informe, no se estan usando los datos predeterminados del usuario para `SALUD`, `PENSION` y `ARL`.

**Decisiones funcionales cerradas:**

- Texto libre de busqueda queda opcional.
- Filtros combinables:
  - estado del contrato;
  - rango de periodo del informe;
  - contratista;
  - revisor;
  - estado del informe.
- Resultado de busqueda: contratos e informes.
- Paginacion inicial: 20 registros por pagina.
- Ordenamiento default:
  1. periodo de informe mas reciente primero;
  2. prioridad operativa de estado: `EN_REVISION`, `ENVIADO`, `DEVUELTO`, `BORRADOR`, `APROBADO`;
  3. numero de contrato ascendente;
  4. contratista ascendente.
- En `DEVUELTO`, el contratista puede editar todo el informe.
- Reenvio de `DEVUELTO` pasa a `ENVIADO`.
- Aportes de seguridad social se editan campo a campo.
- Al crear/diligenciar informe, se deben precargar datos predeterminados del usuario para `SALUD`, `PENSION` y `ARL`, sin bloquear ajustes del contratista.

**Archivos candidatos a revisar antes de implementar:**

- Backend busqueda:
  - `sigcon-backend/src/main/java/.../BusquedaAdminController.java`
  - `sigcon-backend/src/main/java/.../BusquedaAdminService.java`
  - DTOs de busqueda administrativa.
  - tests `*Busqueda*Test`.
- Backend informes/estado:
  - `InformeService`
  - `InformeEstadoService`
  - DTOs de crear/actualizar informe.
  - tests `InformeServiceTest`, `InformeEstadoServiceTest`.
- Backend usuarios/aportes:
  - modelo/DTO de usuario contratista con datos predeterminados de seguridad social.
  - servicio que construye el formulario o respuesta inicial de informe.
- Frontend busqueda:
  - pantalla `/admin/busqueda`.
  - servicio Angular de busqueda.
  - specs de busqueda admin.
- Frontend informe devuelto:
  - `informe-detalle.component`
  - `corregir-informe.component`
  - `informe-form.component`
  - servicios/modelos de informe y aportes.

**Plan de implementacion posterior a este registro documental:**

1. Escribir/ajustar tests backend de busqueda con filtros combinados, paginacion y ordenamiento.
2. Implementar contrato API paginado para busqueda administrativa.
3. Ajustar UI de busqueda para filtros, paginador y orden visible.
4. Escribir/ajustar tests backend para edicion integral de `DEVUELTO` y reenvio a `ENVIADO`.
5. Ajustar frontend de correccion para habilitar actividades, soportes, documentos requeridos y aportes en `DEVUELTO`.
6. Precargar aportes de seguridad social desde datos predeterminados del usuario al crear/diligenciar informe.
7. Ejecutar pruebas focalizadas backend y frontend.
8. Actualizar execution log con resultados, commit y estado de publicacion.

**Validacion esperada:**

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=*Busqueda*Test,InformeServiceTest,InformeEstadoServiceTest"
```

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/features/admin/busqueda/*.spec.ts --include src/app/features/informes/**/*.spec.ts
```

**Commit sugerido de documentacion previa:** `docs: define SIGCON I7 post-test functional fixes`

**Commit sugerido de implementacion posterior:** `fix: apply SIGCON I7 post-test functional fixes`

---

## T12 — Usabilidad busqueda global 2026-05-12

**Origen:** ajuste funcional solicitado despues de pruebas de busqueda avanzada T11.

**Alcance:**

- Agregar boton `Limpiar` junto a `Buscar` en `/admin/busqueda`.
- Restablecer:
  - texto libre;
  - estado del contrato;
  - estado del informe;
  - periodo desde;
  - periodo hasta;
  - pagina actual;
  - error visible;
  - resultados avanzados y legacy.
- No ejecutar busqueda automaticamente al limpiar.

**Archivos candidatos:**

- `sigcon-angular/src/app/features/admin/busqueda/admin-busqueda.component.ts`
- `sigcon-angular/src/app/features/admin/busqueda/admin-busqueda.component.spec.ts`

**Validacion esperada:**

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/features/admin/busqueda/admin-busqueda.component.spec.ts
```

**Commit sugerido:** `fix: add clear filters action to admin search`

---

## T13 — Acceso local-dev contratista responsable IVA 2026-05-14

**Origen:** necesidad de pruebas funcionales con usuario contratista responsable de IVA.

**Alcance:**

- Agregar al menu de ingreso local-dev una opcion para:
  - `Alvaro Echeverry Salcedo`
  - `aecheverry@educacionbogota.gov.co`
  - `Asesor`
  - `CONTRATISTA`
- Mantener disponible el contratista local-dev existente.
- Agregar credencial local-dev HTTP Basic en backend para ese usuario con rol `CONTRATISTA`.
- Usar esta opcion solo para pruebas locales de flujos IVA/FACTURA.

**Archivos candidatos:**

- `sigcon-angular/src/app/features/auth/login.component.ts`
- `sigcon-angular/src/app/core/auth/dev-session.service.ts`
- `sigcon-angular/src/app/core/auth/auth.service.ts`
- `sigcon-angular/src/app/core/auth/dev-session.service.spec.ts`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java`

**Validacion esperada:**

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless --progress=false --include src/app/core/auth/dev-session.service.spec.ts
```

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=SigconBackendSecurityTest"
```

**Commit sugerido:** `fix: add IVA contractor to local dev login`

---

## T14 — Correccion funcional informe DEVUELTO editable 2026-05-14

**Origen:** cierre de revision funcional del 2026-05-14.

**Hallazgo:**

- El informe en estado `DEVUELTO` no permite modificar ningun dato.
- Esto incumple la premisa de T11: permitir correccion integral del informe devuelto.

**Alcance:**

- Revisar detalle de informe y accion disponible para contratista en `DEVUELTO`.
- Revisar ruta/componente de correccion para confirmar que cargue y habilite campos en `DEVUELTO`.
- Asegurar que actividades, soportes, aportes SGSSI y documentos requeridos queden editables.
- Asegurar que guardar y reenviar funcionen desde `DEVUELTO`.

**Archivos candidatos:**

- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.html`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `sigcon-angular/src/app/features/informes/corregir/corregir-informe.component.ts`
- `sigcon-angular/src/app/features/informes/corregir/corregir-informe.component.html`
- `sigcon-angular/src/app/features/informes/corregir/corregir-informe.component.spec.ts`
- `sigcon-backend/src/main/java/.../InformeService.java`
- `sigcon-backend/src/main/java/.../InformeEstadoService.java`
- tests backend de informes/estado si el bloqueo esta en API.

**Validacion esperada:**

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless --progress=false --include src/app/features/informes/detalle/informe-detalle.component.spec.ts --include src/app/features/informes/corregir/corregir-informe.component.spec.ts
```

Si se modifica backend:

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=InformeServiceTest,InformeEstadoServiceTest"
```

**Commit sugerido:** `fix: enable editing returned informes`

---

## T15 — Correccion funcional DEVUELTO solo contratista 2026-05-14

**Origen:** cierre de revision funcional del 2026-05-14.

**Hallazgo:**

- Una vez el revisor devuelve el informe y queda en estado `DEVUELTO`, desde la vista del Revisor aun se pueden modificar datos.
- Esto incumple la regla funcional: la correccion del informe devuelto corresponde al contratista propietario.

**Alcance:**

- Revisar la compuerta de edicion en la vista detalle.
- Asegurar que `DEVUELTO` habilite controles solo cuando el usuario tenga rol `CONTRATISTA`.
- Asegurar que Revisor/Supervisor vean `DEVUELTO` en modo solo lectura.
- Mantener `DEVUELTO` editable para contratista propietario, segun T14.
- Mantener `ENVIADO`, `EN_REVISION` y `APROBADO` sin edicion para contratista.

**Archivos candidatos:**

- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.html`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `sigcon-backend/src/main/java/.../InformeService.java` si se identifica brecha de seguridad API.

**Validacion esperada:**

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless --progress=false --include src/app/features/informes/detalle/informe-detalle.component.spec.ts
```

Si se modifica backend:

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=InformeServiceTest,InformeSecurityTest"
```

**Commit sugerido:** `fix: restrict returned informe editing to contractors`

---

## Orden de Ejecucion

```text
T0 -> T1
T2 -> T3
T4 -> T5 -> T6
T7
T8
T9
T10
T11
T12
T13
T14
T15
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
