# SIGCON I12 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement I12: dual CONTRATISTA+ADMIN profile, editable `porcentajeEjecucion` in review states, admin control to block new informe creation with bulk notification, and contract type `PRO` with differentiated PDF header.

**Architecture:** Keep the increment split by independent requirement so each block can be tested and committed separately. Backend changes stay in the existing layered structure (`domain`, `application`, `web/controller`, `config`), and frontend changes follow the current Angular standalone-component pattern with inline templates in shell components. Database changes are additive scripts under `db/`; runtime parameter endpoints belong to the existing `/api/admin/parametros` controller surface, not to contract administration.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Security 5, Oracle 19c, JUnit 5, Mockito, Angular 20, PrimeNG 20, TypeScript.

**Spec:** `docs/specs/2026-05-25-sigcon-i12-spec.md`

---

## Current Repo Facts To Preserve

- Branch at planning time: `main...origin/main [ahead 5]`.
- I12 spec is approved at `docs/specs/2026-05-25-sigcon-i12-spec.md`.
- Existing plan file is untracked; do not clean unrelated untracked project artifacts.
- Frontend root is `sigcon-angular`, not `sigcon-frontend`.
- `SidebarComponent` and `TopbarComponent` are standalone components with inline templates:
  - `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`
  - `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`
- Existing parameter service/controller path is `/api/admin/parametros`, with Angular service at:
  - `sigcon-angular/src/app/core/services/parametro.service.ts`
- `ParametroService` currently uses `VB_ACTIVO` with `S`/`N`; I12 `CARGA_INFORMES_ACTIVA` should use `true`/`false` because the spec says so. Keep the two parameter semantics explicit.
- Use PowerShell commands in this Windows workspace. Avoid Unix-only `tail`, `grep`, and line-continuation examples.

---

## Task 1: R4 Backend - `TipoContrato.PRO` And PDF Header

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/enums/TipoContrato.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java`

- [x] **Step 1: Add RED tests for OPS and PRO header text**

Add tests that set `informe.getContrato().setTipo(TipoContrato.OPS)` and `TipoContrato.PRO`, then assert the generated HTML contains:

```java
assertThat(html).contains("CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES");
assertThat(html).contains("CONTRATO DE APOYO A LA GESTION");
```

Use the existing test builders in `InformePdfTemplateServiceTest`; if the current service normalizes accents, assert the exact string produced by the template.

- [x] **Step 2: Verify RED**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformePdfTemplateServiceTest"
```

Expected: compile failure because `TipoContrato.PRO` does not exist, or assertion failure if the enum has already been added.

- [x] **Step 3: Add enum value**

```java
public enum TipoContrato {
    OPS,
    PRO
}
```

- [x] **Step 4: Replace hardcoded PDF header text with a switch**

In `InformePdfTemplateService`, replace the fixed contract-type header with:

```java
private String textoTipoContrato(TipoContrato tipoContrato) {
    if (tipoContrato == TipoContrato.PRO) {
        return "CONTRATO DE APOYO A LA GESTION";
    }
    return "CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES";
}
```

Call this helper where the header line is rendered. Keep the change limited to the PDF header; do not alter other PDF layout sections in I12.

- [x] **Step 5: Verify GREEN and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformePdfTemplateServiceTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/enums/TipoContrato.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java
git commit -m "feat(i12-r4): add PRO contract type PDF header"
```

---

## Task 2: R4 Frontend - Contract Type Selector

**Files:**
- Modify: `sigcon-angular/src/app/core/models/contrato.model.ts`
- Modify: `sigcon-angular/src/app/features/admin/contratos/admin-contratos.component.ts`
- Modify: `sigcon-angular/src/app/features/admin/contratos/admin-contratos.component.html` if this component has an external template; otherwise update the inline template.
- Modify tests near `sigcon-angular/src/app/core/services/contrato.service.spec.ts` or the admin contracts component if present.

- [x] **Step 1: Locate the current selector**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
Get-ChildItem -Recurse sigcon-angular\src\app\features\admin\contratos -Filter "*.ts" | Select-String "OPS|tipo"
Get-ChildItem -Recurse sigcon-angular\src\app\features\admin\contratos -Filter "*.html" | Select-String "tipo|OPS"
```

- [x] **Step 2: Update model and option list**

