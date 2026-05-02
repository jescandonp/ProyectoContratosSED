# SIGCON I2 Execution Log

> Documento de handoff compartido para trabajo alternado entre Codex, Claude y otros modelos.
> Fuente rectora: `docs/plans/2026-05-01-sigcon-i2-implementation-plan.md`.
> Spec rectora: `docs/specs/2026-05-01-sigcon-i2-spec.md`.

## Reglas De Coordinacion Multi-Modelo

1. **Sincronizar antes de iniciar:** `git fetch origin && git pull --ff-only origin feat/sigcon-i2`.
2. **Una tarea activa por modelo.** Verificar la tabla "Estado De Tareas I2" antes de tomar una task; si alguna esta en `🔄 in progress`, esperar a que cierre o reasignar.
3. **Actualizar este log al cerrar cada task** con commit SHA, archivos tocados, validaciones ejecutadas y desviaciones respecto al plan.
4. **Inconsistencias spec/plan**: parchear plan o spec en la misma task y dejar nota en el log; nunca silenciarlas en codigo.
5. **Saltos o reordenamientos**: documentar el por que en el log.
6. **Bloqueos**: marcar la task como `⏸ blocked` con razon y "que me desbloquearia".

## Estado De Rama

- Rama activa: `feat/sigcon-i2`
- Punto de partida: `feat/sigcon-i1` cerrada (`be26bbe docs: stamp Task 10 final commit SHA in execution log`).
- Remoto: `origin/feat/sigcon-i2`.
- Cambio local no versionado conocido: `.claude/` queda fuera de Git por configuracion local.

## Ajustes Plan I2 Tras Cierre I1

(Resumen — el detalle vive en el plan, seccion "Adjustments After I1 Closure".)

- `UsuarioRequest` ya incluye `email`; `Authentication.getName()` retorna email completo.
- `SigconBackendApplicationTests` excluye `application.service.*` y `web.controller.*`. I2 mantiene el patron.
- `JpaAuditingConfig.auditorProvider` retorna `"SYSTEM"`. Reemplazo por principal real es deuda I3.
- `DocumentStorageService` + `LocalDocumentStorageService` reusables para `SoporteAdjunto` archivo.
- `Page<T>` ya expone `first` y `last`.
- PrimeNG sigue en 20.x, no 21.
- `npm` global roto en la maquina actual; usar `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" ...`.
- Placeholders I1 a activar en Task 12: boton "Nuevo Informe" en detalle de contrato y card "Informes" en admin dashboard.
- Maven corre con JDK 21 y target/source 1.8. Falta validar JDK 8 real antes de produccion (no bloquea I2).

## Estado De Tareas I2

| Task | Estado | Modelo | Commit | Notas |
|---|---|---|---|---|
| Task 1 - Plan + execution log baseline | ✅ done | Claude | `ea33f5b` | Plan promovido desde outline; ajustes I1 incorporados; reglas multi-modelo agregadas |
| Task 2 - Oracle schema I2 | ✅ done | Claude | `eece45a` | DDL apendizado bajo cabecera `===== INCREMENTO 2 =====`; seed con informe BORRADOR para OPS-2026-001 |
| Task 3 - Backend domain (entities/enums/repos) | ✅ done | Claude | `4be9518` | 5 entidades + 3 enums + 5 repos + 5 tests pasan; mvn test 26/26 |
| Task 4 - Backend DTOs/mappers/services CRUD | ✅ done | Codex | `07a02ce` | DTOs/mappers/services CRUD sin transiciones; 43 backend tests pasan |
| Task 5 - State machine `InformeEstadoService` | ✅ done | Codex | `2eebe24` | Transiciones I2 implementadas; 56 backend tests pasan |
| Task 6 - Backend controllers/security/swagger | ✅ done | Codex | `9889e07` | Controllers REST I2 + RBAC + Swagger; 64 backend tests pasan |
| Task 7 - Frontend models + services | ✅ done | Codex | `aa1ea19` | Modelos TS + services Angular I2; 15 frontend specs pasan |
| Task 8 - Informe form + detalle + preview | ✅ done | Codex | `f40a95b` | Form, detalle y preview I2; 23 frontend specs pasan |
| Task 9 - Corregir informe devuelto | ✅ done | Claude | `4ac95c2` | CorregirInformeComponent con panel de observaciones; 30 frontend specs pasan |
| Task 10 - Cola de revision (REVISOR) | ✅ done | Claude | `f9c81d5` | ColaRevisionComponent + sidebar REVISOR/SUPERVISOR; 39 frontend specs pasan |
| Task 11 - Cola de aprobacion (SUPERVISOR) | ✅ done | Claude | `54b6fe7` | ColaAprobacionComponent + aprobarInforme en ObservacionService; 46 frontend specs pasan |
| Task 12 - Activar placeholders I1 | ✅ done | Claude | `685a61b` | Historial de informes en detalle contrato; card Informes activa en admin; 53 specs pasan |
| Task 13 - Verificacion E2E + docs | ⏳ pending | — | — | — |

