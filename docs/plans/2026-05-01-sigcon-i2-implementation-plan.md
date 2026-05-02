# SIGCON Incremento 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL — Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This plan supersedes `docs/plans/2026-05-01-sigcon-i2-implementation-outline.md` and was promoted from outline to plan after closing I1.

**Goal:** Build Incremento 2 of SIGCON: report management for contractors, review queue for reviewers, and final approval queue for supervisors, with a complete state machine `BORRADOR → ENVIADO → DEVUELTO → ENVIADO → EN_REVISION → APROBADO`. No PDF generation, no email, no in-app notifications.

**Architecture:** Extend the I1 backend (Spring Boot 2.7.18 WAR / Java 8) with new domain entities (`Informe`, `ActividadInforme`, `SoporteAdjunto`, `DocumentoAdicional`, `Observacion`), JPA repositories, services, REST controllers under `/api/informes`, `/api/actividades`, and a state-transition engine isolated in `InformeEstadoService`. Extend the Angular 20 + PrimeNG 20 frontend with new feature folders (`features/informes/...`, `features/revision/...`, `features/aprobacion/...`) reusing the existing `core/auth`, `core/services`, `shared/` shell, and design tokens. Reuse `DocumentStorageService` (introduced in I1) for soporte archivo storage.

**Tech Stack:** Same as I1 — Java 8, Spring Boot 2.7.18, Spring Security 5.7, Spring Data JPA, SpringDoc OpenAPI 1.7.0, Oracle JDBC ojdbc8, Maven WAR, Angular 20, PrimeNG 20, TypeScript 5.8, Tailwind CSS 3.4. **No new framework versions are introduced in I2.**

---

## Source Of Truth

- SDD governance: `docs/CONSTITUTION.md`
- Architecture constraints: `docs/ARCHITECTURE.md`
- Stack versions: `docs/TECNOLOGIAS.md`
- Product scope: `docs/specs/2026-04-30-sigcon-prd.md`
- I1 contract (already implemented): `docs/specs/2026-04-30-sigcon-i1-spec.md`
- **I2 technical contract:** `docs/specs/2026-05-01-sigcon-i2-spec.md`
- Forward compatibility: `docs/specs/2026-05-01-sigcon-i3-spec.md`
- Visual reference: `Prototipo/DESIGN.md`, `Prototipo/nuevo_informe_*`, `Prototipo/corregir_informe_*`, `Prototipo/cola_de_revisi_n_*`
- Strict I2 boundary: do not implement PDF generation, email/in-app notifications, or SECOP2 integration.

Authority order: `docs/CONSTITUTION.md` → `docs/ARCHITECTURE.md` → PRD → I1 spec → **I2 spec** → I3 forward-compatibility notes → this plan → code.

---

## Adjustments After I1 Closure

This plan is the outline promoted to executable form. The following adjustments come from observed I1 behavior:

1. **`UsuarioRequest` already includes `email`** (was changed during I1 Task 6). No I2 change needed; new endpoints can rely on `Authentication.getName()` returning a full email principal both in `local-dev` and `weblogic` profiles.
2. **`SigconBackendApplicationTests` excludes `application.service.*` and `web.controller.*`** because the smoke test does not load Oracle. I2 must extend that exclusion to cover any new packages where context-load tests would require the database; service/controller logic stays covered by unit + MockMvc tests as in I1.
3. **`JpaAuditingConfig` has an `auditorProvider` bean returning `"SYSTEM"`.** Until JWT/Basic principal extraction is wired in, all I2 audit columns will receive `"SYSTEM"`. Replacing the auditor with the real principal is **not** in I2 scope; document it as a known I3 prerequisite.
4. **`DocumentStorageService` interface + `LocalDocumentStorageService` implementation** already exist for firma. I2 reuses both for `SoporteAdjunto` archivo storage. No redesign required; only add a new save method or accept a target subdirectory parameter.
5. **`Page<T>` model now exposes `first` and `last`** (added in Task 9). New I2 paginated lists should rely on these fields directly.
6. **PrimeNG 20 (not 21).** No upgrade in I2.
7. **Frontend npm wrapper workaround** (`node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" ...`) remains. Document in execution log validation steps.
8. **Disabled placeholders to be activated:** `features/contratos/detalle/contrato-detalle.component.ts` (button "Nuevo Informe" with `disabled`, no handler) and `features/admin/dashboard/admin-dashboard.component.ts` (Informes card with `opacity-50`). I2 Task 12 enables both and wires routing.
9. **Maven runtime is Java 21**, target/source 1.8. WebLogic deploy validation under real JDK 8 is still pending; remains an I3-or-pre-prod concern, NOT blocking I2 close.
10. **Datos de prueba I1 already include OPS-2026-001** with obligaciones; reusable for I2 sample informe.

