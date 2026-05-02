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
- Remoto: `origin/feat/sigcon-i3` (push pendiente tras primer commit).
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
| Task 1 - Plan + execution log baseline | ✅ done | Claude | `(ver abajo)` | Plan promovido desde outline; ajustes I2 incorporados; decisiones tecnicas resueltas |
| Task 2 - Oracle DDL I3 | ⏳ pending | — | — | — |
| Task 3 - Backend domain I3 | ⏳ pending | — | — | — |
| Task 4 - Backend PDF service | ⏳ pending | — | — | — |
| Task 5 - Backend notificaciones + email | ⏳ pending | — | — | — |
| Task 6 - Integracion estado + auditorProvider | ⏳ pending | — | — | — |
| Task 7 - Backend controllers + security + Swagger | ⏳ pending | — | — | — |
| Task 8 - Frontend models + services | ⏳ pending | — | — | — |
| Task 9 - Frontend campana + centro notificaciones | ⏳ pending | — | — | — |
| Task 10 - Frontend visor PDF + advertencia firma | ⏳ pending | — | — | — |
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

*Execution log creado 2026-05-02. Rama `feat/sigcon-i3` base commit `0658cef`.*
