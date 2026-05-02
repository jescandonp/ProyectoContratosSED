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
- Remoto: `origin/feat/sigcon-i2` (push pendiente al primer commit).
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
| Task 4 - Backend DTOs/mappers/services CRUD | ⏳ pending | — | — | Disponible para el siguiente modelo |
| Task 5 - State machine `InformeEstadoService` | ⏳ pending | — | — | — |
| Task 6 - Backend controllers/security/swagger | ⏳ pending | — | — | — |
| Task 7 - Frontend models + services | ⏳ pending | — | — | — |
| Task 8 - Informe form + detalle + preview | ⏳ pending | — | — | — |
| Task 9 - Corregir informe devuelto | ⏳ pending | — | — | — |
| Task 10 - Cola de revision (REVISOR) | ⏳ pending | — | — | — |
| Task 11 - Cola de aprobacion (SUPERVISOR) | ⏳ pending | — | — | — |
| Task 12 - Activar placeholders I1 | ⏳ pending | — | — | — |
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

- Antes de Task 4, verificar firma actual de `LocalDocumentStorageService.save(...)`. Si no acepta subdir, extender interfaz y adaptar consumidor de firma de I1 antes de tocar soportes.
- `db/00_setup.sql` se mantiene como archivo unico (PRD), pero las inserciones I2 van bajo cabecera explicita `-- ===== INCREMENTO 2 =====`.
- En Task 12 hay una decision de scope: ADMIN read-only de informes vs. "proximamente". Default sugerido: "proximamente" para no inflar I2. Quien tome Task 12 debe documentar la decision aqui.
- Codes de error nuevos (`INFORME_NO_ENCONTRADO`, `INFORME_NO_EDITABLE`, `TRANSICION_INVALIDA`, `OBSERVACION_REQUERIDA`, `ACTIVIDAD_REQUERIDA`, `PORCENTAJE_INVALIDO`, `SOPORTE_INVALIDO`, `DOCUMENTO_ADICIONAL_REQUERIDO`, `CONTRATO_NO_ACTIVO`) se agregan en `web.exception.ErrorCodes` (ubicacion existente desde I1 Task 5).
- En `feat/sigcon-i1` aun no hay merge a `main`. I2 trabaja directamente sobre `feat/sigcon-i2` partiendo de `feat/sigcon-i1`. La estrategia de merge la decide el modelo que cierre I3 o el integrador final.

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

## Proximo Punto De Retoma

Continuar con **Task 4: Backend Application Layer (DTOs, Mappers, Services CRUD Without Transitions)**.

Antes de avanzar:

1. Sincronizar: `git fetch origin && git pull --ff-only origin feat/sigcon-i2`.
2. Leer `docs/plans/2026-05-01-sigcon-i2-implementation-plan.md`, Task 4.
3. Leer `docs/specs/2026-05-01-sigcon-i2-spec.md` §4.4, §4.5, §4.7.
4. Reusar patron de I1: `application/dto/contrato/ContratoDetalleDto.java`, `application/mapper/ContratoMapper.java`, `application/service/ContratoService.java`.
5. Verificar firma actual de `LocalDocumentStorageService.save(...)`. Si no acepta subdirectorio (esperable en I1), extender el contrato `DocumentStorageService` con `save(byte[], String filename, String subdir)` o similar y migrar el unico consumidor (firma) sin romper su test.
6. Crear DTOs (11 archivos), mappers (5 archivos), servicios CRUD (5 archivos) — **sin** transiciones de estado, eso queda para Task 5.
7. Agregar codigos de error nuevos a `web/exception/ErrorCodes` (o equivalente existente desde I1 Task 5): `INFORME_NO_ENCONTRADO`, `INFORME_NO_EDITABLE`, `OBSERVACION_REQUERIDA`, `ACTIVIDAD_REQUERIDA`, `PORCENTAJE_INVALIDO`, `SOPORTE_INVALIDO`, `DOCUMENTO_ADICIONAL_REQUERIDO`, `CONTRATO_NO_ACTIVO`. (`TRANSICION_INVALIDA` se introduce en Task 5.)
8. Tests Mockito para cada servicio cubriendo: pertenencia contratista, ACCESO_DENEGADO en contrato ajeno, soporte URL valido `http(s)://`, soporte ARCHIVO invoca `DocumentStorageService` con subdir.
9. Validar con `mvn test -Dtest=*ServiceTest` y `mvn test`.
10. Registrar en este log archivos tocados, validaciones y commit.