Leyenda: ⏳ pending, 🔄 in progress, ✅ done, ⏸ blocked, ⚠ deviation.

## Restricciones De Alcance I2

No se debe implementar:

- Generacion real de PDF (`PdfService`, plantillas PDF, archivos PDF).
- Firmas incrustadas en PDF.
- Notificaciones email (SMTP, Microsoft Graph mail, Azure email).
- Notificaciones in-app (centro de notificaciones, polling, badge).
- Endpoints `/api/notificaciones`.
- Tabla `SGCN_NOTIFICACIONES`.
- Integracion SECOP2.
- Contratos personas juridicas.

Validacion de alcance al cerrar cada task con codigo nuevo:

```powershell
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "Pdf|notificacion|/api/notificaciones|MailService|SGCN_NOTIFICACIONES"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|pdf.service"
```

## Decisiones Y Notas Para El Siguiente Modelo

- Task 4 extendio `DocumentStorageService` con `storeFile(String subdir, MultipartFile file)` y adapto `LocalDocumentStorageService`; `storeSignature(...)` queda intacto para I1.
- Task 5 agrego `InformeEstadoService` como punto unico de transiciones. Incluye metodos canonicos `enviar`, `aprobarRevision`, `devolverRevision`, `aprobar`, `devolver`; tambien deja aliases `devolverEnRevision` y `devolverFinal` por coherencia con el texto del plan.
- Task 6 agrego la superficie REST I2: `InformeController`, `ActividadInformeController`, `SoporteAdjuntoController`, `DocumentoAdicionalInformeController`. Las transiciones siguen delegadas exclusivamente a `InformeEstadoService`; los controllers no duplican reglas de estado.
- `DevSecurityConfig` y `SecurityConfig` exponen `/api/informes/**` y `/api/actividades/**` con reglas por rol y `@PreAuthorize`. `/api/notificaciones` sigue inexistente.
- Task 7 agrego models/services Angular en `core/` para consumir la superficie REST I2. `ObservacionService` no crea endpoint propio: encapsula las transiciones que llevan `ObservacionRequest` sobre `/api/informes/{id}/aprobar-revision`, `/devolver-revision` y `/devolver`.
- `db/00_setup.sql` se mantiene como archivo unico (PRD), pero las inserciones I2 van bajo cabecera explicita `-- ===== INCREMENTO 2 =====`.
- En Task 12 hay una decision de scope: ADMIN read-only de informes vs. "proximamente". Default sugerido: "proximamente" para no inflar I2. Quien tome Task 12 debe documentar la decision aqui.
- Codes de error nuevos (`INFORME_NO_ENCONTRADO`, `INFORME_NO_EDITABLE`, `TRANSICION_INVALIDA`, `OBSERVACION_REQUERIDA`, `ACTIVIDAD_REQUERIDA`, `PORCENTAJE_INVALIDO`, `SOPORTE_INVALIDO`, `DOCUMENTO_ADICIONAL_REQUERIDO`, `CONTRATO_NO_ACTIVO`) ya estan en `web.exception.ErrorCode`; Task 5 ya usa los contratos de transicion.
- En `feat/sigcon-i1` aun no hay merge a `main`. I2 trabaja directamente sobre `feat/sigcon-i2` partiendo de `feat/sigcon-i1`. La estrategia de merge la decide el modelo que cierre I3 o el integrador final.
- Inconsistencia documental detectada y corregida: `README.md` seguia indicando fase previa a implementacion I1; ahora remite al execution log activo y registra que I2 esta en `feat/sigcon-i2`.

## Task 2 Implementado

Archivos tocados:

- `db/00_setup.sql` — apendizada seccion I2 con `SGCN_INFORMES`, `SGCN_ACTIVIDADES`, `SGCN_SOPORTES`, `SGCN_DOCS_ADICIONALES`, `SGCN_OBSERVACIONES`, secuencias, indices y triggers `TRG_*_AUDIT`.
- `db/01_datos_prueba.sql` — apendizado bloque I2: 1 informe BORRADOR sobre `OPS-2026-001`, 3 actividades (una por obligacion), 1 soporte URL + 1 soporte ARCHIVO, 1 documento adicional al catalogo obligatorio.

Decisiones:

- Archivo unico `00_setup.sql` se conserva (PRD), con cabecera explicita `-- ===== INCREMENTO 2 =====` para trazabilidad.
- Las inserciones I2 asumen los IDs secuenciales generados por las inserciones I1: contrato `OPS-2026-001` = ID 1, obligaciones = 1, 2, 3, catalogo "Planilla aportes" = ID 1.
- No se uso `SGCN_NOTIFICACIONES` ni columnas PDF nuevas (forward-compat I3).

