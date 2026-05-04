# Spec Tecnica - SIGCON Incremento 4
## Hallazgos Funcionales: Revisor Opcional, Contrato Editable, Informe Editable

> **Metodologia:** Spec-Driven Development (SDD) - Spec-Anchored  
> **Version:** 1.0 - **Fecha:** 2026-05-04  
> **Constitucion:** `docs/CONSTITUTION.md`  
> **Arquitectura:** `docs/ARCHITECTURE.md`  
> **PRD de referencia:** `docs/specs/2026-04-30-sigcon-prd.md`  
> **Specs base:** `docs/specs/2026-04-30-sigcon-i1-spec.md`, `docs/specs/2026-05-01-sigcon-i2-spec.md`, `docs/specs/2026-05-01-sigcon-i3-spec.md`  
> **Origen:** Hallazgos de pruebas funcionales - sesion 2026-05-04  
> **Estado:** Listo para implementacion

---

## 1. Alcance Del Incremento

### Origen de los requerimientos

Este incremento no surge de una fase de diseño previa sino de hallazgos encontrados durante las pruebas funcionales ejecutadas el 2026-05-04 sobre el sistema I1-I3 en ejecucion. Cada hallazgo esta trazado a su hallazgo funcional (Hx).

### Modulos incluidos

| Modulo | Hallazgo | Descripcion | Rol principal |
|--------|----------|-------------|---------------|
| H1 - Revisor opcional | Hallazgo 1 | El revisor del contrato es opcional; solo el supervisor es obligatorio | ADMIN / SUPERVISOR |
| H2 - Contrato editable | Hallazgo 2 | Todos los campos del contrato pueden modificarse en cualquier momento | ADMIN |
| H3 - Informe editable | Hallazgo 3 | El encabezado del informe es editable cuando el informe esta en BORRADOR o DEVUELTO | CONTRATISTA |

### Fuera de este incremento

- Cambios en el modelo de datos de notificaciones.
- Nuevos estados en la maquina de informes.
- Modificacion de actividades del informe (ya existe).
- Integracion SECOP2.
- Firma digital criptografica.
- Motor de pagos.

### Entregable de cierre

El sistema permite crear contratos sin revisor asignado, con el supervisor ejerciendo todas las acciones de aprobacion directamente. Los contratos son modificables en cualquier estado por el administrador. El contratista puede editar el periodo del informe mientras este en BORRADOR o DEVUELTO.

---

## 2. Coherencia Con Incrementos Anteriores

- La maquina de estados se extiende; no se elimina ninguna transicion existente.
- Contratos con revisor asignado mantienen exactamente el flujo I1-I3.
- El PDF institucional y las notificaciones I3 aplican igualmente en el nuevo flujo sin revisor.
- No hay cambios de DDL: `SGCN_CONTRATOS.ID_REVISOR` ya era nullable desde I1.
- La clave de unicidad de `NUMERO` en `SGCN_CONTRATOS` se mantiene; la edicion debe respetarla.

---

## 3. Base De Datos

### Sin cambios de DDL

El campo `ID_REVISOR` en `SGCN_CONTRATOS` ya permite NULL a nivel de base de datos y a nivel de entity JPA (`@ManyToOne(fetch = FetchType.LAZY)` sin `optional = false`). No se requieren `ALTER TABLE`.

### Datos de prueba

El script `db/01_datos_prueba.sql` debe incluir al menos:
- Un contrato SIN revisor asignado (ID_REVISOR = NULL) para validar el nuevo flujo.
- El contrato existente CON revisor no se modifica (regresion).

---

## 4. Backend

### 4.1 H1 — Revisor Opcional en la Maquina de Estados

#### ContratoService

El campo `revisorId` del DTO de creacion pasa de obligatorio a opcional.

```
ContratoService.crear(dto):
  - revisorId puede ser null
  - Si revisorId != null: validar que el usuario exista y tenga rol REVISOR
  - Si revisorId == null: no asignar revisor al contrato
```

