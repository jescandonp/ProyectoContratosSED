# SIGCON Incremento 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Incremento 1 of SIGCON: local authentication/profile, contract administration, and contractor contract views, without implementing reports, review flow, PDF generation, or notifications.

**Architecture:** Implement a Spring Boot 2.7.18 backend packaged as WAR for Java 8/WebLogic, with Oracle-compatible SQL scripts and a local-dev security profile. Implement an Angular 20 + PrimeNG 21 frontend that follows `Prototipo/DESIGN.md` and consumes the backend APIs through a proxy. Keep `docs/superpowers/CONSTITUTION.md`, `ARCHITECTURE.md`, the PRD, and the I1 spec as the governing sources of truth; use I2/I3 only as forward-compatibility constraints so I1 does not block the next increments.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Security 5.7, Spring Data JPA, SpringDoc OpenAPI 1.7.0, Oracle JDBC ojdbc8, Maven WAR, Angular 20, PrimeNG 21, TypeScript 5.8, Tailwind CSS 3.4.

---

## Source Of Truth

- SDD governance: `docs/superpowers/CONSTITUTION.md`
- Architecture constraints: `ARCHITECTURE.md`
- Product scope: `docs/superpowers/specs/2026-04-30-sigcon-prd.md`
- I1 technical contract: `docs/superpowers/specs/2026-04-30-sigcon-i1-spec.md`
- Forward compatibility references: `docs/superpowers/specs/2026-05-01-sigcon-i2-spec.md` and `docs/superpowers/specs/2026-05-01-sigcon-i3-spec.md`
- Visual reference: `Prototipo/DESIGN.md` and `Prototipo/*/screen.png`
- Strict I1 boundary: do not implement report creation, review/approval workflow, PDF generation, or notifications.

Apply this authority order when documents disagree: `CONSTITUTION.md` → `ARCHITECTURE.md` → PRD → I1 spec → I2/I3 forward-compatibility notes → this plan → code.

## Forward-Compatibility Gates From I2/I3

I1 must leave these seams ready without implementing later increments:

- Contract detail includes an empty reports-history section and a disabled "Nuevo Informe" action; no `/api/informes` endpoints or Angular informe routes are implemented in I1.
- `Usuario.firmaImagen` and the firma upload path must be suitable for later PDF signing; I1 only stores JPG/PNG signature references and does not enforce signature presence.
- File storage for firma must be abstractable/configurable so I2 soportes and I3 PDFs can reuse the same pattern; I1 implements only the local-dev storage path.
- `DocumentoCatalogo` must remain scoped by `tipoContrato=OPS` and usable by I2 as the catalog for additional report documents.
- Contract assignments for `contratista`, `revisor`, and `supervisor` are required model relationships for I2 queues and I3 PDF/signature authorization.
- I1 SQL must not create I2/I3 tables (`SGCN_INFORMES`, `SGCN_ACTIVIDADES`, `SGCN_SOPORTES`, `SGCN_DOCS_ADICIONALES`, `SGCN_OBSERVACIONES`, `SGCN_NOTIFICACIONES`) or PDF metadata columns.
- Topbar may reserve visual space for a future notifications icon only if it is inert and hidden/disabled; no notifications API, badge count, or polling in I1.

## File Structure

Create the following implementation structure:

```text
ProyectoContratosSED/
├── ARRANQUE.md
├── db/
│   ├── 00_setup.sql
│   └── 01_datos_prueba.sql
├── sigcon-backend/
│   ├── pom.xml
│   ├── src/main/java/co/gov/bogota/sed/sigcon/
│   │   ├── SigconBackendApplication.java
│   │   ├── application/
│   │   ├── config/
│   │   ├── domain/
│   │   └── web/
│   ├── src/main/resources/
│   │   └── application.yml
│   └── src/main/webapp/WEB-INF/
│       └── weblogic.xml
└── sigcon-angular/
    ├── package.json
    ├── angular.json
    ├── proxy.conf.json
    ├── tailwind.config.js
    └── src/app/
        ├── core/
        ├── features/
        └── shared/
```

