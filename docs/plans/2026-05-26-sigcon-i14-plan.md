# SIGCON I14 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corregir el campo `% Ejecución Acumulada` para que sea editable en los estados y roles correctos, corregir la acción `escalar` para que mueva el informe a `ENVIADO` (Revisor) en lugar de `EN_REVISION` (Supervisor), e integrar la vista completa del informe con panel de acciones para el actor ADMINISTRATIVO en estado `EN_VISTO_BUENO`.

**Architecture:** Tres correcciones quirúrgicas independientes: (1) backend valida 4 estados en lugar de 2, controller amplía roles y valida compatibilidad rol-estado; (2) `InformeEstadoService.escalar` cambia una línea de estado destino; (3) `informe-detalle.component.ts/.html` añade señales y métodos VB, reutiliza el bloque de actividades solo-lectura existente en el mismo template. No se crea ningún componente ni ruta nueva.

**Tech Stack:** Java 8 · Spring Boot 2.7.18 · Spring Security 5 · Oracle 19c · JUnit 5 · Mockito · Angular 20 · PrimeNG 20 · TypeScript.

**Spec:** `docs/specs/2026-05-26-sigcon-i14-spec.md`

---

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `sigcon-backend/.../application/service/InformeService.java` | Ampliar validación de estado en `actualizarPorcentajeEjecucion` |
| `sigcon-backend/.../web/controller/InformeController.java` | Añadir `CONTRATISTA` a `@PreAuthorize` y validar rol-estado |
| `sigcon-backend/.../application/service/InformeEstadoService.java` | Cambiar estado destino de `escalar` a `ENVIADO` |
| `sigcon-backend/.../application/InformeServiceTest.java` | Convertir tests de BORRADOR/ENVIADO de "falla" a "pasa", añadir nuevos negativos |
| `sigcon-backend/.../application/InformeServiceVbAccionesTest.java` | Actualizar assertion de `escalar` a `ENVIADO` |
| `sigcon-angular/.../informes/detalle/informe-detalle.component.ts` | Actualizar `puedeEditarPorcentajeEjecucion`, añadir señales y métodos VB |
| `sigcon-angular/.../informes/detalle/informe-detalle.component.html` | Añadir sección VB con actividades readonly, docs requeridos readonly y panel de acciones |

---

## Task 1: Backend — Ampliar `actualizarPorcentajeEjecucion` (estados + validación rol-estado)

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformeController.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`

- [ ] **Step 1: Actualizar tests — convertir negativos a positivos y añadir nuevos negativos**

En `InformeServiceTest.java`, localizar los dos tests que actualmente esperan fallo en BORRADOR y ENVIADO y reemplazarlos, además añadir casos nuevos:

```java
@Test
void actualizarPorcentajeEjecucionBorradorActualizaValor() {
    Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
    when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
    when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
    when(actividadRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
    when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
    when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(50L)).thenReturn(Collections.emptyList());
    when(aporteSgssiRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());

    InformeDetalleDto result = informeService.actualizarPorcentajeEjecucion(50L, porcentajeRequest("40.00"));

    assertThat(result.getPorcentajeEjecucion()).isEqualByComparingTo("40.00");
    verify(informeRepository).save(informe);
}

@Test
void actualizarPorcentajeEjecucionEnviadoActualizaValor() {
    Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.ENVIADO);
    when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
    when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
    when(actividadRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
    when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
    when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(50L)).thenReturn(Collections.emptyList());
    when(aporteSgssiRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());

    InformeDetalleDto result = informeService.actualizarPorcentajeEjecucion(50L, porcentajeRequest("55.00"));

    assertThat(result.getPorcentajeEjecucion()).isEqualByComparingTo("55.00");
    verify(informeRepository).save(informe);
}

@Test
void actualizarPorcentajeEjecucionAprobadoFalla() {
    Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.APROBADO);
    when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));

    assertThatThrownBy(() -> informeService.actualizarPorcentajeEjecucion(50L, porcentajeRequest("40.00")))
        .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPERACION_NO_PERMITIDA));
}

