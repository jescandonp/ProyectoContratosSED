# Plan de Implementación — SIGCON Incremento 5
## Edición de Actividades en Informe BORRADOR desde Vista de Detalle

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Versión:** 1.0 — **Fecha:** 2026-05-04  
> **Spec de referencia:** `docs/specs/2026-05-04-sigcon-i5-spec.md`  
> **Rama:** `feat/sigcon-i5` (base: `feat/sigcon-i4` HEAD `7b61d09`)  
> **Estado:** Listo para ejecución

---

## Resumen Ejecutivo

Incremento de **3 tareas**, exclusivamente frontend. El backend no requiere cambios.

| Tarea | Scope | Descripción |
|-------|-------|-------------|
| T1 | Infraestructura | Crear rama `feat/sigcon-i5` |
| T2 | Frontend — lógica | Refactorizar `InformeDetalleComponent`: estado local de edición, inyección de servicios, métodos de guardar/eliminar |
| T3 | Frontend — template + tests | Actualizar template con modo edición inline, tests unitarios, guía de pruebas funcionales |

---

## T1 — Infraestructura: Crear Rama

**Archivos a modificar:** ninguno.

**Acciones:**
1. Crear rama `feat/sigcon-i5` desde HEAD de `feat/sigcon-i4` (`7b61d09`).

**Commit:** `chore: bootstrap I5 branch - informe-actividades-edicion`

**Validación:** `git log --oneline -1` muestra el commit base correcto.

---

## T2 — Frontend: Lógica del Componente

**Spec:** secciones 5.2, 5.3, 5.4

**Archivos a modificar:**
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts`

**Acciones:**

### 2.1 Agregar imports

```typescript
import { SlicePipe } from '@angular/common';
import { forkJoin, of, switchMap } from 'rxjs';
import { ActividadInformeService } from '../../../core/services/actividad-informe.service';
import { DocumentoAdicionalService } from '../../../core/services/documento-adicional.service';
import { DocumentoCatalogoService } from '../../../core/services/documento-catalogo.service';
import { SoporteAdjuntoService } from '../../../core/services/soporte-adjunto.service';
import { DocumentoCatalogo } from '../../../core/models/documento-catalogo.model';
```

### 2.2 Agregar interfaz local

```typescript
interface ActividadEditState {
  descripcion: string;
  porcentaje: number;
  guardando: boolean;
  error: string;
  soporteNombre: string;
  soporteUrl: string;
  soporteArchivo: File | null;
}
```

### 2.3 Agregar signals y estado

```typescript
readonly actividadStates = signal<Map<number, ActividadEditState>>(new Map());
readonly catalogoDocumentos = signal<DocumentoCatalogo[]>([]);
readonly nuevoDocIdCatalogo = signal<number | null>(null);
readonly nuevoDocReferencia = signal('');
readonly guardandoDocumento = signal(false);
readonly errorDocumento = signal('');
```

### 2.4 Inyectar servicios adicionales en el constructor

```typescript
constructor(
  private readonly informeService: InformeService,
  private readonly actividadService: ActividadInformeService,
  private readonly soporteService: SoporteAdjuntoService,
  private readonly documentoAdicionalService: DocumentoAdicionalService,
  private readonly documentoCatalogoService: DocumentoCatalogoService,
  private readonly route: ActivatedRoute,
  private readonly router: Router
) {}
```

### 2.5 Inicializar estado de edición al cargar

En el método `cargar()`, tras recibir el informe:

```typescript
if (informe.estado === 'BORRADOR') {
  this.inicializarEstadoEdicion(informe);
  this.cargarCatalogo();
}
```

```typescript
private inicializarEstadoEdicion(informe: InformeDetalle): void {
  const states = new Map<number, ActividadEditState>();
  informe.actividades.forEach((actividad) => {
    states.set(actividad.id, {
      descripcion: actividad.descripcion,
      porcentaje: actividad.porcentaje,
      guardando: false,
      error: '',
      soporteNombre: '',
      soporteUrl: '',
      soporteArchivo: null
    });
  });
  this.actividadStates.set(states);
}