Use commits after each task if the workspace is a Git repository. If `.git` is absent, initialize Git in Task 1 before implementation and make an initial baseline commit.

---

### Task 1: Repository Baseline And SDD Guardrails

**Files:**
- Create: `.gitignore`
- Create: `ARRANQUE.md`
- Verify: `docs/superpowers/CONSTITUTION.md`
- Verify: `docs/superpowers/plans/2026-05-01-sigcon-i1-implementation-plan.md`

- [ ] **Step 1: Check repository state**

Run:

```powershell
git status --short
```

Expected if Git already exists: command succeeds and shows current changes.

Expected if Git is absent: command fails with `fatal: not a git repository`.

- [ ] **Step 2: Initialize Git only if absent**

Run only when Step 1 reports no repository:

```powershell
git init
git add ARCHITECTURE.md "Spec-Driven Development (SDD)_ A Comprehensive Technical Guide.pdf" docs Prototipo Notas_ProyectoContratos
git commit -m "chore: baseline SDD artifacts"
```

Expected: repository initialized with the existing SDD artifacts committed.

- [ ] **Step 3: Create `.gitignore`**

Add ignores for generated artifacts:

```gitignore
.idea/
.vscode/
target/
dist/
node_modules/
.angular/
coverage/
*.log
*.war
```

- [ ] **Step 4: Create initial `ARRANQUE.md`**

Document that I1 is not yet executed and that implementation must follow this plan:

```markdown
# ARRANQUE SIGCON

SIGCON usa Spec-Driven Development (SDD) en nivel Spec-Anchored.

## Orden de trabajo

1. Constitucion SDD: `docs/superpowers/CONSTITUTION.md`
2. Arquitectura SED: `ARCHITECTURE.md`
3. PRD: `docs/superpowers/specs/2026-04-30-sigcon-prd.md`
4. Spec tecnica I1: `docs/superpowers/specs/2026-04-30-sigcon-i1-spec.md`
5. Specs de referencia futura: `docs/superpowers/specs/2026-05-01-sigcon-i2-spec.md`, `docs/superpowers/specs/2026-05-01-sigcon-i3-spec.md`
6. Plan I1: `docs/superpowers/plans/2026-05-01-sigcon-i1-implementation-plan.md`
7. Implementacion I1

## Alcance I1

- Auth local-dev y perfil de usuario
- Administración de contratos
- Vista de contratos del contratista

Fuera de I1: informes, revisión/aprobación, PDF y notificaciones.
```

- [ ] **Step 5: Verify SDD governance files**

Run:

```powershell
Test-Path docs\superpowers\CONSTITUTION.md
Select-String -Path docs\superpowers\CONSTITUTION.md -Pattern "Spec-Anchored|ARCHITECTURE.md|Java runtime: Oracle JDK 8|Incremento 1 excluye|Gates De Calidad"
Select-String -Path docs\superpowers\plans\2026-05-01-sigcon-i1-implementation-plan.md -Pattern "CONSTITUTION.md|ARCHITECTURE.md|I1 boundary"
Select-String -Path docs\superpowers\plans\2026-05-01-sigcon-i1-implementation-plan.md -Pattern "Forward-Compatibility Gates|SGCN_INFORMES|Nuevo Informe|firmaImagen"
```

Expected: `Test-Path` returns `True`, and every `Select-String` command returns at least one match.

- [ ] **Step 6: Commit**

```powershell
git add .gitignore ARRANQUE.md docs/superpowers/CONSTITUTION.md docs/superpowers/plans/2026-05-01-sigcon-i1-implementation-plan.md
git commit -m "docs: add SIGCON SDD governance and I1 plan"
```

Expected: commit succeeds.

---

### Task 2: Oracle Schema Scripts For I1

**Files:**
- Create: `db/00_setup.sql`
- Create: `db/01_datos_prueba.sql`
- Test: static SQL validation by inspection commands below