@Test
void actualizarPorcentajeEjecucionDevueltoFalla() {
    Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.DEVUELTO);
    when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));

    assertThatThrownBy(() -> informeService.actualizarPorcentajeEjecucion(50L, porcentajeRequest("40.00")))
        .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPERACION_NO_PERMITIDA));
}
```

Eliminar los dos métodos anteriores `actualizarPorcentajeEjecucionBorradorFalla` y `actualizarPorcentajeEjecucionAprobadoFalla` (este último se reemplaza por el nuevo con APROBADO y el nuevo DEVUELTO).

- [ ] **Step 2: Verificar que los nuevos tests fallan (RED)**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceTest#actualizarPorcentajeEjecucionBorradorActualizaValor+actualizarPorcentajeEjecucionEnviadoActualizaValor"
```

Esperado: FAIL — el servicio lanza `SigconBusinessException` porque actualmente BORRADOR y ENVIADO están rechazados.

- [ ] **Step 3: Actualizar `actualizarPorcentajeEjecucion` en `InformeService.java`**

Reemplazar el bloque de validación de estado:

```java
// Antes:
if (informe.getEstado() != EstadoInforme.EN_REVISION
    && informe.getEstado() != EstadoInforme.EN_VISTO_BUENO) {
    throw new SigconBusinessException(
        ErrorCode.OPERACION_NO_PERMITIDA,
        "Solo se puede actualizar el porcentaje de ejecucion en estado EN_REVISION o EN_VISTO_BUENO",
        HttpStatus.UNPROCESSABLE_ENTITY
    );
}

// Después:
EstadoInforme estado = informe.getEstado();
if (estado != EstadoInforme.BORRADOR
        && estado != EstadoInforme.ENVIADO
        && estado != EstadoInforme.EN_REVISION
        && estado != EstadoInforme.EN_VISTO_BUENO) {
    throw new SigconBusinessException(
        ErrorCode.OPERACION_NO_PERMITIDA,
        "El porcentaje de ejecucion no puede modificarse en el estado actual del informe.",
        HttpStatus.UNPROCESSABLE_ENTITY
    );
}
```

- [ ] **Step 4: Actualizar `@PreAuthorize` y añadir validación rol-estado en `InformeController.java`**

Localizar el endpoint `PATCH /{id}/porcentaje-ejecucion` y reemplazarlo:

```java
@PatchMapping("/{id}/porcentaje-ejecucion")
@PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'ADMIN', 'ADMINISTRATIVO')")
@Operation(summary = "Actualiza el porcentaje de ejecucion acumulada")
public InformeDetalleDto actualizarPorcentajeEjecucion(
    @PathVariable Long id,
    @Valid @RequestBody PorcentajeEjecucionRequest request,
    Authentication authentication
) {
    validarRolEstadoPorcentaje(authentication, informeService.findActiveInforme(id).getEstado());
    return informeService.actualizarPorcentajeEjecucion(id, request);
}

private void validarRolEstadoPorcentaje(Authentication auth, EstadoInforme estado) {
    boolean ok = false;
    if (estado == EstadoInforme.BORRADOR && hasRole(auth, "CONTRATISTA")) ok = true;
    if (estado == EstadoInforme.ENVIADO && hasRole(auth, "REVISOR")) ok = true;
    if (estado == EstadoInforme.EN_REVISION
            && (hasRole(auth, "REVISOR") || hasRole(auth, "ADMIN") || hasRole(auth, "ADMINISTRATIVO"))) ok = true;
    if (estado == EstadoInforme.EN_VISTO_BUENO
            && (hasRole(auth, "ADMIN") || hasRole(auth, "ADMINISTRATIVO"))) ok = true;
    if (!ok) {
        throw new SigconBusinessException(
            ErrorCode.ACCESO_DENEGADO,
            "Su rol no puede modificar el porcentaje en el estado actual del informe.",
            HttpStatus.FORBIDDEN
        );
    }
}
```

Añadir al inicio del archivo el import necesario si no existe:
```java
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
```