Use the existing model style, but ensure the type accepts both values:

```typescript
export type TipoContrato = 'OPS' | 'PRO';
```

Expose these exact options in the admin contract form:

```typescript
readonly tiposContrato = [
  { label: 'OPS - Contrato de Prestacion de Servicios Profesionales', value: 'OPS' },
  { label: 'PRO - Contrato de Apoyo a la Gestion', value: 'PRO' }
];
```

- [x] **Step 3: Verify dropdown binding**

The form control must persist `tipo: 'OPS' | 'PRO'` through `ContratoRequest` without mapping display text into the payload.

- [x] **Step 4: Run frontend tests/build and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/core/models/contrato.model.ts sigcon-angular/src/app/features/admin/contratos
git commit -m "feat(i12-r4): expose PRO contract type in admin form"
```

---

## Task 3: R2 Backend - Editable `porcentajeEjecucion`

**Files:**
- Create: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/informe/PorcentajeEjecucionRequest.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformeController.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/InformeControllerVbTest.java` or create a focused controller test if this class is not suitable.

- [x] **Step 1: Create request DTO**

```java
package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PorcentajeEjecucionRequest {
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal porcentajeEjecucion;

    public BigDecimal getPorcentajeEjecucion() {
        return porcentajeEjecucion;
    }

    public void setPorcentajeEjecucion(BigDecimal porcentajeEjecucion) {
        this.porcentajeEjecucion = porcentajeEjecucion;
    }
}
```

- [x] **Step 2: Add service tests**

Cover:
- Updates in `EN_REVISION`.
- Updates in `EN_VISTO_BUENO`.
- Rejects `BORRADOR`.
- Rejects `APROBADO`.

The service validation must be state-based. Authorization stays in controller/security.

- [x] **Step 3: Implement service method**

```java
public InformeDetalleDto actualizarPorcentajeEjecucion(Long informeId, PorcentajeEjecucionRequest request) {
    Informe informe = findActiveInforme(informeId);
    if (informe.getEstado() != EstadoInforme.EN_REVISION
            && informe.getEstado() != EstadoInforme.EN_VISTO_BUENO) {
        throw new SigconBusinessException(
            ErrorCode.OPERACION_NO_PERMITIDA,
            "Solo se puede actualizar el porcentaje de ejecucion en estado EN_REVISION o EN_VISTO_BUENO",
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
    informe.setPorcentajeEjecucion(request.getPorcentajeEjecucion());
    return informeMapper.toDetalleDto(informeRepository.save(informe));
}
```

Adapt the return type only if existing service conventions require `void`; controller acceptance only needs a successful status.

- [x] **Step 4: Add controller endpoint**

```java
@PatchMapping("/{id}/porcentaje-ejecucion")
@PreAuthorize("hasAnyRole('REVISOR', 'ADMIN', 'ADMINISTRATIVO')")
@Operation(summary = "Actualiza el porcentaje de ejecucion acumulada")
public ResponseEntity<InformeDetalleDto> actualizarPorcentajeEjecucion(
    @PathVariable Long id,
    @Valid @RequestBody PorcentajeEjecucionRequest request
) {
    return ResponseEntity.ok(informeService.actualizarPorcentajeEjecucion(id, request));
}
```

- [x] **Step 5: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceTest,InformeControllerVbTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/informe/PorcentajeEjecucionRequest.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformeController.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web
git commit -m "feat(i12-r2): allow reviewers to edit execution percentage"
```

---

## Task 4: R2 Security - PDF Preview For ADMIN/VB

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`

- [x] **Step 1: Confirm actual PDF routes**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
Get-ChildItem -Recurse sigcon-backend\src\main\java -Filter "*.java" | Select-String "pdf-preview|/pdf|preview"
```

- [x] **Step 2: Add security tests**

Assert `ROLE_ADMIN` and `ROLE_ADMINISTRATIVO` can access the preview route while existing `REVISOR` and `SUPERVISOR` access remains unchanged.

- [x] **Step 3: Update both security configs**

Add `ADMIN` and `ADMINISTRATIVO` to the preview matcher only. Do not remove existing roles.

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=SigconBackendSecurityTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java
git commit -m "feat(i12-r2): allow admin PDF preview access"
```

---

## Task 5: R2 Frontend - Editable Percentage Field