- [ ] **Step 1: Create `db/00_setup.sql`**

Include exactly these I1 tables and sequences from the spec:

- `SGCN_USUARIOS_SEQ`, `SGCN_USUARIOS`
- `SGCN_CONTRATOS_SEQ`, `SGCN_CONTRATOS`
- `SGCN_OBLIGACIONES_SEQ`, `SGCN_OBLIGACIONES`
- `SGCN_DOCS_CATALOGO_SEQ`, `SGCN_DOCS_CATALOGO`

Do not add I2/I3 objects in I1: `SGCN_INFORMES`, `SGCN_ACTIVIDADES`, `SGCN_SOPORTES`, `SGCN_DOCS_ADICIONALES`, `SGCN_OBSERVACIONES`, `SGCN_NOTIFICACIONES`, `PDF_GENERADO_AT`, or `PDF_HASH`.

Required constraints:

- `CHK_USUARIOS_ROL` allows `CONTRATISTA`, `REVISOR`, `SUPERVISOR`, `ADMIN`
- `CHK_CONTRATOS_ESTADO` allows `EN_EJECUCION`, `LIQUIDADO`, `CERRADO`
- `CHK_CONTRATOS_TIPO` allows `OPS`
- `CHK_DOCS_CATALOGO_TIPO` allows `OPS`
- Foreign keys from contracts to users and obligations to contracts
- Audit triggers update `UPDATED_AT` on update

- [ ] **Step 2: Create `db/01_datos_prueba.sql`**

Seed local-dev data:

- Admin: `admin@educacionbogota.edu.co`
- Contractor: `juan.escandon@educacionbogota.edu.co`
- Reviewer: `revisor1@educacionbogota.edu.co`
- Supervisor: `supervisor1@educacionbogota.edu.co`
- Contract: `OPS-2026-001`
- Three obligations
- Two document catalog rows

- [ ] **Step 3: Verify required objects are present**

Run:

```powershell
Select-String -Path db\00_setup.sql -Pattern "SGCN_USUARIOS|SGCN_CONTRATOS|SGCN_OBLIGACIONES|SGCN_DOCS_CATALOGO|CHK_USUARIOS_ROL|CHK_CONTRATOS_ESTADO|TRG_USUARIOS_AUDIT"
Select-String -Path db\01_datos_prueba.sql -Pattern "admin@educacionbogota.edu.co|OPS-2026-001|Planilla de aportes seguridad social"
Select-String -Path db\00_setup.sql -Pattern "SGCN_INFORMES|SGCN_ACTIVIDADES|SGCN_SOPORTES|SGCN_NOTIFICACIONES|PDF_HASH"
```

Expected: the first two commands return matches; the third command returns no matches.

- [ ] **Step 4: Commit**

```powershell
git add db
git commit -m "feat: add SIGCON I1 Oracle schema scripts"
```

---

### Task 3: Backend Bootstrap As WebLogic-Compatible WAR

**Files:**
- Create: `sigcon-backend/pom.xml`
- Create: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/SigconBackendApplication.java`
- Create: `sigcon-backend/src/main/resources/application.yml`
- Create: `sigcon-backend/src/main/webapp/WEB-INF/weblogic.xml`
- Test: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/SigconBackendApplicationTests.java`

- [ ] **Step 1: Create Maven project**

Configure:

- `<packaging>war</packaging>`
- Java source/target `1.8`
- final artifact name `sigcon-backend`
- `spring-boot-starter-tomcat` with `provided` scope
- dependencies for web, security, oauth2 resource server, data-jpa, validation, actuator, springdoc, lombok, ojdbc8, tests

- [ ] **Step 2: Create application class**

`SigconBackendApplication` must extend `SpringBootServletInitializer` and override `configure`.

- [ ] **Step 3: Create `application.yml`**

Profiles:

- default active profile from `${SPRING_PROFILE:local-dev}`
- `local-dev` datasource using `jdbc:oracle:thin:@localhost:1521/XEPDB1`
- `weblogic` datasource from `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Swagger paths `/api-docs` and `/swagger-ui.html`
- `spring.jpa.open-in-view: false`
- `ddl-auto: validate`

- [ ] **Step 4: Create WebLogic descriptor**

`weblogic.xml` must set context root `/sigcon` and prefer web-inf classes.

- [ ] **Step 5: Add context-load test**

Run:

```powershell
cd sigcon-backend
mvn test -Dtest=SigconBackendApplicationTests
```

Expected: context test passes or fails only because Oracle local is not available. If Oracle is unavailable, mark this task complete only after adding a test profile that excludes datasource auto-configuration for the context test.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-backend
git commit -m "feat: bootstrap SIGCON backend WAR"
```

---

### Task 4: Backend Domain Model And Repositories

**Files:**
- Create under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/`
- Test under `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/domain/`

- [ ] **Step 1: Add enums**

Create:

- `RolUsuario` with `CONTRATISTA`, `REVISOR`, `SUPERVISOR`, `ADMIN`
- `EstadoContrato` with `EN_EJECUCION`, `LIQUIDADO`, `CERRADO`
- `TipoContrato` with `OPS`

- [ ] **Step 2: Add JPA entities**

Create entities exactly mapped to I1 SQL:

- `Usuario`
- `Contrato`
- `Obligacion`
- `DocumentoCatalogo`

Use `@SequenceGenerator(allocationSize = 1)`, `@Enumerated(EnumType.STRING)`, audit fields, and lazy relationships.

- [ ] **Step 3: Add repositories**

Create:

- `UsuarioRepository`
- `ContratoRepository`
- `ObligacionRepository`
- `DocumentoCatalogoRepository`

Required methods:

```java
Optional<Usuario> findByEmailAndActivoTrue(String email);
List<Usuario> findByRolAndActivoTrue(RolUsuario rol);
Page<Usuario> findByActivoTrue(Pageable pageable);
Page<Contrato> findByContratistaAndActivoTrue(Usuario contratista, Pageable pageable);
Page<Contrato> findBySupervisorAndActivoTrue(Usuario supervisor, Pageable pageable);
Page<Contrato> findByActivoTrue(Pageable pageable);
Optional<Contrato> findByNumeroAndActivoTrue(String numero);
List<Obligacion> findByContratoIdAndActivoTrueOrderByOrdenAsc(Long contratoId);
List<DocumentoCatalogo> findByTipoContratoAndActivoTrue(TipoContrato tipo);
Page<DocumentoCatalogo> findByActivoTrue(Pageable pageable);
```

- [ ] **Step 4: Verify compile**

Run:

```powershell
cd sigcon-backend
mvn test -DskipTests
```

Expected: compilation succeeds.

- [ ] **Step 5: Commit**

```powershell
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain
git commit -m "feat: add SIGCON I1 domain model"
```

---

### Task 5: Backend DTOs, Services, And Error Contract

**Files:**
- Create under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/`
- Create under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/`
- Test under `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/`

- [ ] **Step 1: Add DTOs**

Create DTO packages:

- `usuario`: `UsuarioDto`, `UsuarioRequest`, `PerfilUpdateRequest`
- `contrato`: `ContratoResumenDto`, `ContratoDetalleDto`, `ContratoRequest`, `EstadoContratoRequest`
- `obligacion`: `ObligacionDto`, `ObligacionRequest`
- `catalogo`: `DocumentoCatalogoDto`, `DocumentoCatalogoRequest`

Use the exact fields listed in the I1 spec.

- [ ] **Step 2: Add mappers**

Create focused mapper classes:

- `UsuarioMapper`
- `ContratoMapper`
- `ObligacionMapper`
- `DocumentoCatalogoMapper`

Do not expose JPA entities from controllers.

- [ ] **Step 3: Add services**

Create services:

- `UsuarioService`
- `ContratoService`
- `ObligacionService`
- `DocumentoCatalogoService`
- `CurrentUserService`
- `DocumentStorageService`
- `LocalDocumentStorageService`

Required service behavior:

- Admin sees all active contracts.
- Contractor sees only own active contracts.
- Supervisor sees supervised active contracts.
- Reviewer sees contracts assigned for review.
- Duplicate contract number throws conflict.
- Contractor access to a foreign contract throws access denied.
- Soft deletes set `activo=false`.
- `DocumentStorageService` stores firma files in `local-dev` and exposes a stable interface reusable by I2 soportes and I3 PDFs.
- I1 does not implement soporte, informe, PDF, or notification storage operations.

- [ ] **Step 4: Add standard error response**

Create error response JSON shape:

```json
{
  "error": "CODIGO_ERROR",
  "mensaje": "Descripción legible del error",
  "timestamp": "2026-04-30T10:00:00"
}
```

Use error codes from the I1 spec: `USUARIO_NO_ENCONTRADO`, `CONTRATO_NO_ENCONTRADO`, `NUMERO_CONTRATO_DUPLICADO`, `EMAIL_DUPLICADO`, `ACCESO_DENEGADO`, `FORMATO_IMAGEN_INVALIDO`, `ESTADO_INVALIDO`.

- [ ] **Step 5: Add unit tests for service rules**

Run:

```powershell
cd sigcon-backend
mvn test -Dtest=*ServiceTest
```

Expected: service authorization and duplicate-number tests pass.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception sigcon-backend/src/test
git commit -m "feat: add SIGCON I1 application services"
```