---

## Forward-Compatibility Gates From I3

I2 must leave these seams ready without implementing later increments:

- `Informe.pdfRuta` exists as nullable column and DTO field; I2 never populates it. Approved informes in I2 have `pdfRuta = null`.
- `Informe.fechaAprobacion` is set on `EN_REVISION → APROBADO`; I3 will read it for PDF metadata.
- The `EN_REVISION → APROBADO` transition is implemented in `InformeEstadoService.aprobar()` and exposes a single integration seam where I3 will inject PDF generation and notifications without altering states.
- Email of contratista, revisor, supervisor and approval timestamp must be reachable from `InformeDetalleDto` so I3 PDF can render them.
- The topbar visual reservation for a notifications icon (decided in I1) stays inert in I2.

---

## File Structure Additions

Create the following new files. **Do not move or rename I1 files.**

```text
ProyectoContratosSED/
├── db/
│   ├── 00_setup.sql                           # APPEND: I2 tables/sequences/triggers
│   └── 01_datos_prueba.sql                    # APPEND: I2 sample data
├── sigcon-backend/
│   └── src/main/java/co/gov/bogota/sed/sigcon/
│       ├── domain/
│       │   ├── entity/
│       │   │   ├── Informe.java               # NEW
│       │   │   ├── ActividadInforme.java      # NEW
│       │   │   ├── SoporteAdjunto.java        # NEW
│       │   │   ├── DocumentoAdicional.java    # NEW
│       │   │   └── Observacion.java           # NEW
│       │   ├── enums/
│       │   │   ├── EstadoInforme.java         # NEW
│       │   │   ├── TipoSoporte.java           # NEW
│       │   │   └── RolObservacion.java        # NEW
│       │   └── repository/
│       │       ├── InformeRepository.java
│       │       ├── ActividadInformeRepository.java
│       │       ├── SoporteAdjuntoRepository.java
│       │       ├── DocumentoAdicionalRepository.java
│       │       └── ObservacionRepository.java
│       ├── application/
│       │   ├── dto/informe/
│       │   │   ├── InformeResumenDto.java
│       │   │   ├── InformeDetalleDto.java
│       │   │   ├── InformeRequest.java
│       │   │   ├── ActividadInformeDto.java
│       │   │   ├── ActividadInformeRequest.java
│       │   │   ├── SoporteAdjuntoDto.java
│       │   │   ├── SoporteUrlRequest.java
│       │   │   ├── DocumentoAdicionalDto.java
│       │   │   ├── DocumentoAdicionalRequest.java
│       │   │   ├── ObservacionDto.java
│       │   │   └── ObservacionRequest.java
│       │   ├── mapper/
│       │   │   ├── InformeMapper.java
│       │   │   ├── ActividadInformeMapper.java
│       │   │   ├── SoporteAdjuntoMapper.java
│       │   │   ├── DocumentoAdicionalMapper.java
│       │   │   └── ObservacionMapper.java
│       │   └── service/
│       │       ├── InformeService.java
│       │       ├── InformeEstadoService.java       # state machine
│       │       ├── ActividadInformeService.java
│       │       ├── SoporteAdjuntoService.java
│       │       ├── DocumentoAdicionalInformeService.java
│       │       └── ObservacionService.java
│       └── web/controller/
│           ├── InformeController.java
│           ├── ActividadInformeController.java
│           ├── SoporteAdjuntoController.java
│           └── DocumentoAdicionalInformeController.java
└── sigcon-angular/src/app/
    ├── core/
    │   ├── models/
    │   │   ├── informe.model.ts
    │   │   ├── actividad-informe.model.ts
    │   │   ├── soporte-adjunto.model.ts
    │   │   ├── documento-adicional.model.ts
    │   │   └── observacion.model.ts
    │   └── services/
    │       ├── informe.service.ts
    │       ├── actividad-informe.service.ts
    │       ├── soporte-adjunto.service.ts
    │       ├── documento-adicional.service.ts
    │       └── observacion.service.ts
    └── features/
        ├── informes/
        │   ├── nuevo/informe-form.component.ts
        │   ├── detalle/informe-detalle.component.ts
        │   ├── corregir/corregir-informe.component.ts
        │   └── preview/informe-preview.component.ts
        ├── revision/
        │   └── cola-revision.component.ts
        └── aprobacion/
            └── cola-aprobacion.component.ts
```