#### InformeEstadoService — Routing condicional

Se introduce la regla: **si `contrato.getRevisor() == null` entonces el supervisor puede actuar desde ENVIADO**.

**`aprobar(informeId, supervisorEmail)`**

```
boolean sinRevisor = informe.getContrato().getRevisor() == null;
if (sinRevisor):
    assertState(informe, ENVIADO, EN_REVISION)   // acepta ambos estados
else:
    assertState(informe, EN_REVISION)             // comportamiento actual
assertAssignedEmail(contrato.getSupervisor(), supervisorEmail)
// resto del metodo sin cambios (PDF + evento INFORME_APROBADO)
```

**`devolver(informeId, supervisorEmail, observacion)`**

```
boolean sinRevisor = informe.getContrato().getRevisor() == null;
if (sinRevisor):
    assertState(informe, ENVIADO, EN_REVISION)   // acepta ambos estados
else:
    assertState(informe, EN_REVISION)             // comportamiento actual
assertAssignedEmail(contrato.getSupervisor(), supervisorEmail)
// resto del metodo sin cambios (observacion + evento INFORME_DEVUELTO)
```

**`aprobarRevision()` / `devolverRevision()`**

Sin cambios. `assertAssignedEmail(contrato.getRevisor(), revisorEmail)` ya lanza `ACCESO_DENEGADO` cuando `revisor == null`. Comportamiento correcto.

#### Flujo de estados resultante

```
Con revisor (comportamiento I1-I3, sin cambios):
  BORRADOR/DEVUELTO → ENVIADO  (contratista)
  ENVIADO           → EN_REVISION (revisor.aprobarRevision)
  ENVIADO           → DEVUELTO    (revisor.devolverRevision)
  EN_REVISION       → APROBADO    (supervisor.aprobar)
  EN_REVISION       → DEVUELTO    (supervisor.devolver)

Sin revisor (nuevo):
  BORRADOR/DEVUELTO → ENVIADO  (contratista)
  ENVIADO           → APROBADO (supervisor.aprobar)
  ENVIADO           → DEVUELTO (supervisor.devolver)

Edge case — revisor removido mientras informe en EN_REVISION:
  El contrato pasa a tener revisor=null. El supervisor puede entonces
  ejecutar aprobar() o devolver() desde EN_REVISION (cubierto por el
  assertState(ENVIADO, EN_REVISION) del bloque sinRevisor).
```

#### ErrorCodes afectados

No se agregan nuevos `ErrorCode`. Los existentes cubren todos los casos:

| Codigo | Situacion |
|--------|-----------|
| `ACCESO_DENEGADO` | Revisor intenta actuar en contrato sin revisor |
| `TRANSICION_INVALIDA` | Supervisor intenta aprobar desde ENVIADO en contrato con revisor |
| `INFORME_NO_EDITABLE` | Informe ya APROBADO |

### 4.2 H2 — Contrato Editable

#### Nuevo endpoint

```
PUT /api/admin/contratos/{id}
Roles: ADMIN
Produce: application/json
```

#### ContratoUpdateDto

```java
public class ContratoUpdateDto {
    @NotBlank
    private String numero;          // unico, excluye self

    @NotBlank
    @Size(max = 1000)
    private String objeto;

    @NotNull
    private TipoContrato tipo;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valorTotal;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;     // >= fechaInicio

    @NotNull
    private Long contratistaId;     // rol CONTRATISTA

    private Long revisorId;         // opcional, rol REVISOR si presente

    @NotNull
    private Long supervisorId;      // rol SUPERVISOR
}
```

#### ContratoService.actualizar(id, dto)

