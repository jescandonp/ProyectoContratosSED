# SIGCON I3 Execution Log

> Documento de handoff compartido para trabajo alternado entre Codex, Claude y otros modelos.  
> Fuente rectora: `docs/plans/2026-05-02-sigcon-i3-implementation-plan.md`.  
> Spec rectora: `docs/specs/2026-05-01-sigcon-i3-spec.md`.

## Reglas De Coordinacion Multi-Modelo

1. **Sincronizar antes de iniciar:** `git fetch origin && git pull --ff-only origin feat/sigcon-i3`.
2. **Una tarea activa por modelo.** Verificar la tabla "Estado De Tareas I3" antes de tomar una task; si alguna esta en `🔄 in progress`, esperar a que cierre o reasignar.
3. **Actualizar este log al cerrar cada task** con commit SHA, archivos tocados, validaciones ejecutadas y desviaciones respecto al plan.
4. **Inconsistencias spec/plan**: parchear plan o spec en la misma task y dejar nota en el log; nunca silenciarlas en codigo.
5. **Saltos o reordenamientos**: documentar el por que en el log.
6. **Bloqueos**: marcar la task como `⏸ blocked` con razon y "que me desbloquearia".

## Estado De Rama

- Rama activa: `feat/sigcon-i3`
- Punto de partida: `feat/sigcon-i2` HEAD `0658cef` (I2 completo: 64 backend + 53 frontend tests).
- Remoto: `origin/feat/sigcon-i3`; sincronizar antes de retomar.
- Cambio local no versionado conocido: `.claude/` fuera de Git por configuracion local.

## Ajustes Plan I3 Tras Cierre I2

- `DocumentStorageService.storeFile(subdir, file)` ya existe — Task 4 puede usarlo directamente para PDFs.
- `auditorProvider` retorna `"SYSTEM"` — se corrige en Task 6 como prerequisito funcional de I3.
- `SigconBackendSecurityTest` necesita `@MockBean NotificacionRepository` — Task 7 lo agrega.
- El test `informesAreExposedInI2ButFutureNotificationsAreNot` espera `isNotFound()` en `/api/notificaciones`. En I3 ese endpoint existe; Task 7 actualiza el test.
- `DocumentStorageService.loadFile(String path)` puede no existir — Task 7 agrega si falta.
- npm global roto: usar `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" ...`.
- Maven corre con JDK 21 y target/source 1.8. Falta validar JDK 8 real antes de produccion (no bloquea I3).
- PDF_HASH: usar SHA-256 (`MessageDigest.getInstance("SHA-256")`), sin dependencia externa.
- Email simulado en local-dev: `sigcon.mail.enabled=false` — nunca fallar un test por conexion real a Graph.

## Estado De Tareas I3

| Task | Estado | Modelo | Commit | Notas |
|---|---|---|---|---|
| Task 1 - Plan + execution log baseline | ✅ done | Claude | `7c9ff1c` | Plan promovido desde outline; ajustes I2 incorporados; decisiones tecnicas resueltas |
| Task 2 - Oracle DDL I3 | ✅ done | Claude | `5784e17` | DDL notificaciones + metadatos PDF |
| Task 3 - Backend domain I3 | ✅ done | Claude | `65d8ac6` | `Notificacion`, `TipoEvento`, repository y campos PDF |
| Task 4 - Backend PDF service | ✅ done | Claude | `ff59aff` | Template/service PDF I3 |
| Task 5 - Backend notificaciones + email | ✅ done | Claude | `8da44dd` | Services, DTOs, mapper y mail local-dev |
| Task 6 - Integracion estado + auditorProvider | ✅ done | Claude | `de63e28` | PDF/eventos en `InformeEstadoService`; auditorProvider corregido |
| Task 7 - Backend controllers + security + Swagger | ✅ done | Codex | `875ff45` | Controllers PDF/notificaciones; RBAC y endpoints ajustados a spec |
| Task 8 - Frontend models + services | ✅ done | Codex | `8608227` | Modelos/servicios Angular PDF + notificaciones; specs unitarios |
| Task 9 - Frontend campana + centro notificaciones | ✅ done | Codex | `e6d4d4a` | Campana, overlay, centro paginado y ruta `/notificaciones` |
| Task 10 - Frontend visor PDF + advertencia firma | ✅ done | Codex | `6a717a6` | Visor PDF, ruta, accion desde detalle, advertencia firma y DTO PDF backend |
| Task 11 - Verificacion E2E + docs | ✅ done | Codex | `2438de7`, `f2ed686` | Verificacion integral, seam auditorProvider, specs >=70 y ARRANQUE/README I3 |