**Files:**
- Modify: `sigcon-angular/src/app/core/services/informe.service.ts`
- Modify: `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`
- Modify tests for the service/component if present.

- [x] **Step 1: Add Angular service method**

```typescript
actualizarPorcentajeEjecucion(id: number, porcentajeEjecucion: number) {
  return this.http.patch<InformeDetalle>(`${this.baseUrl}/${id}/porcentaje-ejecucion`, { porcentajeEjecucion });
}
```

Use the existing `baseUrl` and `InformeDetalle` names from `informe.service.ts`.

- [x] **Step 2: Update detail component**

Editable condition:

```typescript
get puedeEditarPorcentajeEjecucion(): boolean {
  const estado = this.informe()?.estado;
  return (estado === 'EN_REVISION' || estado === 'EN_VISTO_BUENO')
    && (this.authService.hasRole('REVISOR')
      || this.authService.hasRole('ADMIN')
      || this.authService.hasRole('ADMINISTRATIVO'));
}
```

Persist on blur:

```typescript
guardarPorcentajeEjecucion(valor: number | null | undefined): void {
  const informe = this.informe();
  if (!informe || valor == null) {
    return;
  }
  this.informeService.actualizarPorcentajeEjecucion(informe.id, valor).subscribe({
    next: actualizado => this.informe.set(actualizado),
    error: () => this.messageService.add({
      severity: 'error',
      summary: 'No se pudo actualizar',
      detail: 'El porcentaje de ejecucion no fue guardado.'
    })
  });
}
```

Adapt `this.informe()` / `this.informe.set()` to the existing component state style if it is not signal-based.

- [x] **Step 3: Update template**

Use `p-inputNumber` only when `puedeEditarPorcentajeEjecucion` is true; otherwise preserve the current read-only rendering.

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/core/services/informe.service.ts sigcon-angular/src/app/features/informes/detalle
git commit -m "feat(i12-r2): edit execution percentage from informe detail"
```

---

## Task 6: R3 Database And Event Model

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/enums/TipoEvento.java`
- Modify: `db/08_reset_datos_prueba.sql`
- Create: `db/09_i12_carga_informes.sql`

- [x] **Step 1: Add event enum**

```java
CARGA_INFORMES_DESACTIVADA
```

- [x] **Step 2: Create migration script**

`db/09_i12_carga_informes.sql`:

```sql
-- I12: control de carga de informes y evento de notificacion masiva.

MERGE INTO SGCN_PARAMETROS p
USING (SELECT 'CARGA_INFORMES_ACTIVA' CLAVE FROM DUAL) s
ON (p.CLAVE = s.CLAVE)
WHEN NOT MATCHED THEN
  INSERT (CLAVE, VALOR, DESCRIPCION)
  VALUES (
    'CARGA_INFORMES_ACTIVA',
    'true',
    'Habilita la creacion de nuevos informes por contratistas'
  );

-- Ajustar el nombre real del constraint si difiere en la BD local.
ALTER TABLE SGCN_NOTIFICACIONES DROP CONSTRAINT CHK_NOTIFICACIONES_EVENTO;

ALTER TABLE SGCN_NOTIFICACIONES ADD CONSTRAINT CHK_NOTIFICACIONES_EVENTO CHECK (
  TIPO_EVENTO IN (
    'INFORME_ENVIADO',
    'REVISION_APROBADA',
    'REVISION_DEVUELTA',
    'INFORME_APROBADO',
    'INFORME_DEVUELTO',
    'INFORME_EN_VISTO_BUENO',
    'VB_DADO',
    'VB_ESCALADO',
    'VB_DEVUELTO',
    'CARGA_INFORMES_DESACTIVADA'
  )
);

COMMIT;
```

- [x] **Step 3: Add seed to reset script**

Add the same `MERGE` for `CARGA_INFORMES_ACTIVA` to `db/08_reset_datos_prueba.sql` near other `SGCN_PARAMETROS` seeds.