```
1. Cargar contrato por id (activo = true); si no existe → 404
2. Validar unicidad de dto.numero excluyendo el propio id
3. Resolver usuarios (contratista, supervisor obligatorios; revisor opcional)
4. Validar roles de cada usuario resuelto
5. Validar fechaFin >= fechaInicio
6. Actualizar todos los campos del contrato
7. contrato.setUpdatedAt(LocalDateTime.now())
8. informeRepository.save(contrato)
9. Retornar ContratoDetalleDto
```

**Impacto en informes activos:** El nuevo asignado prevalece de inmediato. No se requiere ningun ajuste adicional; la carga lazy del contrato en `InformeEstadoService` resuelve el usuario actualizado.

#### Respuesta de error

| Condicion | Codigo HTTP | ErrorCode |
|-----------|-------------|-----------|
| Contrato no existe o inactivo | 404 | `CONTRATO_NO_ENCONTRADO` |
| Numero duplicado en otro contrato | 409 | `NUMERO_CONTRATO_DUPLICADO` |
| Usuario no existe o rol incorrecto | 400 | `USUARIO_NO_ENCONTRADO` |
| fechaFin < fechaInicio | 422 | `FECHA_FIN_INVALIDA` |

> `NUMERO_CONTRATO_DUPLICADO` ya existe (I1). `FECHA_FIN_INVALIDA` es nuevo — se agrega al enum en I4 (ver seccion 4.4).

### 4.4 Nuevo ErrorCode I4

Agregar a `ErrorCode.java`:

```java
// I4
FECHA_FIN_INVALIDA
```

Se usa en: `ContratoService.actualizar()` e `InformeService.actualizar()` cuando `fechaFin < fechaInicio`.

### 4.5 ContratoService — Revisor opcional (metodo auxiliar)

El metodo privado `findActiveUsuario(Long id, RolUsuario, String)` lanza excepcion si `id == null`.
Para el revisor se necesita una variante que devuelva `null` cuando `id == null`:

```java
private Usuario findActiveUsuarioOpcional(Long id, RolUsuario expectedRole) {
    if (id == null) return null;
    // misma logica que findActiveUsuario pero sin el null-check inicial
    Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
        .orElseThrow(() -> new SigconBusinessException(
            ErrorCode.USUARIO_NO_ENCONTRADO, "Usuario no encontrado", HttpStatus.NOT_FOUND));
    if (usuario.getRol() != expectedRole) {
        throw new SigconBusinessException(
            ErrorCode.USUARIO_NO_ENCONTRADO,
            "El usuario seleccionado no corresponde al rol " + expectedRole.name(),
            HttpStatus.BAD_REQUEST);
    }
    return usuario;
}
```

Uso en `crear()` y `actualizar()`:

```java
contrato.setRevisor(findActiveUsuarioOpcional(request.getIdRevisor(), RolUsuario.REVISOR));
```

### 4.6 H3 — Informe Editable en BORRADOR / DEVUELTO

#### Nuevo endpoint

```
PATCH /api/informes/{id}
Roles: CONTRATISTA
Produce: application/json
```
> FECHA_FIN_INVALIDA: ver seccion 4.4.

#### InformeUpdateDto

```java
public class InformeUpdateDto {
    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;    // >= fechaInicio
}
```

#### InformeService.actualizar(id, dto, contratistaEmail)

```
1. Cargar informe por id (activo = true); si no existe → 404
2. Validar que informe.estado == BORRADOR o DEVUELTO;
   si no → SigconBusinessException(INFORME_NO_EDITABLE, 409)
3. Validar que contratistaEmail == informe.contrato.contratista.email;
   si no → SigconBusinessException(ACCESO_DENEGADO, 403)
4. Validar fechaFin >= fechaInicio;
   si no → SigconBusinessException(FECHA_FIN_INVALIDA, 422)
5. informe.setFechaInicio(dto.getFechaInicio())
6. informe.setFechaFin(dto.getFechaFin())
7. informeRepository.save(informe)
8. Retornar InformeDetalleDto
```

---

## 5. Seguridad

### Endpoints nuevos y reglas

