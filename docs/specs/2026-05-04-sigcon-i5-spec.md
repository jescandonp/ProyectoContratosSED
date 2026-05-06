# Spec Técnica — SIGCON Incremento 5
## Edición de Actividades en Informe BORRADOR desde Vista de Detalle

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Versión:** 1.0 — **Fecha:** 2026-05-04  
> **Constitución:** `docs/CONSTITUTION.md`  
> **Arquitectura:** `docs/ARCHITECTURE.md`  
> **PRD de referencia:** `docs/specs/2026-04-30-sigcon-prd.md`  
> **Specs base:** I1–I4 completados (HEAD `7b61d09`, rama `feat/sigcon-i4`)  
> **Feature name:** `informe-actividades-edicion`  
> **Estado:** Listo para implementación

---

## 1. Alcance del Incremento

### 1.1 Problema que resuelve

`InformeDetalleComponent` (`/informes/{id}`) muestra las actividades del informe siempre en modo solo lectura. Cuando el informe está en estado **BORRADOR**, el contratista propietario debería poder editar los campos de cada actividad directamente desde esa vista, sin navegar a otra pantalla.

### 1.2 Módulos incluidos

| Módulo | Descripción | Rol |
|--------|-------------|-----|
| Frontend — `InformeDetalleComponent` | Activar modo edición inline de actividades cuando `estado == BORRADOR` | CONTRATISTA |
| Frontend — soportes y documentos adicionales | Edición inline de soportes (agregar URL/archivo, eliminar) y documentos adicionales en BORRADOR | CONTRATISTA |

### 1.3 Fuera de este incremento

- Cambios en el backend (todos los endpoints necesarios ya existen desde I2–I4).
- Cambios de DDL.
- Modificación de la máquina de estados.
- Edición de actividades en estado DEVUELTO — ese flujo ya está cubierto por `CorregirInformeComponent`.
- Creación de nuevas actividades desde `InformeDetalleComponent` (las actividades se crean al crear el informe; la edición cubre las ya existentes).
- Cambios en `CorregirInformeComponent`.
- Cambios en `InformeFormComponent`.

### 1.4 Entregable de cierre

El contratista puede editar descripción, porcentaje, soportes y documentos adicionales de un informe en estado BORRADOR directamente desde `InformeDetalleComponent`, sin salir de la pantalla. Los cambios se persisten llamando a los endpoints existentes. En cualquier otro estado la vista permanece en solo lectura.

---

## 2. Coherencia con Incrementos Anteriores

- **Backend completo:** `ActividadInformeService.actualizar()`, `SoporteAdjuntoService.agregarUrl()`, `SoporteAdjuntoService.agregarArchivo()`, `SoporteAdjuntoService.eliminar()`, `DocumentoAdicionalService.agregar()`, `DocumentoAdicionalService.eliminar()` ya existen y están protegidos con `@PreAuthorize("hasRole('CONTRATISTA')")`.
- **`assertCanEditInforme`** ya valida que el usuario sea CONTRATISTA propietario y que el informe esté en BORRADOR o DEVUELTO. Para BORRADOR el backend ya acepta las llamadas.
- **`CorregirInformeComponent`** cubre el estado DEVUELTO con pantalla separada — no se toca.
- **`InformeFormComponent`** cubre la creación inicial — no se toca.
- No se requieren cambios de DDL ni de máquina de estados.
- Los servicios Angular `ActividadInformeService`, `SoporteAdjuntoService` y `DocumentoAdicionalService` ya tienen todos los métodos necesarios.

---

## 3. Base de Datos

**Sin cambios de DDL.** Toda la estructura de tablas necesaria existe desde I2.

---

## 4. Backend