---

### Task 6: Backend Security And Controllers

**Files:**
- Create under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/`
- Create under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/`
- Test under `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/`

- [ ] **Step 1: Add local-dev security**

Create `DevSecurityConfig` active under `local-dev` with HTTP Basic users:

- `admin/admin123` role `ADMIN`
- `contratista1/contratista123` role `CONTRATISTA`
- `revisor1/revisor123` role `REVISOR`
- `supervisor1/supervisor123` role `SUPERVISOR`

- [ ] **Step 2: Add weblogic security**

Create JWT resource server config active under `weblogic`, validating Azure AD issuer/JWKS from environment.

- [ ] **Step 3: Add controllers**

Create:

- `UsuarioController` at `/api/usuarios`
- `ContratoController` at `/api/contratos`
- `ObligacionController` at `/api/contratos/{id}/obligaciones`
- `DocumentoCatalogoController` at `/api/documentos-catalogo`

Expose exactly the endpoints listed in the I1 spec.

- [ ] **Step 4: Add profile/firma endpoint**

`POST /api/usuarios/me/firma` accepts multipart JPG/PNG up to 2MB and rejects other formats with `FORMATO_IMAGEN_INVALIDO`.

For I1, persist the generated file reference in `FIRMA_IMAGEN`. Store files under a configurable local path in `local-dev`; do not implement SharePoint or Oracle BLOB storage in I1.

- [ ] **Step 5: Add MockMvc tests**

Required tests:

- contractor `GET /api/contratos` returns only own contracts
- admin `GET /api/contratos` returns all contracts
- non-admin `POST /api/contratos` returns 403
- duplicate contract number returns 409
- contractor opening another contractor contract returns 403
- firma upload accepts PNG/JPG and rejects TXT/PDF
- `/actuator/health` returns 200 without auth
- `/swagger-ui.html` and `/api-docs` are public
- `/api/informes` and `/api/notificaciones` are not exposed in I1

Run:

```powershell
cd sigcon-backend
mvn test -Dtest=*ControllerTest,*SecurityTest
```