| Endpoint | Metodo | Regla existente que lo cubre |
|----------|--------|------------------------------|
| `/api/admin/contratos/{id}` | PUT | `hasRole('ADMIN')` via `/api/admin/**` — ya cubierto |
| `/api/informes/{id}` | PATCH | `hasAnyRole('CONTRATISTA',...)` via `/api/informes/**` — ya cubierto |

**No se requieren cambios en `SecurityConfig` ni `DevSecurityConfig`.**

### Parche a tests de seguridad

`SigconBackendSecurityTest` y `InformeSecurityTest` deben agregar:

- `PUT /api/admin/contratos/{id}` con rol ADMIN → 2xx (o 4xx de negocio, no 401/403)
- `PUT /api/admin/contratos/{id}` con rol CONTRATISTA → 403
- `PATCH /api/informes/{id}` con rol CONTRATISTA → 2xx (o 4xx de negocio, no 401/403)
- `PATCH /api/informes/{id}` sin autenticacion → 401

---

## 6. Frontend

### 6.1 H1 — AdminContratoForm: Revisor opcional

- El campo `revisor` del formulario de creacion de contrato pasa a ser **opcional**.
- El campo debe mostrar un indicador visual claro de que es opcional (sin asterisco `*`).
- Si no se selecciona revisor, el DTO enviado incluye `revisorId: null`.
- El endpoint de creacion ya acepta `revisorId: null` tras el cambio de backend.

### 6.2 H2 — AdminContratoForm: Modo edicion

- En la vista de detalle del contrato (ADMIN), agregar boton **"Editar contrato"**.
- Al activar el modo edicion, todos los campos del formulario se vuelven interactivos.
- El boton **"Guardar"** llama a `PUT /api/admin/contratos/{id}`.
- El boton **"Cancelar"** restaura los valores originales sin llamada HTTP.
- Tras guardar exitosamente: mostrar toast de exito y regresar a modo lectura.
- El numero de contrato debe mostrar advertencia inline si hay duplicado (409).

### 6.3 H3 — InformeDetail: Periodo editable

- En la vista de detalle del informe, cuando `estado == BORRADOR || estado == DEVUELTO`:
  - Los campos `fechaInicio` y `fechaFin` son editables (PrimeNG Calendar).
  - Mostrar boton **"Guardar periodo"** al lado de los campos.
  - El boton llama a `PATCH /api/informes/{id}`.
  - Tras guardar exitosamente: actualizar el estado local del informe sin recargar toda la pagina.
- Cuando `estado != BORRADOR && estado != DEVUELTO`: campos en modo solo lectura (comportamiento actual).

### Servicio Angular

```typescript
// contrato.service.ts
actualizarContrato(id: number, dto: ContratoUpdateDto): Observable<ContratoDetalleDto>

// informe.service.ts
actualizarInforme(id: number, dto: InformeUpdateDto): Observable<InformeDetalleDto>
```

---

## 7. Tests Requeridos

### Backend — InformeEstadoService (sin revisor)

| Test | Descripcion |
|------|-------------|
| `aprobarSinRevisorDesdeEnviado` | ENVIADO → APROBADO cuando `contrato.revisor == null` |
| `devolverSinRevisorDesdeEnviado` | ENVIADO → DEVUELTO cuando `contrato.revisor == null` |
| `aprobarConRevisorDesdeEnviadoFalla` | ENVIADO con revisor asignado → TRANSICION_INVALIDA |
| `supervisorNoAsignadoFallaEnflujoSinRevisor` | Supervisor incorrecto → ACCESO_DENEGADO |
| `revisorNoPuedeActuarEnContratoSinRevisor` | aprobarRevision con revisor null → ACCESO_DENEGADO |
| `revisorRemovidoConInformeEnRevision` | Informe en EN_REVISION + revisor removido → supervisor puede aprobar |

### Backend — ContratoService