Validaciones:

```powershell
Select-String -Path db/00_setup.sql -Pattern "SGCN_INFORMES|SGCN_ACTIVIDADES|SGCN_SOPORTES|SGCN_DOCS_ADICIONALES|SGCN_OBSERVACIONES|TRG_INFORMES_AUDIT|SGCN_NOTIFICACIONES"
Select-String -Path db/01_datos_prueba.sql -Pattern "SGCN_INFORMES|SGCN_ACTIVIDADES|BORRADOR|SGCN_NOTIFICACIONES"
```

Resultado: 36 coincidencias I2 en `00_setup.sql`, 6 lineas relevantes en `01_datos_prueba.sql`, 0 coincidencias de `SGCN_NOTIFICACIONES`.

Pendiente: ejecutar los scripts contra una BD Oracle real local-dev cuando el modelo siguiente disponga de DBA/credenciales.

## Task 3 Implementado

Archivos creados:

- Enums: `domain/enums/EstadoInforme.java`, `TipoSoporte.java`, `RolObservacion.java`.
- Entidades: `domain/entity/Informe.java`, `ActividadInforme.java`, `SoporteAdjunto.java`, `DocumentoAdicional.java`, `Observacion.java`. Todas con `@EntityListeners(AuditingEntityListener.class)`, soft delete via `activo`, `SequenceGenerator(allocationSize=1)`.
- Repositorios: `domain/repository/InformeRepository.java`, `ActividadInformeRepository.java`, `SoporteAdjuntoRepository.java`, `DocumentoAdicionalRepository.java`, `ObservacionRepository.java`. Firmas exactas del spec §4.3 mas un `findByContratoIdAndActivoTrue` extra que necesitara la pantalla de detalle de contrato (Task 12).
- Test: `test/java/.../domain/InformeDomainMappingTest.java` con 5 metodos cubriendo mapeo a tablas, secuencias, ManyToOne, enums y nullability de `pdfRuta`.

Decisiones I3 forward-compat:

- `Informe.pdfRuta` queda `nullable=true` y sin handling en I2.
- `Informe.fechaCreacion` y `Observacion.fecha` son `insertable=false, updatable=false` para que Oracle aplique `SYSTIMESTAMP` por DEFAULT.
- `Informe.fechaUltimoEnvio` y `Informe.fechaAprobacion` se setean explicitamente en `InformeEstadoService` (Task 5).

Validaciones:

```powershell
cd sigcon-backend
mvn test -Dtest=InformeDomainMappingTest
mvn test
```

Resultado:

- `InformeDomainMappingTest`: 5/5 pasan.
- `mvn test`: 26 tests, 0 fallas, 0 errores (21 I1 + 5 nuevos).

## Task 4 Implementado

Commit funcional:

- `07a02ce feat: add SIGCON I2 informe DTOs, mappers, and CRUD services`

Archivos creados/modificados:

- DTOs I2: `application/dto/informe/*` con los 11 contratos de datos definidos en spec §4.4.
- Mappers manuales: `InformeMapper`, `ActividadInformeMapper`, `SoporteAdjuntoMapper`, `DocumentoAdicionalMapper`, `ObservacionMapper`.
- Servicios CRUD sin transiciones: `InformeService`, `ActividadInformeService`, `SoporteAdjuntoService`, `DocumentoAdicionalInformeService`, `ObservacionService`.
- Storage: `DocumentStorageService.storeFile(...)` y `LocalDocumentStorageService.storeFile(...)` para soportes tipo `ARCHIVO`.
- Errores I2 en `web.exception.ErrorCode`.
- `SigconBackendSecurityTest` mockea repositorios I2 para mantener cargable el contexto local-dev sin Oracle.
- Tests dedicados: `InformeServiceTest`, `ActividadInformeServiceTest`, `SoporteAdjuntoServiceTest`, `DocumentoAdicionalInformeServiceTest`, `ObservacionServiceTest`; `DocumentStorageServiceTest` cubre almacenamiento de soportes y bloqueo de path traversal.

Cobertura cerrada en Task 4:

- Contratista crea informe `BORRADOR` solo sobre contrato propio `EN_EJECUCION`.
- Contrato ajeno retorna `ACCESO_DENEGADO`; contrato fuera de ejecucion retorna `CONTRATO_NO_ACTIVO`.
- Informe `APROBADO` no es editable (`INFORME_NO_EDITABLE`).
- Actividades validan porcentaje 0-100 (`PORCENTAJE_INVALIDO`).
- Soportes URL aceptan solo `http://`/`https://` (`SOPORTE_INVALIDO`).
- Soportes archivo delegan a `DocumentStorageService` con subdir `soportes/{contratoId}/{informeId}/{actividadId}`.
- Documentos adicionales se asocian al informe usando catalogo activo.
- Observaciones vacias retornan `OBSERVACION_REQUERIDA`; el uso transicional queda para Task 5.