- [ ] **Step 5: Verificar GREEN**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceTest"
```

Esperado: BUILD SUCCESS, todos los tests de `InformeServiceTest` pasan.

- [ ] **Step 6: Commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformeController.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java
git commit -m "fix(i14-r1): ampliar estados y roles para edicion de porcentaje ejecucion"
```

---

## Task 2: Backend — Corregir `escalar` para mover a `ENVIADO`

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeEstadoService.java`
- Modify: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceVbAccionesTest.java`

- [ ] **Step 1: Actualizar el test de `escalar` para esperar `ENVIADO` (RED)**

En `InformeServiceVbAccionesTest.java`, localizar `escalar_persisteObservacionConAccionEscalacion` y cambiar la assertion:

```java
@Test
void escalar_persisteObservacionConAccionEscalacion() {
    Informe informe = informeEnVistoBueno();
    when(informeService.findActiveInforme(50L)).thenReturn(informe);
    ArgumentCaptor<String> accionCaptor = ArgumentCaptor.forClass(String.class);
    when(observacionService.registrarConAccion(
            eq(informe), eq(RolObservacion.ADMINISTRATIVO), eq("Escalo al revisor"), accionCaptor.capture()))
        .thenReturn(new Observacion());
    when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
    when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

    service.escalar(50L, "Escalo al revisor");

    assertThat(informe.getEstado()).isEqualTo(EstadoInforme.ENVIADO);
    assertThat(accionCaptor.getValue()).isEqualTo("ESCALACION");
}
```

- [ ] **Step 2: Verificar RED**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceVbAccionesTest#escalar_persisteObservacionConAccionEscalacion"
```

Esperado: FAIL — el estado actual es `EN_REVISION`, la assertion espera `ENVIADO`.

- [ ] **Step 3: Corregir `escalar` en `InformeEstadoService.java`**

Localizar el método `escalar` y cambiar la línea de estado destino:

```java
// Antes:
informe.setEstado(EstadoInforme.EN_REVISION);
// Después:
informe.setEstado(EstadoInforme.ENVIADO);
```

El resto del método (observación con acción ESCALACION, evento VB_ESCALADO) se mantiene sin cambios.

- [ ] **Step 4: Verificar GREEN**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceVbAccionesTest"
```

Esperado: BUILD SUCCESS, todos los tests de la suite VB pasan.

- [ ] **Step 5: Suite completa backend**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 6: Commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeEstadoService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceVbAccionesTest.java
git commit -m "fix(i14-r2): escalar mueve informe a ENVIADO en lugar de EN_REVISION"
```

---

## Task 3: Frontend — Corregir `puedeEditarPorcentajeEjecucion`

**Files:**
- Modify: `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`

- [ ] **Step 1: Reemplazar `puedeEditarPorcentajeEjecucion` en el componente**

Localizar la función en la línea ~147 y reemplazarla por:

```typescript
puedeEditarPorcentajeEjecucion(informe: InformeDetalle): boolean {
  const e = informe.estado;
  if (e === 'BORRADOR') return this.authService.hasRole('CONTRATISTA');
  if (e === 'ENVIADO') return this.authService.hasRole('REVISOR');
  if (e === 'EN_REVISION') return this.authService.hasRole('REVISOR')
    || this.authService.hasRole('ADMIN')
    || this.authService.hasRole('ADMINISTRATIVO');
  if (e === 'EN_VISTO_BUENO') return this.authService.hasRole('ADMIN')
    || this.authService.hasRole('ADMINISTRATIVO');
  return false;
}
```

- [ ] **Step 2: Verificar tests Angular**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/features/informes/detalle/informe-detalle.component.spec.ts
```

Esperado: todos los specs del componente detalle pasan.