private cargarCatalogo(): void {
  this.documentoCatalogoService.listar({ tipoContrato: 'OPS', size: 100 }).subscribe({
    next: (page) => this.catalogoDocumentos.set(page.content),
    error: () => { /* catálogo no crítico, silenciar */ }
  });
}
```

### 2.6 Métodos de edición de actividad

```typescript
actualizarEstadoActividad(actividadId: number, patch: Partial<ActividadEditState>): void {
  this.actividadStates.update((map) => {
    const current = map.get(actividadId);
    if (!current) return map;
    const next = new Map(map);
    next.set(actividadId, { ...current, ...patch });
    return next;
  });
}

guardarActividad(actividadId: number): void {
  const informe = this.informe();
  if (!informe) return;
  const state = this.actividadStates().get(actividadId);
  if (!state) return;

  // Validaciones frontend
  if (!state.descripcion.trim()) {
    this.actualizarEstadoActividad(actividadId, { error: 'La descripción no puede estar vacía.' });
    return;
  }
  if (state.porcentaje < 0 || state.porcentaje > 100) {
    this.actualizarEstadoActividad(actividadId, { error: 'El porcentaje debe estar entre 0 y 100.' });
    return;
  }

  const actividad = informe.actividades.find((a) => a.id === actividadId);
  if (!actividad) return;

  this.actualizarEstadoActividad(actividadId, { guardando: true, error: '' });

  this.actividadService.actualizar(informe.id, actividadId, {
    idObligacion: actividad.idObligacion ?? 0,
    descripcion: state.descripcion.trim(),
    porcentaje: state.porcentaje
  }).pipe(
    switchMap((actividadActualizada) => {
      const ops: Array<import('rxjs').Observable<unknown>> = [];
      const nombre = state.soporteNombre.trim() || `Soporte obligación ${actividad.ordenObligacion ?? actividadId}`;
      if (state.soporteUrl.trim()) {
        ops.push(this.soporteService.agregarUrl(actividadActualizada.id, { nombre, url: state.soporteUrl.trim() }));
      }
      if (state.soporteArchivo) {
        ops.push(this.soporteService.agregarArchivo(actividadActualizada.id, state.soporteArchivo));
      }
      return ops.length ? forkJoin(ops) : of([]);
    }),
    switchMap(() => this.informeService.obtenerDetalle(informe.id))
  ).subscribe({
    next: (actualizado) => {
      this.informe.set(actualizado);
      this.inicializarEstadoEdicion(actualizado);
    },
    error: () => {
      this.actualizarEstadoActividad(actividadId, {
        guardando: false,
        error: 'No se pudo guardar la actividad. Intente de nuevo.'
      });
    }
  });
}

eliminarSoporte(actividadId: number, soporteId: number): void {
  const informe = this.informe();
  if (!informe) return;
  this.soporteService.eliminar(actividadId, soporteId).pipe(
    switchMap(() => this.informeService.obtenerDetalle(informe.id))
  ).subscribe({
    next: (actualizado) => {
      this.informe.set(actualizado);
      this.inicializarEstadoEdicion(actualizado);
    },
    error: () => this.error.set('No se pudo eliminar el soporte.')
  });
}

seleccionarArchivoActividad(actividadId: number, event: Event): void {
  const input = event.target as HTMLInputElement;
  this.actualizarEstadoActividad(actividadId, { soporteArchivo: input.files?.item(0) ?? null });
}
```

### 2.7 Métodos de documentos adicionales

```typescript
agregarDocumentoAdicional(): void {
  const informe = this.informe();
  const idCatalogo = this.nuevoDocIdCatalogo();
  const referencia = this.nuevoDocReferencia().trim();
  if (!informe || !idCatalogo || !referencia) {
    this.errorDocumento.set('Seleccione el tipo de documento e ingrese la referencia.');
    return;
  }
  this.guardandoDocumento.set(true);
  this.errorDocumento.set('');
  this.documentoAdicionalService.agregar(informe.id, { idCatalogo, referencia }).pipe(
    switchMap(() => this.informeService.obtenerDetalle(informe.id))
  ).subscribe({
    next: (actualizado) => {
      this.informe.set(actualizado);
      this.inicializarEstadoEdicion(actualizado);
      this.nuevoDocIdCatalogo.set(null);
      this.nuevoDocReferencia.set('');
      this.guardandoDocumento.set(false);
    },
    error: () => {
      this.guardandoDocumento.set(false);
      this.errorDocumento.set('No se pudo agregar el documento. Intente de nuevo.');
    }
  });
}