Validaciones:

```powershell
cd sigcon-backend
mvn test -Dtest=*ServiceTest
mvn test
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "Pdf|notificacion|/api/notificaciones|MailService|SGCN_NOTIFICACIONES"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|pdf.service"
Get-ChildItem -Path sigcon-backend\src\main\java,sigcon-backend\src\test\java -Recurse -Include *.java | Select-String -Pattern "\bvar\b|List\.of|Map\.of|Set\.of|Optional\.orElseThrow\(\)"
```

Resultado:

- `mvn test -Dtest=*ServiceTest`: 26 tests, 0 fallas, 0 errores.
- `mvn test`: 43 tests, 0 fallas, 0 errores.
- Compatibilidad Java 8: 0 coincidencias para `var`, `List.of`, `Map.of`, `Set.of`, `Optional.orElseThrow()`.
- Auditoria de alcance backend: solo aparecen referencias esperadas a `Informe.pdfRuta` heredadas de Task 3 como compatibilidad I3; no hay `PdfService`, notificaciones ni `/api/notificaciones`.
- Auditoria de alcance frontend: 0 coincidencias.

## Task 5 Implementado

Commit funcional:

- `2eebe24 feat: add SIGCON I2 informe state machine`

Archivos creados/modificados:

- `application/service/InformeEstadoService.java` — maquina de estados I2.
- `test/java/.../application/InformeEstadoServiceTest.java` — 11 pruebas unitarias de transiciones y contratos de error.
- `test/java/.../application/InformeServiceTest.java` — 2 pruebas adicionales para colas paginadas de revisor/supervisor.

Transiciones implementadas:

- `enviar(informeId, contratistaEmail)`: `BORRADOR|DEVUELTO -> ENVIADO`, exige al menos una actividad y setea `fechaUltimoEnvio`.
- `aprobarRevision(informeId, revisorEmail, observacionOpcional)`: `ENVIADO -> EN_REVISION`, solo revisor asignado; registra observacion si se envia texto.
- `devolverRevision(informeId, revisorEmail, observacion)`: `ENVIADO -> DEVUELTO`, solo revisor asignado y observacion obligatoria.
- `aprobar(informeId, supervisorEmail)`: `EN_REVISION -> APROBADO`, solo supervisor asignado, setea `fechaAprobacion` y fuerza `pdfRuta = null`.
- `devolver(informeId, supervisorEmail, observacion)`: `EN_REVISION -> DEVUELTO`, solo supervisor asignado y observacion obligatoria.
- Aliases mantenidos por coherencia documental: `devolverEnRevision(...)` y `devolverFinal(...)`.

Contratos de error cubiertos:

- `ACTIVIDAD_REQUERIDA` para envio sin actividades.
- `OBSERVACION_REQUERIDA` para devoluciones sin texto.
- `ACCESO_DENEGADO` para principal no asignado al contrato.
- `TRANSICION_INVALIDA` para estados fuente no permitidos.
- `INFORME_NO_EDITABLE` para `APROBADO` terminal.

Validaciones:

```powershell
cd sigcon-backend
mvn test -Dtest=InformeEstadoServiceTest
mvn test "-Dtest=InformeEstadoServiceTest,InformeServiceTest"
mvn test -Dtest=*ServiceTest
mvn test
Get-ChildItem -Path sigcon-backend\src\main\java,sigcon-backend\src\test\java -Recurse -Include *.java | Select-String -Pattern "\bvar\b|List\.of|Map\.of|Set\.of|Optional\.orElseThrow\(\)"
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "Pdf|notificacion|/api/notificaciones|MailService|SGCN_NOTIFICACIONES"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|pdf.service"
```

Resultado:

- `InformeEstadoServiceTest`: 11 tests, 0 fallas, 0 errores.
- `InformeEstadoServiceTest,InformeServiceTest`: 20 tests, 0 fallas, 0 errores.
- `mvn test -Dtest=*ServiceTest`: 39 tests, 0 fallas, 0 errores.
- `mvn test`: 56 tests, 0 fallas, 0 errores.
- Compatibilidad Java 8: 0 coincidencias para `var`, `List.of`, `Map.of`, `Set.of`, `Optional.orElseThrow()`.
- Auditoria de alcance backend: solo referencias esperadas a `Informe.pdfRuta` y `InformeEstadoService.setPdfRuta(null)`; no hay `PdfService`, notificaciones ni `/api/notificaciones`.
- Auditoria de alcance frontend: 0 coincidencias.

## Task 6 Implementado

Commit funcional:

- `9889e07 feat: add SIGCON I2 informe REST controllers and security`