- [ ] **Step 3: Commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts
git commit -m "fix(i14-r1): corregir logica puedeEditarPorcentajeEjecucion por rol y estado"
```

---

## Task 4: Frontend — Añadir señales y métodos VB al componente detalle

**Files:**
- Modify: `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`

- [ ] **Step 1: Añadir señales VB después del bloque de señales de supervisor (línea ~80)**

Insertar tras el bloque `procesandoSupervisor` / `errorSupervisor`:

```typescript
// I14 — Acciones Visto Bueno (ADMIN / ADMINISTRATIVO)
readonly dialogoVB = signal<'DAR_VB' | 'ESCALAR' | 'DEVOLVER' | null>(null);
readonly observacionVB = signal('');
readonly procesandoVB = signal(false);
readonly errorVB = signal('');
```

- [ ] **Step 2: Añadir función `puedeActuarVB` después de `puedeAprobarSupervisor`**

```typescript
puedeActuarVB(informe: InformeDetalle): boolean {
  return informe.estado === 'EN_VISTO_BUENO'
    && (this.authService.hasRole('ADMIN') || this.authService.hasRole('ADMINISTRATIVO'));
}
```

- [ ] **Step 3: Añadir métodos de apertura/cierre de diálogo VB**

```typescript
abrirDialogoVB(accion: 'DAR_VB' | 'ESCALAR' | 'DEVOLVER'): void {
  this.observacionVB.set('');
  this.errorVB.set('');
  this.dialogoVB.set(accion);
}

cerrarDialogoVB(): void {
  this.dialogoVB.set(null);
}
```

- [ ] **Step 4: Añadir método `confirmarAccionVB`**

```typescript
confirmarAccionVB(): void {
  const informe = this.informe();
  if (!informe) return;
  const accion = this.dialogoVB();
  const obs = this.observacionVB().trim();

  if ((accion === 'ESCALAR' || accion === 'DEVOLVER') && !obs) {
    this.errorVB.set('La observacion es obligatoria para esta accion.');
    return;
  }

  this.procesandoVB.set(true);
  this.errorVB.set('');

  const peticion$ = accion === 'DAR_VB'
    ? this.informeService.darVistoBueno(informe.id, obs || undefined)
    : accion === 'ESCALAR'
      ? this.informeService.escalar(informe.id, obs)
      : this.observacionService.devolverInforme(informe.id, { texto: obs });

  peticion$.subscribe({
    next: (actualizado) => {
      this.informe.set(actualizado);
      this.procesandoVB.set(false);
      this.dialogoVB.set(null);
    },
    error: () => {
      this.procesandoVB.set(false);
      this.errorVB.set('No se pudo completar la accion. Intente de nuevo.');
    }
  });
}
```

- [ ] **Step 5: Verificar que compila**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Esperado: BUILD SUCCESS sin errores de TypeScript.

- [ ] **Step 6: Commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts
git commit -m "feat(i14-r3): anadir senales y metodos VB al componente detalle"
```

---

## Task 5: Frontend — Template: sección VB con vista completa y panel de acciones

**Files:**
- Modify: `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.html`

- [ ] **Step 1: Añadir bloque VB de actividades readonly justo antes del cierre del `@if (informe(); as i)`**

Localizar la línea `</div>` que cierra `<div class="grid grid-cols-1 gap-lg lg:grid-cols-3">` (aprox. línea 582) e insertar antes del `}` de cierre del bloque `@if (informe(); as i)`:

```html
<!-- I14: Vista completa + panel de acciones para ADMINISTRATIVO en EN_VISTO_BUENO -->
@if (puedeActuarVB(i)) {
  <section class="mt-lg space-y-lg" data-testid="seccion-vb">

    <!-- Actividades solo lectura -->
    <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
      <div class="mb-md flex items-center gap-sm">
        <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
        <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Actividades reportadas</h3>
      </div>
      <div class="space-y-md">
        @for (actividad of i.actividades; track actividad.id) {
          <article class="rounded-lg border border-[var(--color-outline-variant)] bg-[var(--color-surface-container-low)] p-md">
            <div class="mb-sm flex items-start gap-sm">
              <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-bold text-white">
                {{ actividad.ordenObligacion ?? '-' }}
              </span>
              <div>
                <p class="m-0 text-sm font-semibold text-[var(--color-on-surface)]">{{ actividad.descripcionObligacion ?? 'Obligacion contractual' }}</p>
                <p class="m-0 mt-xs text-sm text-[var(--color-on-surface-variant)]">{{ actividad.descripcion }}</p>
              </div>
            </div>
            @if (actividad.soportes.length > 0) {
              <div class="mt-sm flex flex-wrap gap-xs">
                @for (soporte of actividad.soportes; track soporte.id) {
                  @if (soporte.tipo === 'URL') {
                    <a class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs font-semibold text-[var(--color-primary)] hover:underline"
                      [href]="soporte.referencia" target="_blank" rel="noopener noreferrer">
                      {{ soporte.nombre }} · Abrir
                    </a>
                  } @else {
                    <span class="rounded border border-[var(--color-outline-variant)] bg-white px-sm py-xs text-xs text-[var(--color-on-surface)]">
                      {{ soporte.nombre }} · {{ soporte.tipo }}
                    </span>
                  }
                }
              </div>
            }
          </article>
        }
      </div>
    </div>

    <!-- Documentos requeridos solo lectura -->
    @if (documentosRequeridos().length > 0) {
      <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
        <div class="mb-md flex items-center gap-sm">
          <span class="h-5 w-1 rounded-full bg-[var(--color-primary)]"></span>
          <h3 class="m-0 text-base font-semibold text-[var(--color-on-surface)]">Documentos requeridos</h3>
        </div>
        <div class="space-y-sm">
          @for (doc of documentosRequeridos(); track doc.claveLogica) {
            <div class="flex items-center justify-between rounded-lg border border-[var(--color-outline-variant)] p-sm">
              <span class="text-sm text-[var(--color-on-surface)]">{{ doc.nombre }}</span>
              @if (doc.adjuntoUrl) {
                <a class="text-xs font-semibold text-[var(--color-primary)] hover:underline"
                  [href]="doc.adjuntoUrl" target="_blank" rel="noopener noreferrer">Ver adjunto</a>
              } @else {
                <span class="text-xs text-[var(--color-on-surface-variant)]">Sin adjunto</span>
              }
            </div>
          }
        </div>
      </div>
    }

    <!-- Panel de acciones VB -->
    <div class="rounded-xl border border-[var(--color-outline-variant)] bg-white p-lg">
      <h3 class="mb-md text-base font-semibold text-[var(--color-on-surface)]">Decisión de Visto Bueno</h3>
      <div class="flex flex-wrap gap-sm">
        <button
          class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
          type="button"
          [disabled]="procesandoVB()"
          data-testid="btn-dar-vb"
          (click)="abrirDialogoVB('DAR_VB')"
        >
          Dar Visto Bueno
        </button>
        <button
          class="rounded border border-[var(--color-accent,#f95000)] px-md py-sm text-sm font-semibold text-[var(--color-accent,#f95000)] disabled:opacity-50"
          type="button"
          [disabled]="procesandoVB()"
          data-testid="btn-escalar-revisor"
          (click)="abrirDialogoVB('ESCALAR')"
        >
          Escalar al Revisor
        </button>
        <button
          class="rounded border border-[var(--color-error)] px-md py-sm text-sm font-semibold text-[var(--color-error)] disabled:opacity-50"
          type="button"
          [disabled]="procesandoVB()"
          data-testid="btn-devolver-vb"
          (click)="abrirDialogoVB('DEVOLVER')"
        >
          Devolver al Contratista
        </button>
      </div>
    </div>

  </section>
}
```

- [ ] **Step 2: Añadir diálogo modal VB al final del archivo (después del último `@if (dialogoDevolucionSupervisor())` )**