- [x] **Step 4: Verify compile and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=NotificacionServiceTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/enums/TipoEvento.java db/08_reset_datos_prueba.sql db/09_i12_carga_informes.sql
git commit -m "feat(i12-r3): add carga informes event and database seed"
```

---

## Task 7: R3 Backend - Parameter Service, Notifications, And Admin Endpoint

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ParametroService.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/NotificacionService.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/EmailNotificacionService.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/repository/UsuarioRepository.java`
- Create: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/parametro/CargaInformesRequest.java`
- Create or modify: parameter admin controller under `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/` for `/api/admin/parametros`.
- Modify tests: `ParametroServiceTest`, `NotificacionServiceTest`, and a controller test if present.

- [x] **Step 1: Extend `ParametroService`**

Add:

```java
public static final String CARGA_INFORMES_ACTIVA = "CARGA_INFORMES_ACTIVA";
private static final String DESCRIPCION_CARGA_INFORMES =
    "Habilita la creacion de nuevos informes por contratistas";

@Transactional(readOnly = true)
public boolean isCargaInformesActiva() {
    return parametroRepository.findById(CARGA_INFORMES_ACTIVA)
        .map(SgcnParametro::getValor)
        .map(Boolean::parseBoolean)
        .orElse(true);
}

public boolean setCargaInformesActiva(boolean activo) {
    boolean anterior = isCargaInformesActiva();
    SgcnParametro parametro = parametroRepository.findById(CARGA_INFORMES_ACTIVA)
        .orElseGet(this::nuevoParametroCargaInformes);
    parametro.setValor(Boolean.toString(activo));
    parametro.setDescripcion(DESCRIPCION_CARGA_INFORMES);
    parametroRepository.save(parametro);
    return anterior;
}

private SgcnParametro nuevoParametroCargaInformes() {
    SgcnParametro parametro = new SgcnParametro();
    parametro.setClave(CARGA_INFORMES_ACTIVA);
    parametro.setDescripcion(DESCRIPCION_CARGA_INFORMES);
    return parametro;
}
```

- [x] **Step 2: Add request DTO**

```java
package co.gov.bogota.sed.sigcon.application.dto.parametro;

import javax.validation.constraints.NotNull;