Archivos creados/modificados:

- Controllers REST I2:
  - `web/controller/InformeController.java`
  - `web/controller/ActividadInformeController.java`
  - `web/controller/SoporteAdjuntoController.java`
  - `web/controller/DocumentoAdicionalInformeController.java`
- Seguridad:
  - `config/DevSecurityConfig.java`
  - `config/SecurityConfig.java`
- Tests:
  - `web/InformeSecurityTest.java`
  - `web/SigconBackendSecurityTest.java`

Endpoints implementados:

- `GET /api/informes`
- `GET /api/informes/{id}`
- `POST /api/informes`
- `PUT /api/informes/{id}`
- `POST /api/informes/{id}/enviar`
- `POST /api/informes/{id}/aprobar-revision`
- `POST /api/informes/{id}/devolver-revision`
- `POST /api/informes/{id}/aprobar`
- `POST /api/informes/{id}/devolver`
- `POST /api/informes/{id}/actividades`
- `PUT /api/informes/{id}/actividades/{actividadId}`
- `DELETE /api/informes/{id}/actividades/{actividadId}`
- `POST /api/actividades/{actividadId}/soportes/url`
- `POST /api/actividades/{actividadId}/soportes/archivo`
- `DELETE /api/actividades/{actividadId}/soportes/{soporteId}`
- `POST /api/informes/{id}/documentos-adicionales`
- `DELETE /api/informes/{id}/documentos-adicionales/{documentoId}`

Decisiones y ajustes:

- Se habilito `@EnableGlobalMethodSecurity(prePostEnabled = true)` en `local-dev` y `weblogic` para que `@PreAuthorize` sea efectivo.
- Las reglas URL bloquean acciones por rol antes de llegar al controller; el service sigue aplicando pertenencia y reglas de negocio.
- `SigconBackendSecurityTest` actualizo el contrato anterior de I1: `/api/informes` ya no es futuro en I2; ahora existe y, para ADMIN sin `contratoId`, retorna `ACCESO_DENEGADO`. `/api/notificaciones` continua `404`.
- `SigconBackendApplicationTests` no requirio cambios: ya excluye `application.service.*` y `web.controller.*`.

Validaciones:

```powershell
cd sigcon-backend
mvn test -Dtest=InformeSecurityTest
mvn test -Dtest=*SecurityTest
mvn test
mvn test -DskipTests
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "/api/notificaciones|Pdf|notificacion.service"
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "\bvar\b|List\.of|Map\.of|Set\.of|Optional\.orElseThrow\(\)"
```

Resultado:

- `InformeSecurityTest`: 8 tests, 0 fallas, 0 errores.
- `mvn test -Dtest=*SecurityTest`: 16 tests, 0 fallas, 0 errores.
- `mvn test`: 64 tests, 0 fallas, 0 errores.
- `mvn test -DskipTests`: build success.
- Compatibilidad Java 8: 0 coincidencias para `var`, `List.of`, `Map.of`, `Set.of`, `Optional.orElseThrow()`.
- Auditoria de alcance backend: solo referencias esperadas a `Informe.pdfRuta`/`InformeEstadoService.setPdfRuta(null)`; no hay `PdfService`, `notificacion.service` ni `/api/notificaciones`.
- Auditoria de alcance frontend: no aplica en Task 6; no se tocaron archivos Angular.

## Task 7 Implementado

Commit funcional:

- `aa1ea19 feat: add SIGCON I2 frontend core models and services`

Archivos creados:

- Models:
  - `core/models/informe.model.ts`
  - `core/models/actividad-informe.model.ts`
  - `core/models/soporte-adjunto.model.ts`
  - `core/models/documento-adicional.model.ts`
  - `core/models/observacion.model.ts`
- Services:
  - `core/services/informe.service.ts`
  - `core/services/actividad-informe.service.ts`
  - `core/services/soporte-adjunto.service.ts`
  - `core/services/documento-adicional.service.ts`
  - `core/services/observacion.service.ts`
- Tests:
  - `core/services/informe.service.spec.ts`
  - `core/services/actividad-informe.service.spec.ts`
  - `core/services/soporte-adjunto.service.spec.ts`
  - `core/services/documento-adicional.service.spec.ts`
  - `core/services/observacion.service.spec.ts`

Servicios implementados:

- `InformeService`: `listarInformes`, `obtenerDetalle`, `crearInforme`, `actualizarInforme`, `enviarInforme`, `aprobarInforme`.
- `ActividadInformeService`: `crear`, `actualizar`, `eliminar`.
- `SoporteAdjuntoService`: `agregarUrl`, `agregarArchivo`, `eliminar`.
- `DocumentoAdicionalService`: `agregar`, `eliminar`.
- `ObservacionService`: transiciones con observacion (`aprobarRevision`, `devolverRevision`, `devolverInforme`) usando endpoints de `InformeController`.