```html
@if (dialogoVB()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--color-on-surface)]/40">
    <div class="w-full max-w-lg rounded-xl bg-white p-lg shadow-xl">
      <h2 class="m-0 text-lg font-bold text-[var(--color-on-surface)]">
        @if (dialogoVB() === 'DAR_VB') { Dar Visto Bueno }
        @if (dialogoVB() === 'ESCALAR') { Escalar al Revisor }
        @if (dialogoVB() === 'DEVOLVER') { Devolver al Contratista }
      </h2>
      <p class="mt-xs text-sm text-[var(--color-on-surface-variant)]">
        @if (dialogoVB() === 'DAR_VB') { El informe avanzará a revisión del Supervisor. La observación es opcional. }
        @if (dialogoVB() === 'ESCALAR') { El informe volverá a la cola del Revisor para re-revisión. La observación es obligatoria. }
        @if (dialogoVB() === 'DEVOLVER') { El informe se devolverá al Contratista para corrección. La observación es obligatoria. }
      </p>

      <label class="mt-md block">
        <span class="mb-xs block text-xs font-bold uppercase tracking-wider text-[var(--color-on-surface-variant)]">
          Observación
          @if (dialogoVB() === 'ESCALAR' || dialogoVB() === 'DEVOLVER') {
            <span class="text-[var(--color-error)]"> *</span>
          }
        </span>
        <textarea
          class="min-h-28 w-full resize-y rounded border border-[var(--color-outline-variant)] px-sm py-xs text-sm"
          placeholder="Escriba su observación..."
          data-testid="textarea-observacion-vb"
          [ngModel]="observacionVB()"
          (ngModelChange)="observacionVB.set($event)"
        ></textarea>
        @if (errorVB()) {
          <p class="mt-xs text-xs text-[var(--color-error)]" data-testid="error-vb">{{ errorVB() }}</p>
        }
      </label>

      <div class="mt-md flex justify-end gap-sm">
        <button
          class="rounded border border-[var(--color-outline-variant)] px-md py-sm text-sm font-semibold text-[var(--color-on-surface)]"
          type="button"
          (click)="cerrarDialogoVB()"
        >
          Cancelar
        </button>
        <button
          class="rounded bg-[var(--color-primary)] px-md py-sm text-sm font-semibold text-white disabled:opacity-50"
          type="button"
          [disabled]="procesandoVB()"
          data-testid="btn-confirmar-accion-vb"
          (click)="confirmarAccionVB()"
        >
          {{ procesandoVB() ? 'Procesando...' : 'Confirmar' }}
        </button>
      </div>
    </div>
  </div>
}
```

- [ ] **Step 3: Verificar build**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Esperado: BUILD SUCCESS sin errores de compilación.

- [ ] **Step 4: Commit**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.html
git commit -m "feat(i14-r3): vista VB completa con actividades, documentos y panel de acciones"
```

---

## Task 6: Verificación final y cierre

**Files:**
- Create: `docs/plans/2026-05-26-sigcon-i14-execution-log.md`

- [ ] **Step 1: Suite completa backend**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

Esperado: BUILD SUCCESS, 0 failures.

- [ ] **Step 2: Suite completa Angular**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

Esperado: todos los specs pasan.

- [ ] **Step 3: Build Angular producción**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4: Crear execution log**

Crear `docs/plans/2026-05-26-sigcon-i14-execution-log.md` con la tabla de tareas, commits y resultados de verificación.

- [ ] **Step 5: Commit de cierre y push**

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add docs/plans/2026-05-26-sigcon-i14-execution-log.md
git commit -m "docs(i14): execution log y cierre de iteracion"
git push origin main
```

---

## Checklist de verificación manual post-implementación

| Escenario | Usuario dev | Resultado esperado |
|-----------|-------------|-------------------|
| CONTRATISTA en BORRADOR ve campo % editable | `contratista@` | Campo activo |
| CONTRATISTA en ENVIADO ve campo % solo lectura | `contratista@` | Campo deshabilitado |
| REVISOR en ENVIADO ve campo % editable | `revisor@` | Campo activo |
| REVISOR en EN_REVISION ve campo % editable | `revisor@` | Campo activo |
| ADMINISTRATIVO en EN_VISTO_BUENO ve actividades + soportes | `admin@` | Sección visible |
| ADMINISTRATIVO en EN_VISTO_BUENO ve docs requeridos | `admin@` | Sección visible si hay docs |
| Botón "Dar Visto Bueno" avanza a EN_REVISION | `admin@` | Estado cambia |
| Botón "Escalar al Revisor" requiere obs y mueve a ENVIADO | `admin@` | Informe en cola Revisor |
| Botón "Devolver al Contratista" requiere obs y mueve a DEVUELTO | `admin@` | Estado DEVUELTO |