Use one Git commit per task. Update `docs/plans/2026-05-01-sigcon-i2-execution-log.md` after each task to keep handoff state current for other models (Codex, Claude, etc.).

---

## Task 1: I2 Plan And Branch Baseline

**Files:**
- Create: `docs/plans/2026-05-01-sigcon-i2-implementation-plan.md` (this document)
- Create: `docs/plans/2026-05-01-sigcon-i2-execution-log.md`

- [ ] **Step 1: Confirm branch and starting point**

```powershell
git status --short
git rev-parse --abbrev-ref HEAD
git log --oneline -3
```

Expected: branch `feat/sigcon-i2` based on the last I1 commit (`be26bbe` or its descendant on `feat/sigcon-i1`).

- [ ] **Step 2: Verify I1 acceptance still holds**

```powershell
cd sigcon-backend ; mvn test
cd ..\sigcon-angular ; node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Expected: 21 backend tests, 10 frontend specs, 0 failures.

- [ ] **Step 3: Commit plan + execution log**

```powershell
git add docs/plans/2026-05-01-sigcon-i2-implementation-plan.md docs/plans/2026-05-01-sigcon-i2-execution-log.md
git commit -m "docs: promote SIGCON I2 outline to executable plan"
```

---

## Task 2: Oracle Schema Additions For I2

**Files:**
- Modify: `db/00_setup.sql` (append I2 section)
- Modify: `db/01_datos_prueba.sql` (append I2 sample data)

- [ ] **Step 1: Append `SGCN_INFORMES`, `SGCN_ACTIVIDADES`, `SGCN_SOPORTES`, `SGCN_DOCS_ADICIONALES`, `SGCN_OBSERVACIONES`** with sequences, indexes, and `TRG_*_AUDIT` triggers exactly as in I2 spec §3. Use a clearly marked section header `-- ===== INCREMENTO 2 =====` so the file remains traceable per increment.

- [ ] **Step 2: Append sample data**

In `db/01_datos_prueba.sql`, after I1 inserts:

- One `BORRADOR` informe for contrato `OPS-2026-001`, contratista `juan.escandon@educacionbogota.edu.co`.
- One actividad per obligacion of that contrato with porcentaje 0.
- One `URL` soporte and one `ARCHIVO` soporte placeholder.
- One `documento_adicional` linked to an OPS catalog item flagged `obligatorio = 1`.

- [ ] **Step 3: Static validation**

```powershell
Select-String -Path db/00_setup.sql -Pattern "SGCN_INFORMES|SGCN_ACTIVIDADES|SGCN_SOPORTES|SGCN_DOCS_ADICIONALES|SGCN_OBSERVACIONES|TRG_INFORMES_AUDIT"
Select-String -Path db/01_datos_prueba.sql -Pattern "SGCN_INFORMES|SGCN_ACTIVIDADES|BORRADOR"
```

Expected: every pattern returns at least one match; no I3 tables (`SGCN_NOTIFICACIONES`) appear.

- [ ] **Step 4: Commit**

```powershell
git add db/00_setup.sql db/01_datos_prueba.sql
git commit -m "feat: add SIGCON I2 Oracle schema and sample data"
```

---

## Task 3: Backend Domain Layer (Entities, Enums, Repositories)

**Files:**
- New: enums under `domain/enums/`
- New: entities under `domain/entity/`
- New: repositories under `domain/repository/`
- New: `sigcon-backend/src/test/java/.../domain/InformeDomainMappingTest.java`

- [ ] **Step 1: Create enums** `EstadoInforme`, `TipoSoporte`, `RolObservacion` mirroring I2 spec §4.1.

- [ ] **Step 2: Create entities** with `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`/`@LastModifiedDate`, soft delete via `activo` boolean, and JPA relations described in spec §4.2.

- [ ] **Step 3: Create repositories** matching the signatures in spec §4.3.

- [ ] **Step 4: Add a `DomainModelMappingTest`-style unit test** that verifies entity construction, equals/hashCode, and enum coverage. Pattern: see existing `co.gov.bogota.sed.sigcon.domain.DomainModelMappingTest`.

- [ ] **Step 5: Validate**

```powershell
cd sigcon-backend
mvn test -Dtest=*DomainMappingTest
mvn test -DskipTests
```

Expected: domain tests pass; build success.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-backend/src/main/java sigcon-backend/src/test/java
git commit -m "feat: add SIGCON I2 domain model and repositories"
```