Decisiones:

- No se agrego estado con signals en estos services porque son clientes HTTP delgados, igual que I1 (`ContratoService`, `UsuarioService`). El estado de pantalla se deja para las features de Task 8+.
- `ObservacionService` no introduce `/api/observaciones`; respeta Task 6 y usa los endpoints existentes bajo `/api/informes/**`.
- Los modelos aceptan `null` donde los mappers backend pueden omitir valores por relaciones nulas: contrato, usuarios, fechas de envio/aprobacion y campos derivados de catalogo/obligacion.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|pdf.service"
```

Resultado:

- TDD RED inicial: Angular fallo por imports faltantes de los nuevos models/services, como se esperaba.
- `npm test -- --watch=false`: 15 specs, 0 fallas.
- `npm run build`: build success; output en `sigcon-angular/dist/sigcon-angular`.
- Auditoria de alcance frontend: 0 coincidencias para `/api/notificaciones`, `notificacion.service`, `pdf.service`.

## Task 8 Implementado

Commit funcional:

- `f40a95b feat: add SIGCON I2 informe form, detalle and preview screens`

Archivos creados:

- `features/informes/nuevo/informe-form.component.ts`
- `features/informes/nuevo/informe-form.component.spec.ts`
- `features/informes/detalle/informe-detalle.component.ts`
- `features/informes/detalle/informe-detalle.component.spec.ts`
- `features/informes/preview/informe-preview.component.ts`
- `features/informes/preview/informe-preview.component.spec.ts`

Archivos modificados:

- `app.routes.ts`: agrega rutas `contratos/:contratoId/informes/nuevo`, `informes/:id`, `informes/:id/preview`.
- `app.routes.spec.ts`: actualiza la superficie exacta de rutas I2 hasta Task 8.

Implementado:

- Formulario contratista para crear informe desde contrato, cargando obligaciones ordenadas y documentos OPS desde catalogo.
- Guardado de borrador con `POST /api/informes`, actividades por obligacion con `POST /api/informes/{id}/actividades`, soportes URL/archivo y documentos adicionales.
- Confirmacion antes de enviar, delegando en `POST /api/informes/{id}/enviar`.
- Detalle de informe con actividades, soportes, documentos, actores, observaciones y accion de vista previa.
- Preview read-only tipo documento, sin generacion real de PDF.

Decisiones:

- La ruta de nuevo informe queda protegida para `CONTRATISTA`; detalle y preview quedan autenticadas por el shell y delegan visibilidad fina al backend, porque esas pantallas tambien seran usadas por revision/supervision en Tasks 10-11.
- No se activo el boton de nuevo informe dentro de `contrato-detalle.component.ts`; esa integracion de flujo queda para una task posterior del plan para no mezclar alcance.
- La edicion/correccion de borradores/devueltos no se implementa aqui; Task 9 reutilizara el formulario con ruta `informes/:id/corregir`.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|PdfService|pdf.service|/api/pdf"
```

Resultado:

- TDD RED inicial: Angular fallo por imports faltantes de `InformeFormComponent`, `InformeDetalleComponent`, `InformePreviewComponent`, como se esperaba.
- `npm test -- --watch=false`: 23 specs, 0 fallas.
- `npm run build`: build success; output en `sigcon-angular/dist/sigcon-angular`.
- Auditoria de alcance frontend: 0 coincidencias para `/api/notificaciones`, `notificacion.service`, `PdfService`, `pdf.service`, `/api/pdf`.

## Task 9 Implementado

Commit funcional:

- `4ac95c2 feat: add SIGCON I2 corregir informe screen`

Archivos creados/modificados:

- `features/informes/corregir/corregir-informe.component.ts` — componente de corrección de informe devuelto.
- `features/informes/corregir/corregir-informe.component.spec.ts` — 6 specs cubriendo carga, observaciones, pre-llenado, bloqueo por estado inválido, guardado y reenvío.
- `app.routes.ts` — agrega ruta `informes/:id/corregir` con `roleGuard(['CONTRATISTA'])`.
- `app.routes.spec.ts` — actualiza superficie de rutas a 16 (Task 9).

Implementado:

- Formulario de corrección que reutiliza el layout de `InformeFormComponent` (actividades por obligación, soportes URL/archivo, documentos adicionales).
- Pre-llenado del formulario con actividades y documentos existentes del informe cargado desde `GET /api/informes/{id}`.
- Panel lateral con observaciones históricas (REVISOR/SUPERVISOR) destacadas visualmente con borde rojo.
- Edición solo permitida cuando estado ∈ {`BORRADOR`, `DEVUELTO`}; estados inválidos muestran mensaje y bloquean el formulario.
- Guardado de correcciones: `PUT /api/informes/{id}` + `PUT /api/informes/{id}/actividades/{actividadId}` para actividades existentes; `POST` para actividades nuevas.
- Documentos adicionales nuevos (sin `idDocumento`) se agregan vía `POST /api/informes/{id}/documentos-adicionales`.
- Confirmación explícita antes de reenviar (`POST /api/informes/{id}/enviar`).
- Navegación de vuelta al detalle del informe al guardar o cancelar.