public class CargaInformesRequest {
    @NotNull
    private Boolean activo;

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
```

- [x] **Step 3: Add repository support**

Add `List<Usuario> findByActivoTrue();` to `UsuarioRepository` if not already present.

- [x] **Step 4: Add bulk notification**

`NotificacionService.notificarBloqueoMasivo()` must:
- Fetch all active users.
- Create one internal notification per user with `TipoEvento.CARGA_INFORMES_DESACTIVADA`.
- Send one email per active user using the existing email service abstraction.
- Use message: `La carga de nuevos informes ha sido temporalmente deshabilitada por el administrador del sistema.`

- [x] **Step 5: Add parameter endpoint under `/api/admin/parametros`**

Use the existing admin-parameter controller if present; otherwise create one that matches the existing Angular `ParametroService.baseUrl`.

```java
@GetMapping("/carga-informes")
@PreAuthorize("hasRole('ADMIN')")
public Map<String, Boolean> obtenerCargaInformes() {
    return Collections.singletonMap("activo", parametroService.isCargaInformesActiva());
}

@PutMapping("/carga-informes")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map<String, Boolean>> setCargaInformes(@Valid @RequestBody CargaInformesRequest request) {
    boolean anterior = parametroService.setCargaInformesActiva(request.getActivo());
    if (anterior && !request.getActivo()) {
        notificacionService.notificarBloqueoMasivo();
    }
    return ResponseEntity.ok(Collections.singletonMap("activo", request.getActivo()));
}
```

- [x] **Step 6: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=ParametroServiceTest,NotificacionServiceTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ParametroService.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/NotificacionService.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/EmailNotificacionService.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/repository/UsuarioRepository.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/parametro/CargaInformesRequest.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller sigcon-backend/src/test/java
git commit -m "feat(i12-r3): add carga informes parameter and bulk notification"
```

---

## Task 8: R3 Backend - Block `crearInforme`

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`

- [x] **Step 1: Inject `ParametroService` into `InformeService`**

Add it through the existing constructor pattern and update tests accordingly.

- [x] **Step 2: Block at the start of `crearInforme`**

```java
if (!parametroService.isCargaInformesActiva()) {
    throw new SigconBusinessException(
        ErrorCode.OPERACION_NO_PERMITIDA,
        "La creacion de nuevos informes esta temporalmente deshabilitada.",
        HttpStatus.LOCKED
    );
}
```

Place this before any expensive creation workflow. Preserve existing role/contract validation for the active case.

- [x] **Step 3: Add tests**

Cover:
- `crearInforme` throws `HttpStatus.LOCKED` when parameter is false.
- Existing happy-path creation still works when parameter is true.

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java
git commit -m "feat(i12-r3): block informe creation when disabled"
```

---

## Task 9: R3 Frontend - Admin Toggle And 423 Handling

**Files:**
- Modify: `sigcon-angular/src/app/core/services/parametro.service.ts`
- Modify: existing admin dashboard or parameter UI:
  - Start search at `sigcon-angular/src/app/features/admin/dashboard/admin-dashboard.component.ts`
- Modify: `sigcon-angular/src/app/features/informes/nuevo/informe-form.component.ts`

- [x] **Step 1: Extend Angular parameter service**

```typescript
export interface ParametroCargaInformes {
  activo: boolean;
}

obtenerCargaInformes() {
  return this.http.get<ParametroCargaInformes>(`${this.baseUrl}/carga-informes`);
}

setCargaInformesActiva(activo: boolean) {
  return this.http.put<ParametroCargaInformes>(`${this.baseUrl}/carga-informes`, { activo });
}
```

- [x] **Step 2: Add admin toggle**

Place the control in the existing admin dashboard/parameter area. Use `p-toggleButton` and `p-confirmDialog` if those modules are already imported; otherwise import the PrimeNG modules in the standalone component.

Behavior:
- Load current state on init.
- Confirm only when changing from true to false.
- Call `setCargaInformesActiva`.
- Show success/warn toast consistent with existing UI.

- [x] **Step 3: Handle 423 in informe creation form**

When `crearInforme` returns `err.status === 423`, show a `p-message` warning and do not navigate.

Text:

```typescript
this.mensajeBloqueo = 'La carga de nuevos informes esta temporalmente deshabilitada por el administrador.';
```

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/core/services/parametro.service.ts sigcon-angular/src/app/features/admin sigcon-angular/src/app/features/informes/nuevo/informe-form.component.ts
git commit -m "feat(i12-r3): add carga informes toggle and blocked form warning"
```

---

## Task 10: R1 Backend - `Usuario.esAdmin`

**Files:**
- Create: `db/10_i12_usuario_es_admin.sql`
- Modify: `db/08_reset_datos_prueba.sql`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Usuario.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/usuario/UsuarioRequest.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/usuario/UsuarioDto.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/UsuarioMapper.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/UsuarioService.java`
- Modify related usuario tests.

- [x] **Step 1: Create DB migration**

```sql
-- I12: dual CONTRATISTA+ADMIN profile flag.

ALTER TABLE SGCN_USUARIOS ADD ES_ADMIN NUMBER(1) DEFAULT 0 NOT NULL;

COMMENT ON COLUMN SGCN_USUARIOS.ES_ADMIN IS
  'I12: 1 si el usuario CONTRATISTA tambien tiene acceso ADMIN. Ignorado para otros roles.';

COMMIT;
```

- [x] **Step 2: Add reset seed compatibility**

In `db/08_reset_datos_prueba.sql`, include `ES_ADMIN` in user inserts and set a local dual contractor seed to `1` if the script has explicit test users.

- [x] **Step 3: Add field through entity/DTO/mapper**

Entity:

```java
@Column(name = "ES_ADMIN", nullable = false)
private Boolean esAdmin = false;
```

DTO/request:

```java
private Boolean esAdmin;
```

Mapper:

```java
dto.setEsAdmin(Boolean.TRUE.equals(usuario.getEsAdmin()));
```

Service apply request:

```java
usuario.setEsAdmin(usuario.getRol() == RolUsuario.CONTRATISTA
    && Boolean.TRUE.equals(request.getEsAdmin()));
```

This enforces the spec rule that the flag only applies to contractors.

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=UsuarioServiceTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add db/08_reset_datos_prueba.sql db/10_i12_usuario_es_admin.sql sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/domain/entity/Usuario.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/usuario/UsuarioRequest.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/usuario/UsuarioDto.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/mapper/UsuarioMapper.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/UsuarioService.java sigcon-backend/src/test/java
git commit -m "feat(i12-r1): add esAdmin flag to contractor users"
```

---

## Task 11: R1 Backend - Security Authorities

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`

- [x] **Step 1: Add authorities for DB-backed auth**

Where the authenticated user authorities are built, add `ROLE_ADMIN` when:

```java
usuario.getRol() == RolUsuario.CONTRATISTA && Boolean.TRUE.equals(usuario.getEsAdmin())
```

Keep `ROLE_CONTRATISTA`.

- [x] **Step 2: Add local-dev dual user**

In `DevSecurityConfig`, add:

```java
User.withUsername("dual@educacionbogota.edu.co")
    .password("{noop}dual123")
    .roles("CONTRATISTA", "ADMIN")
    .build()
```

- [x] **Step 3: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=SigconBackendSecurityTest"
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/SecurityConfig.java sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/config/DevSecurityConfig.java sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java
git commit -m "feat(i12-r1): grant admin authority to dual contractor users"
```

---

## Task 12: R1 Frontend - Auth Context And Guards

**Files:**
- Create: `sigcon-angular/src/app/core/auth/auth-context.service.ts`
- Modify: `sigcon-angular/src/app/core/auth/auth.service.ts`
- Modify: `sigcon-angular/src/app/core/auth/dev-session.service.ts`
- Modify: `sigcon-angular/src/app/core/auth/role.guard.ts`
- Modify: `sigcon-angular/src/app/core/models/usuario.model.ts`

- [x] **Step 1: Add `esAdmin` to user model**

```typescript
esAdmin?: boolean;
```

- [x] **Step 2: Create auth context service**

```typescript
import { Injectable, signal } from '@angular/core';

export type ActiveRole = 'CONTRATISTA' | 'ADMIN';

@Injectable({ providedIn: 'root' })
export class AuthContextService {
  readonly activeRole = signal<ActiveRole>('CONTRATISTA');