---

## Task 4: Backend Application Layer (DTOs, Mappers, Services Without Transitions)

**Files:**
- New: DTOs under `application/dto/informe/`
- New: mappers under `application/mapper/`
- New: services `InformeService`, `ActividadInformeService`, `SoporteAdjuntoService`, `DocumentoAdicionalInformeService`, `ObservacionService`
- New: `*ServiceTest` for each service

- [x] **Step 1: Create all DTOs** per spec §4.4.

- [x] **Step 2: Create mappers** following `ContratoMapper` pattern (manual mapping, no MapStruct).

- [x] **Step 3: Implement services** for CRUD, listing, and pertenencia/role enforcement. **Do not implement state transitions yet** — that goes in Task 5.

- [x] **Step 4: Reuse `DocumentStorageService`** for `SoporteAdjuntoService.agregarSoporteArchivo()`. Add a method `storeFile(String subdir, MultipartFile file)` to the interface if not already present, and adapt `LocalDocumentStorageService` accordingly.

- [x] **Step 5: Tests** — Mockito-based unit tests covering:
- CONTRATISTA can list/create informes only on own active contracts.
- ACCESO_DENEGADO when contratista accesses informe of another contract.
- Soporte URL must be `http://` or `https://`; otherwise `SOPORTE_INVALIDO`.
- Soporte archivo invokes `DocumentStorageService` with the right subdir.

- [x] **Step 6: Validate**

```powershell
cd sigcon-backend
mvn test -Dtest=*ServiceTest
mvn test
```

Expected: all new tests pass; total test count grows monotonically.

- [x] **Step 7: Commit**

```powershell
git add sigcon-backend/src/main/java sigcon-backend/src/test/java
git commit -m "feat: add SIGCON I2 informe DTOs, mappers, and CRUD services"
```

---

## Task 5: State Machine — `InformeEstadoService`

**Files:**
- New: `application/service/InformeEstadoService.java`
- New: `application/service/InformeEstadoServiceTest.java`
- Modify: relevant services to delegate transitions to `InformeEstadoService`

- [ ] **Step 1: Implement transitions** as a single class with explicit guard methods:
- `enviar(informeId, contratistaEmail)`: `BORRADOR|DEVUELTO → ENVIADO`, requires ≥1 actividad, sets `fechaUltimoEnvio`.
- `aprobarRevision(informeId, revisorEmail, observacionOpcional)`: `ENVIADO → EN_REVISION`, only revisor asignado.
- `devolverEnRevision(informeId, revisorEmail, observacion)`: `ENVIADO → DEVUELTO`, observacion obligatoria.
- `aprobar(informeId, supervisorEmail)`: `EN_REVISION → APROBADO`, only supervisor asignado, sets `fechaAprobacion`. **Leaves `pdfRuta = null`** (I3 seam).
- `devolverFinal(informeId, supervisorEmail, observacion)`: `EN_REVISION → DEVUELTO`, observacion obligatoria.

- [ ] **Step 2: Error contract**:
- Invalid transition → `TRANSICION_INVALIDA`.
- Empty observacion when required → `OBSERVACION_REQUERIDA`.
- Empty actividades on `enviar` → `ACTIVIDAD_REQUERIDA`.
- Wrong role/principal → `ACCESO_DENEGADO` (reuse I1 code).
- Aprobado is terminal → `INFORME_NO_EDITABLE`.

- [ ] **Step 3: Tests** — Cover all 14 acceptance backend bullets in spec §7.

- [ ] **Step 4: Validate**

```powershell
cd sigcon-backend
mvn test -Dtest=InformeEstadoServiceTest
mvn test
```

- [ ] **Step 5: Commit**

```powershell
git add sigcon-backend/src/main/java sigcon-backend/src/test/java
git commit -m "feat: add SIGCON I2 informe state machine"
```

---

## Task 6: Backend REST Controllers And Security

**Files:**
- New: controllers per spec §4.6
- Modify: `DevSecurityConfig`, `SecurityConfig` to expose `/api/informes/**`, `/api/actividades/**` with role rules
- New: `InformeSecurityTest` (MockMvc, follows `SigconBackendSecurityTest` pattern)