Expected: all controller/security tests pass.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web sigcon-backend/src/test
git commit -m "feat: add SIGCON I1 backend APIs and security"
```

---

### Task 7: Frontend Bootstrap And Design System

**Files:**
- Create: `sigcon-angular/package.json`
- Create: `sigcon-angular/angular.json`
- Create: `sigcon-angular/proxy.conf.json`
- Create: `sigcon-angular/tailwind.config.js`
- Create: `sigcon-angular/src/styles.scss`
- Create: `sigcon-angular/src/app/shared/design-tokens.scss`

- [ ] **Step 1: Create Angular 20 workspace**

Create the Angular app named `sigcon-angular` with standalone components, strict TypeScript, routing, SCSS, and test support.

- [ ] **Step 2: Add dependencies**

Use:

- `@angular/*` `^20.0.0`
- `primeng` `^21.0.0`
- `@primeng/themes` `^21.0.0`
- `primeicons` `^7.0.0`
- `@azure/msal-angular` `^3.0.0`
- `@azure/msal-browser` `^3.0.0`
- `tailwindcss` `^3.4.0`
- `rxjs` `^7.8.0`

- [ ] **Step 3: Add design tokens**

Map `Prototipo/DESIGN.md` tokens into CSS variables. Required tokens:

- `--color-primary: #002869`
- `--color-primary-container: #0b3d91`
- `--color-secondary: #7e5700`
- `--color-secondary-container: #feb300`
- `--color-tertiary: #5f001b`
- `--color-surface: #f8f9ff`
- `--font-family: 'Public Sans', 'Inter', sans-serif`
- PrimeNG primary variables

- [ ] **Step 4: Configure proxy**

`proxy.conf.json` proxies `/api`, `/api-docs`, `/swagger-ui.html`, and `/actuator` to backend local URL `http://localhost:8080`.

- [ ] **Step 5: Verify frontend installs and builds**

Run:

```powershell
cd sigcon-angular
npm install
npm run build
```

Expected: Angular production build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add sigcon-angular
git commit -m "feat: bootstrap SIGCON Angular app"
```

---

### Task 8: Frontend Core Auth, API Models, And Shell

**Files:**
- Create under `sigcon-angular/src/app/core/`
- Create under `sigcon-angular/src/app/shared/`
- Test under `sigcon-angular/src/app/**/*.spec.ts`

- [ ] **Step 1: Add TypeScript models**

Create:

- `usuario.model.ts`
- `contrato.model.ts`
- `obligacion.model.ts`
- `documento-catalogo.model.ts`

Fields must match I1 spec.

Do not create informe, actividad, soporte, PDF, or notificacion TypeScript models in I1. The contract detail view may use an empty reports-history UI model local to the component only.

- [ ] **Step 2: Add API services**

Create:

- `usuario.service.ts`
- `contrato.service.ts`
- `obligacion.service.ts`
- `documento-catalogo.service.ts`

All API calls use relative `/api/...` URLs.

Do not create `informe.service.ts`, `pdf.service.ts`, or `notificacion.service.ts` in I1.

- [ ] **Step 3: Add auth facade**

Create:

- `auth.service.ts`
- `auth.guard.ts`
- `role.guard.ts`
- `dev-session.service.ts`

In local-dev, login stores the chosen dev user session and sends HTTP Basic credentials through an interceptor. In production/weblogic mode, leave MSAL wiring configured but environment driven.

- [ ] **Step 4: Add app shell**

Create:

- `shared/app-shell.component.ts`
- `shared/components/sidebar`
- `shared/components/topbar`
- `shared/components/status-chip`
- `shared/components/empty-state`

Sidebar hides Admin navigation unless current user role is `ADMIN`.

Topbar may include a hidden or disabled notification affordance for future I3 visual alignment, but it must not call `/api/notificaciones`, show a badge count, or start polling in I1.

- [ ] **Step 5: Add routes**

Routes must match I1 spec:

- `/login`
- `/perfil`
- `/contratos`
- `/contratos/:id`
- `/admin`
- `/admin/contratos`
- `/admin/contratos/nuevo`
- `/admin/contratos/:id/editar`
- `/admin/usuarios`
- `/admin/documentos-catalogo`

- [ ] **Step 6: Verify unit tests**

Run:

```powershell
cd sigcon-angular
npm test -- --watch=false
```

Expected: core service/guard tests pass.

- [ ] **Step 7: Commit**

```powershell
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON Angular core shell"
```

---

### Task 9: Frontend I1 Screens

**Files:**
- Create under `sigcon-angular/src/app/features/auth/`
- Create under `sigcon-angular/src/app/features/perfil/`
- Create under `sigcon-angular/src/app/features/contratos/`
- Create under `sigcon-angular/src/app/features/admin/`

- [ ] **Step 1: Login screen**

Implement `/login` following `Prototipo/login_institucional_sigcon/screen.png`.

Local-dev behavior:

- Login as admin redirects to `/admin`
- Login as contractor/reviewer/supervisor redirects to `/contratos`

- [ ] **Step 2: Perfil screen**

Implement `/perfil` following `Prototipo/mi_perfil_y_firma_sigcon/screen.png`.

Required:

- Show current user data
- Edit name and cargo
- Upload JPG/PNG signature
- Preview current signature

- [ ] **Step 3: Contract list screen**

Implement `/contratos` following `Prototipo/panel_del_contratista_sigcon/screen.png`.

Required:

- PrimeNG `p-table`
- server-side pagination
- search by number/object with 300ms debounce
- filter by status
- row click navigates to `/contratos/:id`

- [ ] **Step 4: Contract detail screen**

Implement `/contratos/:id` following `Prototipo/detalle_del_contrato_sigcon/screen.png`.

Required:

- contract data
- supervisor/reviewer data
- ordered obligations
- empty reports history message
- disabled "Nuevo Informe" button for I1

The empty reports section is a forward-compatibility placeholder for I2. It must not call `/api/informes`, must not navigate to informe routes, and must not show a PDF download action in I1.

- [ ] **Step 5: Admin contract screens**

Implement:

- `/admin`
- `/admin/contratos`
- `/admin/contratos/nuevo`
- `/admin/contratos/:id/editar`

Follow `Prototipo/dashboard_administrador_sigcon/screen.png` and `Prototipo/gesti_n_de_contratos_sigcon/screen.png`.

Required:

- contract CRUD form
- contractor/reviewer/supervisor dropdowns
- dynamic obligations list
- required-field validation before submit

- [ ] **Step 6: Admin users and catalog screens**

Implement:

- `/admin/usuarios`
- `/admin/documentos-catalogo`

Required:

- user table filtered by role where needed
- document catalog CRUD for `OPS`
- soft-delete actions call backend delete endpoints

Keep document catalog fields compatible with I2 additional report documents: name, description, required flag, and `tipoContrato=OPS`. Do not add report-specific document upload behavior in I1.

- [ ] **Step 7: Verify build and visual consistency**

Run:

```powershell
cd sigcon-angular
npm run build
```

Expected: build succeeds.

Manually compare the implemented screens to the referenced `Prototipo/*/screen.png` files for layout, density, color tokens, and table styling.

- [ ] **Step 8: Commit**

```powershell
git add sigcon-angular/src/app
git commit -m "feat: add SIGCON I1 frontend screens"
```

---

### Task 10: End-To-End Local Verification And Documentation

**Files:**
- Modify: `ARRANQUE.md`
- Verify: backend, frontend, SQL, acceptance matrix

- [ ] **Step 1: Complete `ARRANQUE.md`**

Document:

- prerequisites: JDK 8, Maven 3.9, Node 20, Oracle XE/local Oracle 19c-compatible database
- database setup with `db/00_setup.sql` and `db/01_datos_prueba.sql`
- backend run command
- frontend run command
- users and passwords
- URLs:
  - backend health: `http://localhost:8080/actuator/health`
  - Swagger: `http://localhost:8080/swagger-ui.html`
  - frontend: `http://localhost:4200`

- [ ] **Step 2: Run backend tests**

```powershell
cd sigcon-backend
mvn test
```

Expected: all tests pass.

- [ ] **Step 3: Build WAR**

```powershell
cd sigcon-backend
mvn clean package
```

Expected: `target/sigcon-backend.war` is created.

- [ ] **Step 4: Run frontend tests and build**

```powershell
cd sigcon-angular
npm test -- --watch=false
npm run build
```

Expected: tests and build pass.

- [ ] **Step 5: Verify I1 acceptance criteria**

Manually run through:

- admin can create a contract with obligations and assignments
- contractor sees only own contracts
- contractor cannot access another contractor contract
- non-admin cannot create contracts
- profile signature upload accepts JPG/PNG and rejects other files
- signature upload stores through `DocumentStorageService` local-dev implementation
- contract list search and pagination work
- detail screen shows ordered obligations
- detail screen shows empty reports history and disabled "Nuevo Informe" without informe API calls
- Admin sidebar hidden for contractor
- Swagger and health are accessible
- no I2/I3 endpoints are exposed: `/api/informes`, `/api/notificaciones`, `/api/informes/{id}/pdf`

- [ ] **Step 6: Commit**

```powershell
git add ARRANQUE.md sigcon-backend sigcon-angular db
git commit -m "docs: document SIGCON I1 local verification"
```

---

## Acceptance Traceability

| I1 acceptance criterion | Covered by tasks |
|---|---|
| Contractor `GET /api/contratos` returns only own contracts | Tasks 5, 6, 10 |
| Admin `GET /api/contratos` returns all contracts | Tasks 5, 6, 10 |
| Non-admin `POST /api/contratos` returns 403 | Tasks 6, 10 |
| Duplicate contract number returns 409 | Tasks 5, 6, 10 |
| Contractor cannot access foreign contract detail | Tasks 5, 6, 10 |
| Signature upload accepts JPG/PNG and rejects other formats | Tasks 6, 9, 10 |
| Signature storage can be reused by I2/I3 storage flows | Tasks 5, 6, 10 |
| Swagger UI accessible | Tasks 3, 6, 10 |
| Health endpoint unauthenticated | Tasks 3, 6, 10 |
| User sync on first login | Tasks 5, 6, 10 |
| Login redirects by role | Tasks 8, 9, 10 |
| Contractor does not see Admin section | Tasks 8, 9, 10 |
| Contract table pagination/search | Tasks 6, 9, 10 |
| Contract detail shows ordered obligations | Tasks 4, 6, 9, 10 |
| Contract detail reserves reports history without implementing I2 | Tasks 9, 10 |
| Contract form validates fields | Tasks 9, 10 |
| Design tokens and prototype consistency | Tasks 7, 9, 10 |
| Works in local-dev | Tasks 2-10 |
| WAR builds for WebLogic | Tasks 3, 10 |

## Forward-Compatibility Traceability

| Future spec dependency | I1 plan response |
|---|---|
| I2 needs contract detail reports history | Task 9 creates empty history only; Task 10 verifies no informe API calls |
| I2 needs storage pattern for soportes | Tasks 5/6 introduce `DocumentStorageService` for firma only |
| I2 needs document catalog for additional report documents | Tasks 5/9 keep catalog generic by OPS type and required flag |
| I2 reviewer/supervisor queues depend on assignments | Tasks 4/5/9 preserve contractor, reviewer, supervisor relationships |
| I3 PDF signing depends on user firma | Tasks 6/9 implement firma upload without enforcing presence |
| I3 notifications depend on topbar | Task 8 shell can reserve hidden/inert notification area only; no API/service in I1 |
| I3 PDF download depends on approved reports | Task 9 does not show PDF download until I3 because I1 has no reports/PDF |

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-01-sigcon-i1-implementation-plan.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, with spec-compliance review and code-quality review after each task.
2. **Inline Execution** - execute tasks in this session using executing-plans, with checkpoints after backend foundation, backend APIs, frontend foundation, frontend screens, and final verification.