**Sin cambios de backend.** Todos los endpoints requeridos existen y están correctamente protegidos:

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/informes/{informeId}/actividades/{actividadId}` | PUT | Actualiza descripción y porcentaje |
| `/api/actividades/{actividadId}/soportes/url` | POST | Agrega soporte URL |
| `/api/actividades/{actividadId}/soportes/archivo` | POST | Agrega soporte archivo |
| `/api/actividades/{actividadId}/soportes/{soporteId}` | DELETE | Elimina soporte |
| `/api/informes/{informeId}/documentos-adicionales` | POST | Agrega documento adicional |
| `/api/informes/{informeId}/documentos-adicionales/{documentoId}` | DELETE | Elimina documento adicional |

---

## 5. Frontend

### 5.1 Estrategia de diseño

`InformeDetalleComponent` adopta un modo dual:

- **Modo lectura** (comportamiento actual): actividades, soportes y documentos adicionales se muestran como texto estático. Aplica a todos los estados excepto BORRADOR.
- **Modo edición inline** (nuevo): cuando `informe.estado === 'BORRADOR'`, cada tarjeta de actividad expone campos editables directamente. Los cambios se guardan actividad por actividad con un botón "Guardar" por tarjeta. Los soportes y documentos adicionales también son editables.

No se introduce una pantalla separada ni un modo de edición global con barra de acciones fija. La edición es granular por actividad para minimizar el riesgo de pérdida de datos.

### 5.2 Cambios en `InformeDetalleComponent`

#### 5.2.1 Detección del modo edición

```typescript
esBorrador(estado: EstadoInforme): boolean {
  return estado === 'BORRADOR';
}
```

Solo BORRADOR activa el modo edición. DEVUELTO sigue usando `CorregirInformeComponent`.

#### 5.2.2 Estado local de edición de actividades

Se introduce una interfaz de estado local para cada actividad en edición:

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

El componente mantiene un `Map<number, ActividadEditState>` indexado por `actividad.id`, inicializado al cargar el informe cuando `estado === 'BORRADOR'`.

#### 5.2.3 Guardar actividad

Al hacer clic en "Guardar actividad" de una tarjeta:

1. Llamar `ActividadInformeService.actualizar(informeId, actividadId, { idObligacion, descripcion, porcentaje })`.
2. Si hay `soporteUrl` no vacía: llamar `SoporteAdjuntoService.agregarUrl(actividadId, { nombre, url })`.
3. Si hay `soporteArchivo`: llamar `SoporteAdjuntoService.agregarArchivo(actividadId, file)`.
4. Tras éxito: recargar el informe completo (`InformeService.obtenerDetalle(id)`) para reflejar los soportes actualizados.
5. Limpiar los campos de nuevo soporte (`soporteNombre`, `soporteUrl`, `soporteArchivo`) del estado local.

El guardado es por actividad individual, no global. Cada tarjeta tiene su propio indicador de carga y error.

#### 5.2.4 Eliminar soporte

Cada soporte existente muestra un botón de eliminar (icono ×) cuando `estado === 'BORRADOR'`. Al hacer clic:

1. Llamar `SoporteAdjuntoService.eliminar(actividadId, soporteId)`.
2. Tras éxito: recargar el informe.

#### 5.2.5 Documentos adicionales en BORRADOR

Cuando `estado === 'BORRADOR'`:

- Los documentos adicionales existentes muestran un botón de eliminar.
- Se muestra un formulario inline para agregar nuevo documento adicional (selector de catálogo + campo referencia).
- Al agregar: llamar `DocumentoAdicionalService.agregar(informeId, { idCatalogo, referencia })`.
- Al eliminar: llamar `DocumentoAdicionalService.eliminar(informeId, documentoId)`.
- Tras cada operación: recargar el informe.

#### 5.2.6 Validaciones en frontend

- `descripcion`: no puede estar vacía al guardar.
- `porcentaje`: debe estar entre 0 y 100 (entero).
- Si hay `soporteUrl`: debe ser una URL válida (no vacía, formato básico).
- Si hay `soporteNombre` sin `soporteUrl` ni `soporteArchivo`: ignorar el nombre (no es error).

#### 5.2.7 Servicios Angular requeridos

Los servicios ya existen. El componente debe inyectar:

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

`DocumentoCatalogoService` se usa para cargar el catálogo de documentos adicionales disponibles cuando `estado === 'BORRADOR'`.

#### 5.2.8 Imports Angular adicionales

```typescript
imports: [StatusChipComponent, FormsModule, SlicePipe]
```

`SlicePipe` ya está disponible en `@angular/common`.

### 5.3 Estructura de la tarjeta de actividad en modo BORRADOR

```
┌─────────────────────────────────────────────────────────────┐
│  [N]  Obligación N — Descripción de la obligación           │
│                                                             │
│  Actividad realizada *                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ textarea editable (descripcion)                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Avance %                                                   │
│  ┌──────────┐                                               │
│  │ 0–100    │                                               │
│  └──────────┘                                               │
│                                                             │
│  Soportes existentes:                                       │
│  [Evidencia · URL]  [×]   [Archivo.pdf · ARCHIVO]  [×]     │
│                                                             │
│  Agregar soporte:                                           │
│  Nombre: [___________]  URL: [___________]  Archivo: [___] │
│                                                             │
│  [error inline si aplica]                                   │
│  [Guardando... / Guardar actividad]                         │
└─────────────────────────────────────────────────────────────┘
```

### 5.4 Estructura de documentos adicionales en modo BORRADOR

```
┌─────────────────────────────────────────────────────────────┐
│  Documentos adicionales                                     │
│                                                             │
│  Planilla — DOC-301                          [×]            │
│                                                             │
│  Agregar documento:                                         │
│  Tipo: [Selector catálogo ▼]  Referencia: [___________]    │
│  [Agregar]                                                  │
└─────────────────────────────────────────────────────────────┘
```

### 5.5 Comportamiento en otros estados

En ENVIADO, EN_REVISION, DEVUELTO y APROBADO: el componente mantiene exactamente el comportamiento actual (solo lectura). No se muestra ningún control de edición.

---

## 6. Seguridad

No se requieren cambios de seguridad. El backend ya valida:

- Rol CONTRATISTA en todos los endpoints de actividades y soportes.
- Propiedad del informe (`assertCanEditInforme`) — un contratista no puede editar actividades de informes de otro contratista.
- Estado BORRADOR o DEVUELTO — el backend rechaza ediciones en otros estados.

El frontend solo muestra los controles de edición cuando `estado === 'BORRADOR'`, pero la validación de autoridad reside en el backend.

---

## 7. Tests Requeridos

### 7.1 Frontend — `InformeDetalleComponent`

| Test | Descripción |
|------|-------------|
| `muestra actividades en modo edición cuando estado es BORRADOR` | Los campos textarea y número son visibles y editables |
| `no muestra controles de edición en estado ENVIADO` | Solo lectura en ENVIADO |
| `no muestra controles de edición en estado EN_REVISION` | Solo lectura en EN_REVISION |
| `no muestra controles de edición en estado APROBADO` | Solo lectura en APROBADO |
| `guarda actividad correctamente y recarga el informe` | PUT actividad exitoso → recarga |
| `muestra error inline si descripción está vacía al guardar` | Validación frontend |
| `muestra error inline si porcentaje es inválido` | Porcentaje fuera de rango |
| `elimina soporte y recarga el informe` | DELETE soporte exitoso → recarga |
| `agrega soporte URL y recarga el informe` | POST soporte URL exitoso → recarga |
| `agrega documento adicional y recarga el informe` | POST documento adicional exitoso → recarga |
| `elimina documento adicional y recarga el informe` | DELETE documento adicional exitoso → recarga |
| `muestra indicador de carga durante guardado de actividad` | Signal `guardando` activo durante la llamada |

### 7.2 Regresión

Los tests existentes de `InformeDetalleComponent` deben seguir pasando sin modificación:

- `loads and renders the report detail`
- `navigates to the read-only preview`
- `shows PDF action for approved reports with generated PDF`
- `hides PDF action for approved reports without generated PDF`
- `confirms before sending a draft report`
- `muestra campos editables de periodo en estado BORRADOR`
- `no muestra campos editables de periodo en estado ENVIADO`
- `guarda el periodo correctamente y actualiza la vista`

---

## 8. Criterios de Aceptación

### Edición de actividades en BORRADOR

- [ ] Cuando el informe está en BORRADOR, cada tarjeta de actividad muestra campos editables (descripción y porcentaje).
- [ ] El contratista puede modificar la descripción de una actividad y guardar el cambio.
- [ ] El contratista puede modificar el porcentaje de avance de una actividad y guardar el cambio.
- [ ] Al guardar una actividad, la vista se actualiza con los datos persistidos.
- [ ] Si la descripción está vacía al intentar guardar, se muestra un error inline y no se realiza la llamada HTTP.
- [ ] Si el porcentaje está fuera del rango 0–100, se muestra un error inline y no se realiza la llamada HTTP.
- [ ] Cada tarjeta de actividad tiene su propio indicador de carga y error independiente.

### Edición de soportes en BORRADOR

- [ ] El contratista puede agregar un soporte URL a una actividad desde la vista de detalle.
- [ ] El contratista puede agregar un soporte archivo a una actividad desde la vista de detalle.
- [ ] El contratista puede eliminar un soporte existente de una actividad.
- [ ] Tras agregar o eliminar un soporte, la lista de soportes de la actividad se actualiza.

### Edición de documentos adicionales en BORRADOR

- [ ] El contratista puede agregar un documento adicional al informe desde la vista de detalle.
- [ ] El contratista puede eliminar un documento adicional existente del informe.
- [ ] Tras agregar o eliminar un documento, la lista se actualiza.

### Solo lectura en otros estados

- [ ] En estado ENVIADO, EN_REVISION, DEVUELTO y APROBADO, las actividades se muestran en modo solo lectura (sin campos editables ni botones de guardar/eliminar).
- [ ] El comportamiento de solo lectura es idéntico al comportamiento actual del componente.

### Seguridad y aislamiento

- [ ] Un contratista no puede editar actividades de informes de otro contratista (validado por el backend).
- [ ] El backend rechaza con 403 cualquier intento de edición por un usuario no propietario.
- [ ] El backend rechaza con 409 cualquier intento de edición de un informe en estado no editable.

---

## 9. Rama y Entorno

- **Rama base:** `feat/sigcon-i4` (HEAD `7b61d09`)
- **Rama nueva:** `feat/sigcon-i5`
- **Schema Oracle:** `SED_SIGCON` (prefijo tablas `SGCN_`)
- **Sin migraciones DDL** en este incremento
- **Sin cambios de backend** en este incremento
- **Stack:** Java 8, Spring Boot 2.7.18 WAR, Angular 20 + PrimeNG 20, TypeScript strict
- **Restricción Java 8:** Sin `var`, sin `Map.of()`, sin `List.of()`, sin `InputStream.readAllBytes()`

---

## 10. Métricas de Cierre

| Métrica | Meta |
|---------|------|
| Backend tests | Sin regresión — igual que I4 (123 tests, 0 fallos) |
| Frontend specs | >= 90 specs, 0 fallos |
| Endpoints nuevos | 0 (todos existen) |
| DDL changes | 0 |
| Regresión InformeDetalleComponent | 8 tests existentes pasan sin cambios |
| Nuevos tests frontend | >= 12 |