- [ ] **Step 1: Implement controllers** binding endpoints from spec §4.6. Use `@PreAuthorize` per role; map errors via existing `GlobalExceptionHandler`.

- [ ] **Step 2: Update SpringDoc/Swagger annotations** so all I2 endpoints appear in `/swagger-ui.html`.

- [ ] **Step 3: Update SigconBackendApplicationTests exclusions** if needed so the smoke test still loads context without Oracle (mirror existing `application.service.*` and `web.controller.*` exclusion).

- [ ] **Step 4: MockMvc tests** covering the 8 frontend acceptance bullets and rejecting:
- POST `/api/informes` from REVISOR or SUPERVISOR → 403.
- POST `/api/informes/{id}/aprobar` from CONTRATISTA → 403.
- Transición invalida → 409 `TRANSICION_INVALIDA`.
- `/api/informes` and `/api/actividades` are NOT public (require auth).

- [ ] **Step 5: Validate**

```powershell
cd sigcon-backend
mvn test -Dtest=*SecurityTest
mvn test
mvn test -DskipTests
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "/api/notificaciones|Pdf|notificacion.service"
```

Expected: all tests pass; no I3 references.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-backend/src/main/java sigcon-backend/src/test/java sigcon-backend/src/main/resources
git commit -m "feat: add SIGCON I2 informe REST controllers and security"
```

---

## Task 7: Frontend Models And Services

**Files:**
- New: TS models under `core/models/` for informe, actividad-informe, soporte-adjunto, documento-adicional, observacion
- New: TS services under `core/services/`

- [ ] **Step 1: Create models** matching backend DTOs (1-to-1 fields, camelCase, nullable typing matches Java optionality).

- [ ] **Step 2: Create services** using `HttpClient`, signal-based state where useful, and relative URLs `/api/...` (proxy handles dev forwarding).

- [ ] **Step 3: Unit tests** following `contrato.service.spec.ts` pattern — mock `HttpClient` and verify URLs/methods.

- [ ] **Step 4: Validate**

```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

- [ ] **Step 5: Commit**

```powershell
git add sigcon-angular/src/app/core
git commit -m "feat: add SIGCON I2 frontend core models and services"
```

---

## Task 8: Frontend — Informe Form And Detalle (Contratista)

**Files:**
- New: `features/informes/nuevo/informe-form.component.ts`
- New: `features/informes/detalle/informe-detalle.component.ts`
- New: `features/informes/preview/informe-preview.component.ts`
- Modify: `app.routes.ts` (add `contratos/:contratoId/informes/nuevo`, `informes/:id`, `informes/:id/preview`)

- [ ] **Step 1:** Build the form following `Prototipo/nuevo_informe_de_actividades_optimizado_sigcon/` (screen.png + code.html). Iterate over the contract's obligaciones in order, allow descripcion/porcentaje per obligacion, attach soportes (URL or archivo), and load documentos adicionales from the OPS catalog.

- [ ] **Step 2:** Save draft → `POST /api/informes` then `POST /api/informes/{id}/actividades` per row. Re-open from detail page when state is `BORRADOR`.

- [ ] **Step 3:** Implement preview screen reading `GET /api/informes/{id}` and showing the same data in read-only PDF-like layout.

- [ ] **Step 4:** Confirm dialog before `POST /api/informes/{id}/enviar`.

- [ ] **Step 5: Validate**

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

- [ ] **Step 6: Commit**

```powershell
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON I2 informe form, detalle and preview screens"
```

---

## Task 9: Frontend — Corregir Informe Devuelto (Contratista)

**Files:**
- New: `features/informes/corregir/corregir-informe.component.ts`
- Modify: `app.routes.ts` (add `informes/:id/corregir`)

- [ ] **Step 1:** Reuse the form layout from Task 8 but render past observaciones in a side panel. Editable only when state ∈ {`BORRADOR`, `DEVUELTO`}.

- [ ] **Step 2:** On submit → `PUT /api/informes/{id}` for the form changes, then `POST /api/informes/{id}/enviar`.