Decisiones:

- Se reutilizó el layout de `InformeFormComponent` sin duplicar lógica de validación; las diferencias clave son: pre-llenado desde informe existente, uso de `actualizar` en lugar de `crear` para actividades, y panel lateral de observaciones.
- Los soportes existentes no se re-envían en la corrección (solo se agregan nuevos); eliminar soportes existentes queda fuera del scope de I2 (no hay endpoint de eliminación masiva en el flujo de corrección).
- `SlicePipe` importado para formatear fechas de observaciones en el template.
- La ruta `informes/:id/corregir` queda protegida para `CONTRATISTA`; el backend valida adicionalmente que el informe pertenezca al contratista autenticado.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|PdfService|pdf.service|/api/pdf"
```

Resultado:

- `npm test -- --watch=false`: 30 specs, 0 fallas (23 anteriores + 6 nuevas de `CorregirInformeComponent` + 1 actualizada de `app.routes.spec`).
- `npm run build`: build success; `corregir-informe-component` aparece como lazy chunk de 16.84 kB.
- Auditoría de alcance frontend: 0 coincidencias para `/api/notificaciones`, `notificacion.service`, `PdfService`, `pdf.service`, `/api/pdf`.

## Task 10 Implementado

Commit funcional:

- `f9c81d5 feat: add SIGCON I2 cola de revision`

Archivos creados/modificados:

- `features/revision/cola-revision.component.ts` — tabla paginada de informes `ENVIADO` para REVISOR.
- `features/revision/cola-revision.component.spec.ts` — 7 specs cubriendo carga, estado vacío, navegación, aprobar revisión, bloqueo de devolución sin observación, devolución con observación y cierre de diálogo.
- `app.routes.ts` — agrega ruta `revision/informes` con `roleGuard(['REVISOR'])`.
- `app.routes.spec.ts` — actualiza superficie de rutas a 17 (Task 10).
- `shared/components/sidebar/sidebar.component.ts` — agrega entradas "Revision" (REVISOR) y "Aprobacion" (SUPERVISOR) condicionales por rol.
- `shared/components/sidebar/sidebar.component.spec.ts` — agrega 2 specs para REVISOR y SUPERVISOR.

Implementado:

- Tabla paginada con filtros por contrato, contratista y periodo (aplicados en cliente; el backend filtra por revisor autenticado).
- Acción "Aprobar revisión" → `POST /api/informes/{id}/aprobar-revision` vía `ObservacionService`.
- Acción "Devolver" abre diálogo modal con textarea de observación obligatoria → `POST /api/informes/{id}/devolver-revision`.
- Bloqueo de envío si la observación está vacía con mensaje de error inline.
- Paginación con botones Anterior/Siguiente usando `page.first` y `page.last`.
- Sidebar actualizado: "Revision" visible solo para REVISOR, "Aprobacion" visible solo para SUPERVISOR (anticipando Task 11 sin duplicar trabajo).

Decisiones:

- El sidebar ya incluye la entrada "Aprobacion" para SUPERVISOR aunque Task 11 aún no existe; la ruta `aprobacion/informes` se agregará en Task 11. Esto evita tocar el sidebar dos veces.
- Los filtros de contrato/contratista/periodo son campos de UI; el endpoint `GET /api/informes` ya filtra por revisor autenticado en backend. Los filtros de texto se pueden extender en I3 si se añaden query params al backend.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|PdfService|pdf.service|/api/pdf"
```

Resultado:

- `npm test -- --watch=false`: 39 specs, 0 fallas (30 anteriores + 7 nuevas de `ColaRevisionComponent` + 2 nuevas de `SidebarComponent`).
- `npm run build`: build success; `cola-revision-component` aparece como lazy chunk de 12.52 kB.
- Auditoría de alcance frontend: 0 coincidencias.

## Task 11 Implementado

Commit funcional:

- `54b6fe7 feat: add SIGCON I2 cola de aprobacion`

Archivos creados/modificados:

- `features/aprobacion/cola-aprobacion.component.ts` — tabla paginada de informes `EN_REVISION` para SUPERVISOR.
- `features/aprobacion/cola-aprobacion.component.spec.ts` — 7 specs cubriendo carga, estado vacío, navegación, aprobar, bloqueo de devolución sin observación, devolución con observación y cierre de diálogo.
- `core/services/observacion.service.ts` — agrega `aprobarInforme(informeId)` → `POST /api/informes/{id}/aprobar`.
- `core/services/observacion.service.spec.ts` — agrega test del nuevo método `aprobarInforme`.
- `app.routes.ts` — agrega ruta `aprobacion/informes` con `roleGuard(['SUPERVISOR'])`.
- `app.routes.spec.ts` — actualiza superficie de rutas a 18 (Task 11).