eliminarDocumentoAdicional(documentoId: number): void {
  const informe = this.informe();
  if (!informe) return;
  this.documentoAdicionalService.eliminar(informe.id, documentoId).pipe(
    switchMap(() => this.informeService.obtenerDetalle(informe.id))
  ).subscribe({
    next: (actualizado) => {
      this.informe.set(actualizado);
      this.inicializarEstadoEdicion(actualizado);
    },
    error: () => this.error.set('No se pudo eliminar el documento.')
  });
}
```

### 2.8 Helper

```typescript
esBorrador(estado: EstadoInforme): boolean {
  return estado === 'BORRADOR';
}

toNumber(value: string | number): number {
  return Number(value) || 0;
}
```

**Commit:** `feat(i5): add edit state logic to InformeDetalleComponent`

**Validación:** `ng build` sin errores de compilación TypeScript.

---

## T3 — Frontend: Template + Tests + Documentación

**Spec:** secciones 5.2–5.5, 7, 8

**Archivos a modificar:**
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.ts` (template inline)
- `sigcon-angular/src/app/features/informes/detalle/informe-detalle.component.spec.ts`
- `docs/GUIA_PRUEBAS_FUNCIONALES.md`

### 3.1 Cambios en el template

Dentro del bloque `@for (actividad of i.actividades; track actividad.id)`, reemplazar el `<article>` actual por una bifurcación:

```
@if (esBorrador(i.estado)) {
  <!-- Tarjeta editable -->
  <article data-testid="actividad-editable-{actividad.id}">
    <!-- Cabecera: número de obligación + descripción de obligación -->
    <!-- Textarea: descripcion (ngModel → actividadStates) -->
    <!-- Input number: porcentaje (ngModel → actividadStates) -->
    <!-- Soportes existentes con botón × por cada uno -->
    <!-- Formulario agregar soporte: nombre, url, archivo -->
    <!-- Error inline + botón "Guardar actividad" con estado guardando -->
  </article>
} @else {
  <!-- Tarjeta solo lectura (código actual sin cambios) -->
  <article>...</article>
}
```

Para la sección de documentos adicionales, bifurcar igualmente:

```
@if (esBorrador(i.estado)) {
  <!-- Lista con botón × por documento + formulario agregar -->
} @else {
  <!-- Lista solo lectura (código actual) -->
}
```

**Atributos `data-testid` requeridos:**

| Elemento | data-testid |
|----------|-------------|
| Tarjeta actividad editable | `actividad-editable` |
| Textarea descripción | `input-descripcion-{actividadId}` |
| Input porcentaje | `input-porcentaje-{actividadId}` |
| Botón guardar actividad | `btn-guardar-actividad-{actividadId}` |
| Botón eliminar soporte | `btn-eliminar-soporte-{soporteId}` |
| Input URL soporte | `input-soporte-url-{actividadId}` |
| Input archivo soporte | `input-soporte-archivo-{actividadId}` |
| Botón eliminar documento | `btn-eliminar-documento-{documentoId}` |
| Botón agregar documento | `btn-agregar-documento` |
| Error inline actividad | `error-actividad-{actividadId}` |

### 3.2 Tests unitarios a agregar en `informe-detalle.component.spec.ts`

El `TestBed` debe incluir los nuevos servicios como spies:

```typescript
actividadService = jasmine.createSpyObj<ActividadInformeService>('ActividadInformeService', ['actualizar']);
soporteService = jasmine.createSpyObj<SoporteAdjuntoService>('SoporteAdjuntoService', ['agregarUrl', 'agregarArchivo', 'eliminar']);
documentoAdicionalService = jasmine.createSpyObj<DocumentoAdicionalService>('DocumentoAdicionalService', ['agregar', 'eliminar']);
documentoCatalogoService = jasmine.createSpyObj<DocumentoCatalogoService>('DocumentoCatalogoService', ['listar']);
```