- [ ] **Step 3: Validate / Commit**

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON I2 corregir informe screen"
```

---

## Task 10: Frontend — Cola De Revision (REVISOR)

**Files:**
- New: `features/revision/cola-revision.component.ts`
- Modify: `app.routes.ts` (add `revision/informes` with `roleGuard(['REVISOR'])`)
- Modify: `shared/components/sidebar.component.ts` (show "Revisión" entry only for REVISOR)

- [ ] **Step 1:** Paginated table listing informes `ENVIADO` for contratos asignados to the current revisor. Filters: contrato, contratista, periodo.

- [ ] **Step 2:** Row actions: open detail, "Aprobar revisión" → `POST /api/informes/{id}/aprobar-revision`; "Devolver" opens dialog requiring observación, then `POST /api/informes/{id}/devolver-revision`.

- [ ] **Step 3: Validate / Commit**

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON I2 cola de revisión"
```

---

## Task 11: Frontend — Cola De Aprobación (SUPERVISOR)

**Files:**
- New: `features/aprobacion/cola-aprobacion.component.ts`
- Modify: `app.routes.ts` (add `aprobacion/informes` with `roleGuard(['SUPERVISOR'])`)
- Modify: `shared/components/sidebar.component.ts` (show "Aprobación" entry only for SUPERVISOR)

- [ ] **Step 1:** Paginated table listing informes `EN_REVISION` for contratos supervisados. Same filter pattern as Task 10.

- [ ] **Step 2:** Row actions: open detail, "Aprobar" → `POST /api/informes/{id}/aprobar`; "Devolver" with observación → `POST /api/informes/{id}/devolver`.

- [ ] **Step 3: Validate / Commit**

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON I2 cola de aprobación"
```

---

## Task 12: Activate I1 Placeholders (Detalle Contrato And Admin Dashboard)

**Files:**
- Modify: `features/contratos/detalle/contrato-detalle.component.ts` — replace disabled "Nuevo Informe" with `routerLink="/contratos/{{c.id}}/informes/nuevo"`. List historial with `GET /api/informes?contratoId=...`.
- Modify: `features/admin/dashboard/admin-dashboard.component.ts` — remove `opacity-50` from "Informes" card; **for ADMIN it leads to a read-only consultation view** (per spec §6: ADMIN consults but does not aprobar/devolver in I2). If a read-only admin view is too costly for I2 scope, leave the admin Informes card linked to a "próximamente" message. Choose explicitly during implementation and document the choice in the execution log.

- [ ] **Step 1:** Wire detalle contrato to the new informe routes. Show real historial with state chips.

- [ ] **Step 2:** Decide ADMIN visibility (read-only list vs. redirect-to-pending). Document.

- [ ] **Step 3: Validate / Commit**

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
git add sigcon-angular/src/app
git commit -m "feat: activate I1 informe placeholders for I2"
```

---

## Task 13: End-To-End Local Verification And Documentation

**Files:**
- Modify: `docs/ARRANQUE.md` (append "I2 incluye…" / "I2 excluye…" sections)
- Update: `docs/plans/2026-05-01-sigcon-i2-execution-log.md`

- [ ] **Step 1: Backend tests**

```powershell
cd sigcon-backend
mvn test
```

Expected: all I1 + I2 tests pass.

- [ ] **Step 2: WAR build**

```powershell
mvn clean package -DskipTests
```

Expected: `target/sigcon-backend.war` exists.

- [ ] **Step 3: Frontend tests + build**