Leyenda: ⏳ pending, 🔄 in progress, ✅ done, ⏸ blocked, ⚠ deviation.

## Restricciones De Alcance I3

No se debe implementar:

- Firma digital criptografica avanzada (PKCS#11, CAdES, XAdES).
- Radicacion oficial externa.
- Motor de pagos.
- Integracion SECOP2.
- Nuevos tipos contractuales.
- Nuevas transiciones de estado de informe.

Validacion de alcance al cerrar cada task con codigo nuevo:

```powershell
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|radicacion|PKcs11|firma.criptografica"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos"
```

## Decisiones Y Notas Para El Siguiente Modelo

- Task 4 usa `OpenPDF 1.3.x` + `Flying Saucer 9.1.22` (ver plan §Decisiones Tecnicas).
- `PdfInformeService.generarYPersistir()` es el unico punto que escribe `pdfRuta`, `pdfGeneradoAt`, `pdfHash`. Ningun otro servicio debe escribir esos campos.
- El PDF es inmutable: si `pdfRuta != null`, no se regenera aunque se llame al servicio. Esta regla es de negocio, no de infra.
- `EventoInformeService.publicar()` no lanza excepciones chequeadas hacia arriba — captura internamente los errores de email y los loguea.
- `InformeEstadoService.aprobar()` falla ANTES de persistir si el PDF falla. Si el PDF tuvo exito y luego la notificacion falla, la aprobacion ya se guardo y es valida.
- Notificaciones: el campo `leida` es `boolean` en Java / `NUMBER(1)` en Oracle. Mapear con `@Column(name="LEIDA")` y usar `0/1`.
- `fecha` de `Notificacion` es `insertable=false, updatable=false` igual que `Informe.fechaCreacion`.
- Email en `local-dev`: al capturar el log, el formato debe incluir tipo evento, idInforme y destinatario para facilitar debugging sin BD Oracle.

## Task 1 Implementado

Archivos creados:

- `docs/plans/2026-05-02-sigcon-i3-implementation-plan.md` — plan ejecutable promovido desde outline; 11 tareas con specs detalladas, dependencias, validaciones y criterios de cierre.
- `docs/plans/2026-05-02-sigcon-i3-execution-log.md` — este archivo.

Decisiones incorporadas al plan:
- PDF: OpenPDF 1.3.x + Flying Saucer 9.1.22 (LGPL, Java 8 + WebLogic 12 OK).
- Hash: SHA-256.
- Email: Microsoft Graph API; local-dev simulado via log.
- PDF storage: subdirectorio `pdfs/{idContrato}/{idInforme}/` en `DocumentStorageService`.
- `auditorProvider`: se corrige en Task 6, no en Task 1.

Validaciones:
```powershell
Test-Path docs/plans/2026-05-02-sigcon-i3-implementation-plan.md  # True
Test-Path docs/plans/2026-05-02-sigcon-i3-execution-log.md         # True
git branch --show-current                                           # feat/sigcon-i3
```

Proximo Punto De Retoma (para siguiente modelo):

> Tomar **Task 2** (Oracle DDL I3). Agregar cabecera `===== INCREMENTO 3 =====` a `db/00_setup.sql` con el `ALTER TABLE` de `SGCN_INFORMES` y `CREATE TABLE SGCN_NOTIFICACIONES` exactos del plan. Agregar seed de prueba a `db/01_datos_prueba.sql`. Commitear y actualizar este log con SHA y resultado de las validaciones del plan Task 2.

---

## Checkpoint Tras Trabajo Paralelo Tasks 2-6

Commits detectados en `feat/sigcon-i3` antes de tomar Task 7:

- `5784e17 feat: add SIGCON I3 Oracle DDL -- notifications table and PDF metadata`
- `65d8ac6 feat: add SIGCON I3 backend domain -- Notificacion entity, TipoEvento enum, repository`
- `ff59aff feat: add SIGCON I3 PDF service -- InformePdfTemplateService + PdfInformeService`
- `8da44dd feat: add SIGCON I3 notification and email services`
- `de63e28 feat(i3): integrate PDF + eventos into InformeEstadoService; fix auditorProvider`

Notas de checkpoint:

- `README.md` y `docs/ARRANQUE.md` aun describen I2 como estado activo/cerrado. No se corrige en Task 7 para no mezclar documentacion general; queda como ajuste de cierre I3 o Task 11.
- Se detecto incompatibilidad Java 8 heredada en `EmailNotificacionService` por uso de `Map.of`; se corrigio en Task 7 porque afecta WebLogic/JDK 8.

## Task 7 Implementado

Commit funcional:

- `875ff45 feat: add SIGCON I3 PDF and notification controllers`

Archivos creados:

- `web/controller/InformePdfController.java`
- `web/controller/NotificacionController.java`

Archivos modificados:

- `config/SecurityConfig.java`
- `config/DevSecurityConfig.java`
- `application/service/EmailNotificacionService.java`
- `application/service/PdfInformeService.java`
- `test/java/.../web/InformeSecurityTest.java`
- `test/java/.../web/SigconBackendSecurityTest.java`

Implementado y ajustado:

- `GET /api/informes/{id}/pdf` con `@Tag(name = "PDF")`, `Content-Type: application/pdf` y `Content-Disposition: attachment`.
- PDF restringido a `CONTRATISTA`, `SUPERVISOR`, `ADMIN`; `REVISOR` queda bloqueado segun spec I3.
- `GET /api/notificaciones`.
- `GET /api/notificaciones/no-leidas/count`.
- `PATCH /api/notificaciones/{id}/leida`.
- `PATCH /api/notificaciones/leidas`.
- Seguridad local-dev/weblogic explicita para `/api/informes/*/pdf` y `/api/notificaciones/**`.
- Tests de seguridad actualizados para exponer notificaciones en I3 y validar que PDF no permite `REVISOR`.
- Compatibilidad Java 8: `EmailNotificacionService` ya no usa `Map.of`; auditoria queda limpia.

Validaciones:

```powershell
cd sigcon-backend
mvn test "-Dtest=InformeSecurityTest,SigconBackendSecurityTest"
mvn test
mvn package -DskipTests
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "\bvar\b|List\.of|Map\.of|Set\.of|Optional\.orElseThrow\(\)|InputStream\.readAllBytes"
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|radicacion|PKcs11|firma.criptografica"
```

Resultado:

- TDD RED inicial: `REVISOR` alcanzaba el controller PDF y `/api/notificaciones/no-leidas/count` devolvia 404.
- `InformeSecurityTest,SigconBackendSecurityTest`: 18 tests, 0 fallas, 0 errores.
- `mvn test`: 91 tests, 0 fallas, 0 errores.
- `mvn package -DskipTests`: build success; WAR generado en `sigcon-backend/target/sigcon-backend.war`.
- Auditoria Java 8: 0 coincidencias.
- Auditoria de alcance I3: 0 coincidencias para SECOP, motor de pagos, radicacion, PKCS#11 o firma criptografica.

## Task 8 Implementado

Commit funcional:

- `8608227 feat: add SIGCON I3 frontend PDF and notification services`

Archivos creados:

- `sigcon-angular/src/app/core/models/notificacion.model.ts`
- `sigcon-angular/src/app/core/services/notificacion.service.ts`
- `sigcon-angular/src/app/core/services/notificacion.service.spec.ts`
- `sigcon-angular/src/app/core/services/pdf-informe.service.ts`
- `sigcon-angular/src/app/core/services/pdf-informe.service.spec.ts`

Archivos modificados:

- `sigcon-angular/src/app/core/models/informe.model.ts`

Implementado:

- Modelo `Notificacion` y union type `TipoEventoNotificacion` alineados con eventos I3.
- Servicio `NotificacionService` con:
  - `GET /api/notificaciones`
  - `GET /api/notificaciones/no-leidas/count`
  - `PATCH /api/notificaciones/{id}/leida`
  - `PATCH /api/notificaciones/leidas`
  - polling local `pollNoLeidas()`.
- Servicio `PdfInformeService.descargar(idInforme)` contra `GET /api/informes/{id}/pdf` con `responseType: 'blob'`.
- Campos PDF opcionales en `InformeResumen`: `pdfRuta`, `pdfGeneradoAt`, `pdfHash`.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos|PKCS|CAdES|XAdES|regenerarPdf|regenerar-pdf"
```

Resultado:

- TDD RED inicial: specs fallaron porque no existian modelo/servicios I3.
- `ng test`: 57 specs, 0 fallas.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.
- Auditoria de alcance frontend: 0 coincidencias para SECOP, motor de pagos, PKCS, CAdES, XAdES o regeneracion de PDF.

Notas:

- `sigcon-angular/angular.json` mantiene un cambio local no relacionado (`cli.analytics`) y no fue incluido en este commit.
- `.claude/` permanece como carpeta local no versionada conocida.
- Pendiente tecnico detectado para siguientes tasks/cierre: confirmar si los DTO backend de informe deben exponer `pdfRuta`, `pdfGeneradoAt` y `pdfHash` para que la UI pueda mostrar estado PDF sin llamada adicional.

## Task 9 Implementado

Commit funcional:

- `e6d4d4a feat: add SIGCON I3 notification center UI`

Archivos creados:

- `sigcon-angular/src/app/features/notificaciones/notificaciones-menu/notificaciones-menu.component.ts`
- `sigcon-angular/src/app/features/notificaciones/notificaciones-menu/notificaciones-menu.component.spec.ts`
- `sigcon-angular/src/app/features/notificaciones/centro-notificaciones/centro-notificaciones.component.ts`
- `sigcon-angular/src/app/features/notificaciones/centro-notificaciones/centro-notificaciones.component.spec.ts`

Archivos modificados:

- `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`
- `sigcon-angular/src/app/app.routes.ts`
- `sigcon-angular/src/app/app.routes.spec.ts`

Implementado:

- Campana de notificaciones en topbar con icono `pi pi-bell`.
- Badge oculto cuando `count = 0` y visible cuando hay no leidas.
- Overlay con ultimas 5 notificaciones, enlace a centro y marcado como leida al hacer click.
- Centro de notificaciones en `/notificaciones` con lista paginada, icono por tipo, fecha relativa y estado leida/no leida.
- Click en fila marca como leida y navega a `/informes/{idInforme}` cuando aplica.
- Accion "Marcar todas como leidas".

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos|PKCS|CAdES|XAdES|regenerarPdf|regenerar-pdf"
```

Resultado:

- TDD RED inicial: specs fallaron porque no existian los componentes `NotificacionesMenuComponent` y `CentroNotificacionesComponent`.
- `ng test`: 61 specs, 0 fallas.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.
- Auditoria de alcance frontend: 0 coincidencias para SECOP, motor de pagos, PKCS, CAdES, XAdES o regeneracion de PDF.

## Task 10 Implementado

Commit funcional:

- `6a717a6 feat: add SIGCON I3 PDF viewer and signature warning`

Archivos creados:

- `sigcon-angular/src/app/features/informes/visor-pdf/visor-pdf.component.ts`
- `sigcon-angular/src/app/features/informes/visor-pdf/visor-pdf.component.spec.ts`
- `sigcon-angular/src/app/features/perfil/perfil.component.spec.ts`

Archivos modificados:

- `sigcon-angular/src/app/app.routes.ts`
- `sigcon-angular/src/app/app.routes.spec.ts`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `sigcon-angular/src/app/features/perfil/perfil.component.ts`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/informe/InformeResumenDto.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/InformeMapper.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`

Implementado:

- Ruta `/informes/:id/pdf`.
- `VisorPdfComponent` carga metadatos con `InformeService.obtenerDetalle(id)` y PDF con `PdfInformeService.descargar(id)`.
- Boton "Descargar PDF" disponible solo cuando el Blob se obtuvo correctamente.
- Mensaje institucional cuando el PDF no esta disponible: "El PDF no esta disponible. El informe debe estar en estado APROBADO."
- Boton "Ver / Descargar PDF" en detalle solo cuando `estado === 'APROBADO'` y existe `pdfRuta`.
- Advertencia de firma faltante en perfil para CONTRATISTA/SUPERVISOR sin `firmaImagen`.
- DTO/mapping backend expone `pdfRuta`, `pdfGeneradoAt` y `pdfHash` para que la UI pueda detectar PDF generado.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build

cd sigcon-backend
mvn test -Dtest=InformeServiceTest
mvn test

Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|radicacion|PKcs11|firma.criptografica|PKCS|CAdES|XAdES"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos|PKCS|CAdES|XAdES|regenerarPdf|regenerar-pdf"
```

Resultado:

- TDD RED inicial: specs fallaron porque no existia `VisorPdfComponent`.
- `ng test`: 66 specs, 0 fallas.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.
- `InformeServiceTest`: 10 tests, 0 fallas.
- `mvn test`: 92 tests, 0 fallas.
- Auditoria backend/frontend de alcance I3: 0 coincidencias.
- Ruido no bloqueante: un spec de perfil genera 404 de imagen simulada `/api/storage/firmas/ana.png`, sin fallo de test.

## Task 11 Implementado

Commits:

- `2438de7 fix: close SIGCON I3 verification seams`
- `f2ed686 docs: update SIGCON I3 startup guidance`

Archivos modificados:

- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/JpaAuditingConfig.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/config/JpaAuditingConfigTest.java`
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `sigcon-angular/src/app/features/informes/visor-pdf/visor-pdf.component.spec.ts`
- `sigcon-angular/src/app/features/notificaciones/centro-notificaciones/centro-notificaciones.component.ts`
- `sigcon-angular/src/app/features/notificaciones/centro-notificaciones/centro-notificaciones.component.spec.ts`
- `sigcon-angular/src/app/features/notificaciones/notificaciones-menu/notificaciones-menu.component.ts`
- `sigcon-angular/src/app/features/notificaciones/notificaciones-menu/notificaciones-menu.component.spec.ts`
- `docs/ARRANQUE.md`
- `README.md`

Implementado:

- Se agregaron 4 specs frontend reales para cumplir el umbral de cierre I3: 70 specs.
- Se corrigio `JpaAuditingConfig` para eliminar fallback `"SYSTEM"` y usar `AUDITOR_NO_AUTENTICADO` en contextos anonimos/no autenticados.
- Se agrego `JpaAuditingConfigTest` para fijar principal autenticado y fallback anonimo.
- `docs/ARRANQUE.md` actualizado a estado I3 completado, alcance I3, DDL I1+I2+I3 y configuracion PDF/email.
- `README.md` actualizado para apuntar a `feat/sigcon-i3` y execution log I3.

Validaciones:

```powershell
cd sigcon-backend
mvn test
mvn package -DskipTests
Get-ChildItem -Path sigcon-backend\target -Filter *.war | Select-Object Name,Length,LastWriteTime
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern '"SYSTEM"' | Where-Object { $_.Line -notmatch '//|/\*' }
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|radicacion|PKcs11|firma.criptografica|PKCS|CAdES|XAdES"

cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos|PKCS|CAdES|XAdES|regenerarPdf|regenerar-pdf"
Select-String -Path sigcon-angular\src\app\app.routes.ts -Pattern "path:" | Format-Table
```

Resultado:

- `mvn test`: 94 tests, 0 fallas, 0 errores.
- `mvn package -DskipTests`: build success; WAR generado en `sigcon-backend/target/sigcon-backend.war`.
- WAR: `sigcon-backend.war`, 57,173,672 bytes, generado 2026-05-02 17:01.
- Auditoria `"SYSTEM"` runtime en backend main: 0 coincidencias.
- Auditoria backend de alcance I3: 0 coincidencias para SECOP, motor de pagos, radicacion, PKCS, CAdES, XAdES.
- `ng test`: 70 specs, 0 fallas.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.
- Auditoria frontend de alcance I3: 0 coincidencias para SECOP, motor de pagos, PKCS, CAdES, XAdES o regeneracion de PDF.
- Rutas frontend verificadas: 20 rutas incluyendo `/informes/:id/pdf` y `/notificaciones`; sin rutas futuras no planeadas.

Notas:

- `sigcon-angular/angular.json` mantiene un cambio local no relacionado (`cli.analytics`) y no fue incluido.
- `.claude/` permanece como carpeta local no versionada conocida.
- Ruido no bloqueante: specs backend de resiliencia imprimen errores esperados en logs; specs frontend de perfil generan 404 de imagen simulada `/api/storage/firmas/ana.png`, sin fallo.

## Hardening Post-Review I3 Implementado

Motivo:

- Revision integral detecto 3 hallazgos antes de considerar I3 cerrado para continuidad multi-modelo:
  - PDF descargable por rol sin validar asignacion del contrato.
  - Notificaciones in-app podian marcar rollback de la transaccion principal pese al `catch`.
  - PDF institucional podia generarse con `Fecha de aprobacion: N/A`.

Archivos modificados:

- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformePdfController.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/NotificacionService.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeEstadoService.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/InformeSecurityTest.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeEstadoServiceI3Test.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/NotificacionServiceTest.java`

Implementado:

- `InformeService.obtenerInformeAutorizado(id)` reutiliza `CurrentUserService` + `assertCanViewInforme` para devolver la entidad solo si el usuario esta asignado o es admin.
- `InformePdfController` descarga PDF usando `obtenerInformeAutorizado(id)` en vez de `findActiveInforme(id)`.
- `NotificacionService.crear(...)` usa `@Transactional(propagation = REQUIRES_NEW)` para aislar fallos de notificacion del flujo de aprobacion.
- `InformeEstadoService.aprobar(...)` asigna `fechaAprobacion` antes de `pdfInformeService.generarYPersistir(...)`, y la limpia si la generacion PDF falla antes de persistir el estado.

Pruebas agregadas:

- `InformeSecurityTest.contractorCannotDownloadPdfWhenInformeIsNotAssigned`
- `InformeEstadoServiceI3Test.aprobarAsignaFechaAprobacionAntesDeGenerarPdf`
- `NotificacionServiceTest.crearUsaTransaccionIndependienteParaNoMarcarRollbackDelFlujoPrincipal`

Validaciones:

```powershell
cd sigcon-backend
mvn test "-Dtest=InformeSecurityTest,InformeEstadoServiceI3Test,NotificacionServiceTest"
mvn test
mvn package -DskipTests

cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Resultado:

- Validacion backend acotada: 23 tests, 0 fallas, 0 errores.
- `mvn test`: 97 tests, 0 fallas, 0 errores.
- `mvn package -DskipTests`: build success; WAR generado en `sigcon-backend/target/sigcon-backend.war`.
- WAR: `sigcon-backend.war`, 57,173,927 bytes, generado 2026-05-02 17:21.
- `ng test`: 70 specs, 0 fallas.
- Ruido no bloqueante esperado: logs de errores simulados en tests de resiliencia backend y 404 de imagen simulada `/api/storage/firmas/ana.png` en frontend.

## Cierre Hallazgos Finales I1-I3

Motivo:

- Revision final cruzada I1-I3 detecto 3 hallazgos remanentes de cierre antes de continuidad:
  - OAuth2 token request para Microsoft Graph armaba `application/x-www-form-urlencoded` por concatenacion manual.
  - Perfil `weblogic` no declaraba configuracion `sigcon.mail.*`, dejando riesgo de email simulado por default.
  - Seeds I3 de notificaciones quedaban despues del unico `COMMIT` del script local-dev.

Archivos modificados:

- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/EmailNotificacionService.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/service/EmailNotificacionServiceTest.java`
- `sigcon-backend/src/main/resources/application.yml`
- `db/01_datos_prueba.sql`
- `docs/ARRANQUE.md`

Implementado:

- `EmailNotificacionService.obtenerToken()` usa `LinkedMultiValueMap` para que `RestTemplate` serialice y codifique correctamente el form OAuth2.
- Nueva prueba `EmailNotificacionServiceTest.obtenerTokenEnviaFormUrlEncodedSeguroParaSecretosConCaracteresReservados` cubre `client_id` y `client_secret` con caracteres reservados.
- Perfil `weblogic` declara `sigcon.mail.enabled=${SIGCON_MAIL_ENABLED:true}`, `MAIL_FROM`, `GRAPH_API_BASE_URL`, `AZURE_TENANT_ID`, `MAIL_CLIENT_ID` y `MAIL_CLIENT_SECRET`.
- `docs/ARRANQUE.md` queda alineado con las variables productivas de correo I3.
- `db/01_datos_prueba.sql` agrega `COMMIT` final para persistir seeds I3 en clientes Oracle con autocommit desactivado.

Validaciones:

```powershell
cd sigcon-backend
mvn test "-Dtest=EmailNotificacionServiceTest"
mvn test
mvn package -DskipTests

cd ..
Get-Content sigcon-backend/src/main/resources/application.yml | Select-String -Pattern 'SIGCON_MAIL_ENABLED','GRAPH_API_BASE_URL','MAIL_CLIENT_SECRET' -Context 2,2
Select-String -Path db/01_datos_prueba.sql -Pattern '^COMMIT;'
Get-Item sigcon-backend/target/sigcon-backend.war | Select-Object Name,Length,LastWriteTime
```

Resultado:

- Prueba roja inicial confirmo el defecto: body sin encoding (`client_id=client+id&client_secret=a+b&c=d`).
- Prueba acotada despues del fix: 1 test, 0 fallas, 0 errores.
- `mvn test`: 98 tests, 0 fallas, 0 errores.
- `mvn package -DskipTests`: build success; WAR generado en `sigcon-backend/target/sigcon-backend.war`.
- WAR: `sigcon-backend.war`, 57,174,107 bytes, generado 2026-05-03 11:52.
- `application.yml` contiene configuracion mail explicita para perfil `weblogic`.
- `db/01_datos_prueba.sql` contiene `COMMIT` en lineas 82 y 97; el segundo confirma seeds I3.

## Ajuste UX Durante Pruebas Funcionales I1

Motivo:

- Durante pruebas manuales del paso 1 de administracion, la pantalla `Nuevo usuario` mostraba solo `Error al guardar el usuario.`
- Desde esta sesion no habia backend escuchando en `localhost:8080`, por lo que el caso de fallo de conexion/proxy quedaba indistinguible de validacion, permisos o duplicado.

Archivos modificados:

- `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.ts`
- `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.spec.ts`

Implementado:

- `AdminUsuariosComponent` ahora distingue errores de guardado:
  - `status === 0`: backend/proxy no disponible en `localhost:8080`.
  - `401/403`: sesion sin permisos admin o expirada.
  - `EMAIL_DUPLICADO`: email ya registrado.
  - `VALIDACION_FALLIDA`: mensaje de validacion del backend.
  - Fallback: mensaje backend si existe o error generico.
- Se agrego prueba para el caso `status === 0` usando el mismo payload del flujo manual reportado.

Validaciones:

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless --include src/app/features/admin/usuarios/admin-usuarios.component.spec.ts
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Resultado:

- Prueba roja inicial confirmo el defecto: para `status: 0` mostraba `Error al guardar el usuario.`
- Prueba acotada despues del fix: 1 test, 0 fallas.
- `ng test`: 71 specs, 0 fallas.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.

## Proximo Punto De Retoma

Incremento 3 queda cerrado metodologicamente despues del hardening post-review y cierre de hallazgos finales. Antes de iniciar cualquier implementacion futura:

1. Sincronizar: `git fetch origin && git pull --ff-only origin feat/sigcon-i3`.
2. Revisar si el siguiente paso es una revision final cruzada I1-I3 o iniciar una spec nueva.
3. Si se inicia un incremento nuevo, crear primero spec/plan/execution log bajo `docs/specs` y `docs/plans`.
4. Mantener fuera de SIGCON actual: firma criptografica avanzada, SECOP2, radicacion oficial externa y motor de pagos hasta que exista spec/plan aprobado.

*Execution log creado 2026-05-02. Rama `feat/sigcon-i3` base commit `0658cef`.*

## Ajuste Durante Pruebas Funcionales De Contratos

Motivo:

- En prueba manual se creo el contrato `CO1.PCCNTR 8504408 - 2025` sin revisor.
- La creacion persistio en Oracle, pero la UI no mostro confirmacion.
- El listado de contratos no cargaba porque el usuario local-dev autenticado `admin@educacionbogota.edu.co` no existia como usuario activo en `SGCN_USUARIOS`; el frontend ocultaba ese error.

Evidencia local:

```sql
select c.ID, c.NUMERO, c.ESTADO, c.ACTIVO, c.ID_CONTRATISTA, c.ID_REVISOR, c.ID_SUPERVISOR
from SGCN_CONTRATOS c
where c.NUMERO = 'CO1.PCCNTR 8504408 - 2025';
```

Resultado observado:

- Contrato activo existe.
- `ID_REVISOR` quedo nulo.
- `GET /api/contratos?page=0&size=15` con basic auth local-dev devolvia `USUARIO_NO_ENCONTRADO`.

Implementado:

- Backend: `ContratoRequest` exige `idRevisor` e `idSupervisor`.
- Backend: `ContratoService` valida que contratista, revisor y supervisor existan activos y correspondan a sus roles.
- Frontend: formulario de contrato marca Revisor y Supervisor como obligatorios.
- Frontend: despues de crear/editar contrato se muestra mensaje de confirmacion en el listado.
- Frontend: el listado muestra error explicito si la sesion local-dev no existe en `SGCN_USUARIOS`.
- DB local-dev: nuevo script `db/02_reparar_usuarios_local_dev.sql` repara de forma idempotente los cuatro usuarios esperados por `DevSecurityConfig` y `DevSessionService`.

Validaciones:

```powershell
cd sigcon-backend
mvn test -Dtest=ContratoServiceTest
mvn test

cd ../sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --browsers=ChromeHeadless

cd ..
sqlplus -S SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/02_reparar_usuarios_local_dev.sql
curl.exe -i -u "admin@educacionbogota.edu.co:admin123" "http://localhost:8080/api/contratos?page=0&size=15"
```

Resultado:

- `ContratoServiceTest`: 9 tests, 0 fallas, 0 errores.
- `mvn test`: 100 tests, 0 fallas, 0 errores.
- `ng build`: build success; salida en `sigcon-angular/dist/sigcon-angular`.
- `ng test`: 71 specs, 0 fallas.
- Script Oracle local-dev: 4 filas fusionadas, `COMMIT` confirmado.
- `GET /api/contratos?page=0&size=15` como Admin local-dev responde `200` y devuelve el contrato `CO1.PCCNTR 8504408 - 2025`.

Proximo punto de retoma:

- Refrescar la pantalla de contratos o reiniciar `ng serve` si estaba corriendo antes del cambio frontend.
- Reintentar crear un contrato nuevo con contratista, revisor y supervisor seleccionados.
- Revisar si el contrato `CO1.PCCNTR 8504408 - 2025` debe editarse para asignarle revisor antes de usarlo en flujos I2/I3.
