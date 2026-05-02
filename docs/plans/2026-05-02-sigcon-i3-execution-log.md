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
| Task 11 - Verificacion E2E + docs | ⏳ pending | — | — | — |

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

## Proximo Punto De Retoma

Continuar con **Task 11 — Verificacion E2E + Documentacion**.

Antes de avanzar:

1. Sincronizar: `git fetch origin && git pull --ff-only origin feat/sigcon-i3`.
2. Leer `docs/plans/2026-05-02-sigcon-i3-implementation-plan.md`, Task 11.
3. Ejecutar verificacion integral backend/frontend.
4. Revisar y actualizar `docs/ARRANQUE.md` y documentacion de estado si aplica.
5. Confirmar que I3 no incluye firma criptografica avanzada, SECOP, radicacion oficial externa ni motor de pagos.
6. Actualizar este log con resultados finales, commits y punto de retoma posterior a I3.

*Execution log creado 2026-05-02. Rama `feat/sigcon-i3` base commit `0658cef`.*