| Test | Descripcion |
|------|-------------|
| `crearContratoSinRevisorExitoso` | Contrato creado con revisorId = null |
| `actualizarContratoExitoso` | Actualiza todos los campos correctamente |
| `actualizarContratoNumeroDuplicadoFalla` | Numero ya usado en otro contrato → 409 |
| `actualizarContratoAgregaRevisor` | Contrato sin revisor pasa a tener revisor |
| `actualizarContratoQuitaRevisor` | Contrato con revisor pasa a sin revisor |

### Backend — InformeService

| Test | Descripcion |
|------|-------------|
| `actualizarInformeBorradorExitoso` | Actualiza periodo en estado BORRADOR |
| `actualizarInformeDevueltoExitoso` | Actualiza periodo en estado DEVUELTO |
| `actualizarInformeEnviadoFalla` | Estado ENVIADO → INFORME_NO_EDITABLE |
| `actualizarInformeAprobadoFalla` | Estado APROBADO → INFORME_NO_EDITABLE |
| `actualizarInformeContratistaIncorrectoFalla` | Email diferente → ACCESO_DENEGADO |
| `actualizarInformeFechaFinInvalidaFalla` | fechaFin < fechaInicio → FECHA_FIN_INVALIDA |

### Frontend

| Test | Descripcion |
|------|-------------|
| `AdminContratoForm crea contrato sin revisor` | Envio con revisorId: null exitoso |
| `AdminContratoForm modo edicion activa campos` | Campos editables al activar edicion |
| `AdminContratoForm guarda cambios y vuelve a lectura` | PUT exitoso → modo lectura |
| `InformeDetail muestra campos editables en BORRADOR` | Campos habilitados |
| `InformeDetail no muestra edicion en ENVIADO` | Campos deshabilitados |
| `InformeDetail guarda periodo correctamente` | PATCH exitoso → actualiza vista |

---

## 8. Criterios De Aceptacion

### H1 — Revisor opcional

- [ ] Se puede crear un contrato sin seleccionar revisor.
- [ ] El contratista puede enviar el informe normalmente (BORRADOR → ENVIADO).
- [ ] El supervisor puede aprobar el informe directamente desde ENVIADO.
- [ ] El supervisor puede devolver el informe directamente desde ENVIADO (con observacion).
- [ ] Si el revisor es removido mientras un informe esta en EN_REVISION, el supervisor puede actuar.
- [ ] Un contrato con revisor mantiene el flujo original sin regresion.

### H2 — Contrato editable

- [ ] El ADMIN puede editar cualquier campo del contrato en cualquier momento.
- [ ] El numero de contrato no puede duplicarse con otro contrato existente.
- [ ] Cambiar el supervisor o revisor de un contrato con informes activos hace que el nuevo asignado sea quien puede actuar.
- [ ] Roles no ADMIN (CONTRATISTA, REVISOR, SUPERVISOR) reciben 403 al intentar editar un contrato.

### H3 — Informe editable

- [ ] El contratista puede modificar el periodo (fechaInicio/fechaFin) de un informe en BORRADOR.
- [ ] El contratista puede modificar el periodo de un informe en DEVUELTO.
- [ ] No es posible editar el informe cuando esta en ENVIADO, EN_REVISION o APROBADO.
- [ ] Un contratista no puede editar el informe de otro contrato.
- [ ] fechaFin no puede ser anterior a fechaInicio.

---

## 9. Rama Y Entorno

- **Rama:** `feat/sigcon-i4` (base: `feat/sigcon-i3` HEAD `9be9c73`)
- **Schema Oracle:** `SED_SIGCON` (prefijo tablas `SGCN_`)
- **Sin migraciones DDL** en este incremento
- **Stack:** Java 8, Spring Boot 2.7.18 WAR, Angular 20 + PrimeNG 20
- **Restriccion Java 8:** Sin `var`, sin `Map.of()`, sin `List.of()`, sin `InputStream.readAllBytes()`