  initForUser(rol: string, esAdmin: boolean): void {
    if (rol === 'CONTRATISTA' && esAdmin) {
      this.activeRole.set('CONTRATISTA');
      return;
    }
    if (rol === 'ADMIN') {
      this.activeRole.set('ADMIN');
    }
  }

  setActiveRole(role: ActiveRole): void {
    this.activeRole.set(role);
  }

  matches(role: string): boolean {
    return role === this.activeRole();
  }
}
```

Adapt to Observable/BehaviorSubject only if existing auth code is not signal-based.

- [x] **Step 3: Initialize context after login/session load**

Call:

```typescript
this.authContextService.initForUser(user.rol, !!user.esAdmin);
```

from the place where `AuthService.currentUser` is set.

- [x] **Step 4: Update role guard**

For dual users, route decisions for `ADMIN` and `CONTRATISTA` must consult `AuthContextService.activeRole`; for non-dual users, preserve existing role behavior.

- [x] **Step 5: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/core/auth sigcon-angular/src/app/core/models/usuario.model.ts
git commit -m "feat(i12-r1): add active role context for dual users"
```

---

## Task 13: R1 Frontend - Topbar Selector And Sidebar Menu

**Files:**
- Modify: `sigcon-angular/src/app/shared/components/topbar/topbar.component.ts`
- Modify: `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`

- [x] **Step 1: Add dropdown imports to standalone topbar**

Add `FormsModule` and PrimeNG `DropdownModule` to `imports` in `TopbarComponent`.

- [x] **Step 2: Add role selector to inline template**

Show only for `user.rol === 'CONTRATISTA' && user.esAdmin`.

```html
<p-dropdown
  *ngIf="user.rol === 'CONTRATISTA' && user.esAdmin"
  [options]="roleOptions"
  [ngModel]="authContext.activeRole()"
  optionLabel="label"
  optionValue="value"
  (onChange)="authContext.setActiveRole($event.value)"
  styleClass="min-w-40">
</p-dropdown>
```

- [x] **Step 3: Adapt sidebar computed menu**

`SidebarComponent.navItems` already uses a computed array. Change admin menu inclusion to:

```typescript
const user = this.authService.currentUser();
const activeRole = this.authContext.activeRole();
const isDual = user?.rol === 'CONTRATISTA' && !!user.esAdmin;
const isAdminMode = this.authService.hasRole('ADMIN') && (!isDual || activeRole === 'ADMIN');
const isContractorMode = this.authService.hasRole('CONTRATISTA') && (!isDual || activeRole === 'CONTRATISTA');
```

Then show contractor routes only in contractor mode and admin routes only in admin mode. Preserve `REVISOR`, `SUPERVISOR`, and `ADMINISTRATIVO` behavior.

- [x] **Step 4: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/shared/components/topbar/topbar.component.ts sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts
git commit -m "feat(i12-r1): add dual role selector to shell"
```

---

## Task 14: R1 Frontend - Admin Users Checkbox

**Files:**
- Modify: `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.ts`
- Modify: `sigcon-angular/src/app/features/admin/usuarios/admin-usuarios.component.html` if external template exists.
- Modify: `sigcon-angular/src/app/core/services/usuario.service.ts` only if request typing blocks `esAdmin`.

- [x] **Step 1: Add form control**

Add `esAdmin: [false]` to the user form.

- [x] **Step 2: Reset `esAdmin` when role is not `CONTRATISTA`**

Subscribe to role changes or normalize before submit:

```typescript
const payload = {
  ...this.form.getRawValue(),
  esAdmin: this.form.value.rol === 'CONTRATISTA' && !!this.form.value.esAdmin
};
```

- [x] **Step 3: Add checkbox**

```html
<p-checkbox
  *ngIf="form.get('rol')?.value === 'CONTRATISTA'"
  formControlName="esAdmin"
  [binary]="true"
  inputId="esAdmin"
  label="Tambien es Admin">
</p-checkbox>
```

- [x] **Step 4: Add table indicator**

Show a small badge/tag when `usuario.rol === 'CONTRATISTA' && usuario.esAdmin`.

- [x] **Step 5: Verify and commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/features/admin/usuarios sigcon-angular/src/app/core/services/usuario.service.ts
git commit -m "feat(i12-r1): manage contractor admin flag in users UI"
```

---

## Task 15: Final Verification And Execution Log

**Files:**
- Modify: `docs/plans/2026-05-25-sigcon-i12-plan.md`
- Create: `docs/plans/2026-05-25-sigcon-i12-execution-log.md`

- [x] **Step 1: Run backend full suite**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

Expected: `BUILD SUCCESS`, 0 failures.

- [x] **Step 2: Run frontend tests/build**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Expected: tests pass and production build completes.

- [x] **Step 3: Manual local-dev verification**

Use `dual@educacionbogota.edu.co / dual123` in local-dev if `DevSecurityConfig` is active.

Verify:
- Dual user starts in CONTRATISTA mode.
- Topbar selector appears only for dual user.
- Sidebar changes between contractor and admin menus.
- Non-dual users do not see selector.
- `% ejecucion` is editable for REVISOR/ADMIN in `EN_REVISION` and `EN_VISTO_BUENO`.
- PDF preview works for ADMIN/VB.
- Admin can deactivate carga informes and active users receive internal notification + email attempt.
- Contractor receives warning when trying to create informe while disabled.
- OPS and PRO PDF headers render the expected text.

- [x] **Step 4: Create execution log**

Create `docs/plans/2026-05-25-sigcon-i12-execution-log.md` using `docs/plans/2026-05-22-sigcon-i11-execution-log.md` as format reference. Include:
- Increment scope.
- Task-by-task completion status.
- Commit SHA per task.
- Backend test result.
- Frontend test/build result.
- DB scripts created/applied.
- Manual verification notes.
- Known residual risks, if any.

- [x] **Step 5: Commit closing docs**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add docs/plans/2026-05-25-sigcon-i12-plan.md docs/plans/2026-05-25-sigcon-i12-execution-log.md
git commit -m "docs(i12): close execution plan and log"
```

---

## Spec Coverage Self-Review

- R1 dual profile: covered by Tasks 10-14, including DB, authorities, frontend active role, topbar, sidebar, guards, and admin users checkbox.
- R2 editable percentage and ADMIN/VB preview: covered by Tasks 3-5.
- R3 carga informes parameter, notification, mail, backend block, admin toggle, and 423 UI: covered by Tasks 6-9.
- R4 `PRO` type and PDF/admin selector: covered by Tasks 1-2.
- AC-19 final backend test suite: covered by Task 15.

## Execution Recommendation

Use `superpowers:subagent-driven-development` if multiple agents are available because R1, R2, R3, and R4 are mostly independent. Otherwise execute inline in this order: R4 first, R2 second, R3 third, R1 last, then final verification/log.