**Tests a agregar:**

```
describe('modo edición BORRADOR', () => {
  it('muestra actividades en modo edición cuando estado es BORRADOR')
  it('no muestra controles de edición en estado ENVIADO')
  it('no muestra controles de edición en estado EN_REVISION')
  it('no muestra controles de edición en estado APROBADO')
  it('guarda actividad correctamente y recarga el informe')
  it('muestra error inline si descripción está vacía al guardar')
  it('muestra error inline si porcentaje es inválido')
  it('muestra indicador de carga durante guardado de actividad')
  it('elimina soporte y recarga el informe')
  it('agrega soporte URL y recarga el informe')
  it('agrega documento adicional y recarga el informe')
  it('elimina documento adicional y recarga el informe')
})
```

### 3.3 Guía de pruebas funcionales

Agregar sección 13 en `docs/GUIA_PRUEBAS_FUNCIONALES.md`:

```markdown
## Sección 13 — I5: Edición de Actividades en Informe BORRADOR

### Escenario 13.1 — Editar descripción de actividad
1. Autenticarse como CONTRATISTA.
2. Navegar a un informe en estado BORRADOR.
3. Verificar que las tarjetas de actividad muestran campos editables.
4. Modificar la descripción de una actividad.
5. Hacer clic en "Guardar actividad".
6. Verificar que la descripción actualizada se muestra en la tarjeta.

### Escenario 13.2 — Editar porcentaje de avance
1. En la misma pantalla, modificar el porcentaje de una actividad.
2. Guardar. Verificar que el porcentaje actualizado se muestra.

### Escenario 13.3 — Agregar soporte URL
1. En la tarjeta de una actividad, ingresar URL en el campo de soporte.
2. Guardar. Verificar que el soporte aparece en la lista de soportes.

### Escenario 13.4 — Eliminar soporte
1. Hacer clic en × junto a un soporte existente.
2. Verificar que el soporte desaparece de la lista.

### Escenario 13.5 — Agregar documento adicional
1. En la sección de documentos adicionales, seleccionar tipo y referencia.
2. Hacer clic en "Agregar". Verificar que aparece en la lista.

### Escenario 13.6 — Eliminar documento adicional
1. Hacer clic en × junto a un documento adicional.
2. Verificar que desaparece de la lista.

### Escenario 13.7 — Solo lectura en otros estados
1. Navegar a un informe en estado ENVIADO.
2. Verificar que NO aparecen campos editables ni botones de guardar.
3. Repetir con EN_REVISION y APROBADO.

### Diagnóstico de errores comunes I5

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| 403 al guardar actividad | Contratista no es propietario del informe | Verificar que el usuario autenticado es el contratista del contrato |
| 409 al guardar actividad | Informe no está en BORRADOR | Recargar la página; el estado puede haber cambiado |
| Campos no editables en BORRADOR | Bug en detección de estado | Verificar `informe.estado === 'BORRADOR'` en el componente |
```

**Commit:** `feat(i5): informe detalle edit template, tests and functional guide`

**Validación final:**
- `ng test --include="**/informe-detalle*"` → todos los specs pasan (>= 20 specs, 0 fallos)
- `ng build` → sin errores
- Revisión manual: flujo completo de edición de actividades en BORRADOR
- Verificar que DEVUELTO sigue redirigiendo a `CorregirInformeComponent` sin cambios

---

## Orden de Ejecución y Dependencias

```
T1 (rama)
  └─ T2 (lógica del componente — TypeScript)
       └─ T3 (template + tests + docs)
```

Las tareas son estrictamente lineales. T3 depende de T2 porque el template referencia los métodos y signals definidos en T2.

---

## Métricas de Cierre

| Métrica | Meta |
|---------|------|
| Backend tests | Sin regresión — 123 tests, 0 fallos |
| Frontend specs totales | >= 90, 0 fallos |
| Nuevos tests frontend I5 | >= 12 |
| Tests existentes InformeDetalleComponent | 8 pasan sin cambios |
| Endpoints nuevos | 0 |
| DDL changes | 0 |
| Regresión CorregirInformeComponent | Sin cambios, todos sus tests pasan |