```powershell
cd ..\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

- [ ] **Step 4: Manual acceptance walkthrough** (`local-dev`)

Run backend + frontend, then manually walk: login as contratista → crear informe BORRADOR → enviar → login revisor → aprobar revisión → login supervisor → aprobar → verificar que el informe queda APROBADO con `pdfRuta = null`. Repeat with the devolver path. Record results in execution log.

- [ ] **Step 5: Forward-compat audit**

```powershell
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "Pdf|notificacion|/api/notificaciones|MailService"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "/api/notificaciones|notificacion.service|pdf.service"
```

Expected: no production code matches (only spec/plan references).

- [ ] **Step 6: Update ARRANQUE.md** with I2 status and any new local-dev steps.

- [ ] **Step 7: Update execution log** with Task 13 results, final commit SHA, and "Estado Final I2" section.

- [ ] **Step 8: Commit**

```powershell
git add docs/ARRANQUE.md docs/plans/2026-05-01-sigcon-i2-execution-log.md
git commit -m "docs: complete SIGCON I2 — verify local flow and update docs"
```

---

## Risks Captured After I1

| Riesgo | Mitigación I2 |
|---|---|
| Auditoría con `"SYSTEM"` enmascara autoría real de transiciones | Documentar como deuda I3; en I2 el principal real ya queda en `Informe.fechaUltimoEnvio` indirectamente (via timestamps + role checks en el service). |
| `DocumentStorageService` puede no soportar carpetas por contrato/informe | Antes de Task 4 verificar firma de `LocalDocumentStorageService`; si no expone subdir, extender la interfaz primero. |
| `db/00_setup.sql` se vuelve monolítico | Mantener archivo único pero con encabezados `-- ===== INCREMENTO N =====` para trazabilidad. PRD pide un solo archivo, no se rompe la decisión. |
| Java 21 maven runtime puede aceptar APIs no disponibles en JDK 8 | Antes de cierre I2, idealmente probar `mvn -P jdk8-compat verify` con un JDK 8 real; si no es viable en máquina actual, documentar como pre-prod gate. |
| ADMIN read-only de informes amplía scope de I2 | Decisión registrada en Task 12 Step 2. Default: linkar a "próximamente" para no inflar el incremento. |
| Múltiples modelos editan en paralelo | Cada task se commitea individualmente; el execution log se actualiza al cierre de cada task; antes de empezar, hacer `git pull --ff-only origin feat/sigcon-i2`. |

---

## Acceptance Criteria (Mirrors Spec §7)

### Backend

- [x] Contratista crea informe `BORRADOR` sobre contrato activo propio.
- [x] Contratista no crea informe sobre contrato ajeno o inactivo (`ACCESO_DENEGADO` o `CONTRATO_NO_ACTIVO`).
- [x] Actividades aceptan porcentaje 0–100; fuera de rango → `PORCENTAJE_INVALIDO`.
- [x] Soportes URL aceptan `http://`/`https://`; otros → `SOPORTE_INVALIDO`.
- [x] Documentos adicionales asociados al informe.
- [ ] `BORRADOR → ENVIADO` con cero actividades → `ACTIVIDAD_REQUERIDA`.
- [ ] Revisor lista solo `ENVIADO` asignados.
- [ ] `ENVIADO → EN_REVISION` solo revisor asignado.
- [ ] `ENVIADO → DEVUELTO` exige `OBSERVACION_REQUERIDA`.
- [ ] Supervisor lista solo `EN_REVISION` supervisados.
- [ ] `EN_REVISION → APROBADO` solo supervisor asignado; `pdfRuta` queda nulo.
- [ ] `EN_REVISION → DEVUELTO` exige `OBSERVACION_REQUERIDA`.
- [x] `APROBADO` es terminal (`INFORME_NO_EDITABLE`).
- [ ] Transiciones invalidas → 409 `TRANSICION_INVALIDA`.

### Frontend

- [ ] "Nuevo Informe" habilitado para CONTRATISTA con contrato `EN_EJECUCION`.
- [ ] Form muestra obligaciones en orden.
- [ ] Borrador se guarda y se reabre.
- [ ] Envío exige confirmación.
- [ ] Pantalla de corrección muestra observaciones históricas.
- [ ] Cola revisor muestra `ENVIADO` asignados.
- [ ] Cola supervisor muestra `EN_REVISION` asignados.
- [ ] Devolver exige observación no vacía.

### General

- [ ] Flujo completo `BORRADOR → APROBADO` en `local-dev`.
- [x] No hay PDF real ni notificaciones en producción de código.
- [ ] Swagger documenta `/api/informes/**` y `/api/actividades/**`.
- [x] I1 tests siguen pasando sin cambios.

---

## Multi-Model Coordination Rules

This plan is executed in shared mode between Codex, Claude, and other models. Follow these rules to avoid stepping on each other:

1. **Always pull before starting:** `git fetch origin && git pull --ff-only origin feat/sigcon-i2`.
2. **One task at a time per model.** Never start a new task while another model has a task in `🔄 in progress` state in the execution log.
3. **Update the execution log on commit.** When you finish a task, update the log with the commit SHA, files touched, validations run, and any deviation from the plan.
4. **If you find a spec/plan inconsistency**, do NOT silently fix it in code. Either:
   - Patch the plan/spec in the same task and call it out in the log, or
   - Open a sub-task and document the conflict for the next model.
5. **If you skip or reorder steps**, write why in the log; another model may need to know.
6. **If a task gets blocked**, set its log status to `⏸ blocked` with the reason and a clear "what would unblock me" note.

---

*Plan promoted from outline to executable form on 2026-05-01 after closing I1.*