Implementado:

- Mismo patrón que `ColaRevisionComponent` con las diferencias de rol y transición:
  - Muestra informes `EN_REVISION` (el backend filtra por supervisor autenticado).
  - Acción "Aprobar" → `ObservacionService.aprobarInforme()` → `POST /api/informes/{id}/aprobar`.
  - Acción "Devolver" → diálogo con observación obligatoria → `ObservacionService.devolverInforme()` → `POST /api/informes/{id}/devolver`.
- `ObservacionService.aprobarInforme` añadido como método nuevo (faltaba; `InformeService.aprobarInforme` existía pero no era el canal correcto para la cola de aprobación).

Decisiones:

- Se usó `ObservacionService` como punto de entrada para las transiciones de supervisor, igual que para revisor, para mantener coherencia: todas las transiciones con observación (o sin ella) pasan por `ObservacionService`.
- El sidebar ya tenía la entrada "Aprobacion" desde Task 10; no se tocó.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|PdfService|pdf.service|/api/pdf"
```

Resultado:

- `npm test -- --watch=false`: 46 specs, 0 fallas (39 anteriores + 7 nuevas de `ColaAprobacionComponent`).
- `npm run build`: build success; `cola-aprobacion-component` aparece como lazy chunk de 12.07 kB.
- Auditoría de alcance frontend: 0 coincidencias.

## Task 12 Implementado

Commit funcional:

- `685a61b feat: activate I1 informe placeholders for I2`

Archivos creados/modificados:

- `features/contratos/detalle/contrato-detalle.component.ts` — historial de informes activo con `GET /api/informes?contratoId=...`; botón "Nuevo Informe" habilitado para contratos `EN_EJECUCION`.
- `features/contratos/detalle/contrato-detalle.component.spec.ts` — 4 specs nuevas cubriendo carga del contrato, carga del historial, estado vacío y visibilidad del botón.
- `features/admin/dashboard/admin-dashboard.component.ts` — card "Informes" activada como enlace a `/contratos`.
- `features/admin/dashboard/admin-dashboard.component.spec.ts` — 3 specs cubriendo renderizado de las 4 cards, que la card Informes es un `<a>` activo sin `opacity-60`, y que enlaza a `/contratos`.

Implementado:

- `ContratoDetalleComponent` ahora inyecta `InformeService` y carga el historial de informes del contrato al inicializar.
- El botón "+ Nuevo Informe" es un `routerLink` a `/contratos/:id/informes/nuevo`, visible solo cuando `estado === 'EN_EJECUCION'`.
- Cada informe del historial es un enlace a `/informes/:id` con chip de estado.
- `RouterLink` importado en el componente para los enlaces del historial y el botón.

Decisión Task 12 Step 2 (ADMIN):

- La card "Informes" del admin dashboard se activó como enlace a `/contratos` (lista de contratos). Desde allí el ADMIN puede navegar al detalle de cualquier contrato y ver su historial de informes. Esta decisión evita crear una vista read-only de informes específica para ADMIN (que inflaría el scope de I2) y cumple el criterio de "quitar opacity-50 y activar la card". El ADMIN no puede aprobar ni devolver informes (spec §6), lo cual se respeta porque las acciones de aprobación/devolución están en las colas de REVISOR y SUPERVISOR.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|PdfService|pdf.service|/api/pdf"
```

Resultado:

- `npm test -- --watch=false`: 53 specs, 0 fallas (46 anteriores + 4 nuevas de `ContratoDetalleComponent` + 3 nuevas de `AdminDashboardComponent`).
- `npm run build`: build success; `contrato-detalle-component` crece de 11.08 kB a 9.21 kB (lazy chunk).
- Auditoría de alcance frontend: 0 coincidencias.

## Proximo Punto De Retoma

Continuar con **Task 13: Verificación E2E + Documentación**.

Antes de avanzar:

1. Sincronizar: `git fetch origin && git pull --ff-only origin feat/sigcon-i2`.
2. Ejecutar `mvn test` en backend y verificar que todos los tests I1+I2 pasan.
3. Ejecutar `mvn clean package -DskipTests` y verificar que `sigcon-backend.war` se genera.
4. Ejecutar `npm test -- --watch=false` y `npm run build` en frontend.
5. Ejecutar auditoría de alcance (backend y frontend).
6. Actualizar `docs/ARRANQUE.md` con sección I2.
7. Cerrar el execution log con "Estado Final I2" y commit final.
