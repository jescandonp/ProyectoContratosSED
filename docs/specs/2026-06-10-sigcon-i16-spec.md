# SIGCON I16 — Spec Técnica
## Bloqueo Individual de Carga de Informes + Campo Plazo en Contrato

**Fecha:** 2026-06-10
**Incremento:** I16
**Origen:** Requerimiento funcional — control operativo de carga de informes por contrato y texto libre de plazo contractual en PDF
**Rama:** `main`

---

## Contexto

Se requieren dos funcionalidades independientes pero parte del mismo incremento por su bajo alcance:

1. **F1 — Bloqueo de carga:** El ADMIN debe poder bloquear individualmente la creación de informes en un contrato específico. El contratista no ve el botón "Nuevo Informe" cuando el contrato está bloqueado.
2. **F2 — Plazo libre:** El texto de "Plazo:" en la Sección 1 del PDF (formato 11-IF-023 V1) actualmente está hardcodeado en `InformePdfTemplateService`. Debe ser un campo de texto libre gestionado desde el formulario de crear/editar contrato y persistido en BD.

---

## F1 — Bloqueo individual de carga de informes

### Requerimiento

El ADMIN puede activar/desactivar un bloqueo en cualquier contrato que impide al contratista crear nuevos informes. El bloqueo no afecta informes ya creados ni el flujo de revisión/aprobación.

### Cambios de esquema

```sql
ALTER TABLE SGCN_CONTRATOS
  ADD BLOQUEADO_CARGA_INFORME NUMBER(1) DEFAULT 0 NOT NULL;
```

### Backend

**Entidad `Contrato`:**
- Nuevo campo: `@Column(name = "BLOQUEADO_CARGA_INFORME", nullable = false) private Boolean bloqueadoCargaInforme = false;`

**DTO `ContratoDetalleDto`:**
- Agregar campo `bloqueadoCargaInforme: boolean` para exponerlo al frontend.

**Nuevo DTO `BloqueoInformeRequest`:**
```java
public class BloqueoInformeRequest {
    @NotNull
    private Boolean bloqueado;
    // getter/setter
}
```

**Nuevo endpoint en `AdminContratoController`:**
```
PATCH /api/admin/contratos/{id}/bloqueo-informe
Body: { "bloqueado": true }
```
- Solo accesible para ADMIN (coherente con los demás endpoints de `AdminContratoController`).
- Delega a `ContratoService.actualizarBloqueoInforme(id, bloqueado)`.

**`ContratoService.actualizarBloqueoInforme(Long id, Boolean bloqueado)`:**
- Carga el contrato, actualiza `bloqueadoCargaInforme`, persiste.
- Lanza `SigconBusinessException(NOT_FOUND)` si el contrato no existe.

**`InformeService.crearInforme()`:**
- Antes de crear, verificar: `if (contrato.getBloqueadoCargaInforme()) throw new SigconBusinessException(INFORME_CARGA_BLOQUEADA, HttpStatus.FORBIDDEN)`.

**Nuevo `ErrorCode.INFORME_CARGA_BLOQUEADA`:** mensaje `"La carga de informes para este contrato está bloqueada por el administrador."`.

**`ContratoMapper`:**
- Incluir `bloqueadoCargaInforme` al mapear `Contrato` → `ContratoDetalleDto`.

### Frontend (Angular)

**`contrato.model.ts`:**
- Agregar `bloqueadoCargaInforme: boolean` a `ContratoDetalle`.

**`contrato-detalle.component.ts`:**
- El botón/enlace "Nuevo Informe" se renderiza solo si `!contrato().bloqueadoCargaInforme`.

**`admin-contratos.component.ts` o panel admin:**
- Añadir toggle "Bloquear carga de informes" en la vista de detalle/gestión del contrato.
- Al cambiar llama `PATCH /api/admin/contratos/{id}/bloqueo-informe` vía `ContratoService.actualizarBloqueoInforme(id, bloqueado)`.
- El estado del toggle refleja `contrato.bloqueadoCargaInforme`.

**`contrato.service.ts`:**
- Agregar método `actualizarBloqueoInforme(id: number, bloqueado: boolean): Observable<ContratoDetalle>`.

---

## F2 — Campo Plazo libre en contrato

### Requerimiento

El texto de la fila "Plazo:" en Sección 1 del PDF actualmente está generado automáticamente con las fechas del contrato (ver `InformePdfTemplateService.java` líneas 322–326). Debe poder ser reemplazado por un texto libre ingresado por el ADMIN al crear o editar el contrato. Si el campo está vacío, se mantiene el texto autogenerado como fallback (retrocompatibilidad con contratos existentes).

### Cambios de esquema

```sql
ALTER TABLE SGCN_CONTRATOS ADD PLAZO CLOB;
```

### Backend

**Entidad `Contrato`:**
- Nuevo campo: `@Column(name = "PLAZO", columnDefinition = "CLOB") private String plazo;` (nullable).

**`ContratoRequest`:**
- Agregar `private String plazo;` (sin `@NotBlank`, sin `@Size` — es CLOB libre).

**`ContratoDetalleDto`:**
- Agregar `private String plazo;`.

**`ContratoMapper`:**
- Incluir `plazo` en el mapeo bidireccional (request → entidad, entidad → dto).

**`InformePdfTemplateService.appendSeccion1()`:**
- Reemplazar el bloque de texto hardcodeado del plazo (líneas 322–326) por:
  ```java
  String plazoTexto = notEmpty(c.getPlazo())
      ? esc(c.getPlazo())
      : "El plazo del contrato ser&#225; hasta el " + c.getFechaFin().format(DATE_FMT)
        + " y a partir de la suscripci&#243;n del acta de inicio, previo cumplimiento de los"
        + " requisitos de perfeccionamiento y ejecuci&#243;n. En todo caso, la fecha de Inicio"
        + " no podr&#225; ser anterior al " + c.getFechaInicio().format(DATE_FMT) + ".";
  fila2(sb, "Plazo:", plazoTexto, false);
  ```
- `notEmpty()` ya existe en el servicio; verificar uso consistente.

### Frontend (Angular)

**`contrato.model.ts`:**
- Agregar `plazo?: string | null` a `ContratoDetalle` y `ContratoRequest`.

**`admin-contrato-form.component.ts`:**
- Agregar `<textarea>` para "Plazo" en la sección "Información del Contrato", entre "Forma de Pago" y "Modificaciones".
- Campo opcional, sin `required`. Hint: "Si se deja vacío, se usará el texto estándar basado en fechas del contrato."
- Enlazado a `form.plazo` con `[(ngModel)]`.

**`contrato-detalle.component.ts`:**
- Mostrar el campo "Plazo" en la sección de datos generales cuando `c.plazo` no sea null/vacío, al igual que `formaPago` y `modificaciones`.

---

## Archivos a modificar

### Backend

| Archivo | Cambio |
|---------|--------|
| `domain/entity/Contrato.java` | +2 campos: `bloqueadoCargaInforme`, `plazo` |
| `application/dto/contrato/ContratoDetalleDto.java` | +2 campos: `bloqueadoCargaInforme`, `plazo` |
| `application/dto/contrato/ContratoRequest.java` | +1 campo: `plazo` |
| `application/dto/contrato/BloqueoInformeRequest.java` | **Nuevo DTO** |
| `application/mapper/ContratoMapper.java` | Mapear 2 campos nuevos |
| `application/service/ContratoService.java` | +método `actualizarBloqueoInforme()` |
| `application/service/InformeService.java` | +validación bloqueo en `crearInforme()` |
| `application/service/InformePdfTemplateService.java` | Reemplazar texto hardcodeado de plazo |
| `web/controller/AdminContratoController.java` | +endpoint `PATCH .../bloqueo-informe` |
| `web/exception/ErrorCode.java` | +`INFORME_CARGA_BLOQUEADA` |
| `db/migration/` | +script DDL con 2 `ALTER TABLE` |

### Frontend

| Archivo | Cambio |
|---------|--------|
| `core/models/contrato.model.ts` | +campos `bloqueadoCargaInforme`, `plazo` |
| `core/services/contrato.service.ts` | +método `actualizarBloqueoInforme()` |
| `features/contratos/detalle/contrato-detalle.component.ts` | Ocultar botón nuevo informe si bloqueado; mostrar plazo |
| `features/admin/contratos/admin-contrato-form.component.ts` | +textarea Plazo |
| `features/admin/contratos/admin-contratos.component.ts` | +toggle bloqueo informe |

### Tests

| Archivo | Cambio |
|---------|--------|
| `InformeServiceTest.java` | +test `crearInforme_bloqueado_lanzaForbidden` |
| `ContratoServiceTest.java` | +test `actualizarBloqueoInforme_activa_y_desactiva` |
| `InformePdfTemplateServiceTest.java` | +test `seccion1UsaPlazoCustomSiExiste` + `seccion1UsaFallbackSiPlazoNulo` |

---

## Criterios de aceptación

- [ ] F1-BE: `PATCH /api/admin/contratos/{id}/bloqueo-informe` actualiza el flag y retorna el DTO actualizado.
- [ ] F1-BE: `POST /api/informes` en contrato bloqueado retorna `403 FORBIDDEN` con código `INFORME_CARGA_BLOQUEADA`.
- [ ] F1-FE: El botón "Nuevo Informe" en `contrato-detalle` no aparece cuando `bloqueadoCargaInforme: true`.
- [ ] F1-FE: El toggle admin cambia el estado del bloqueo y refleja el estado actual.
- [ ] F2-BE: Al crear/editar contrato con `plazo` no vacío, el valor se persiste en `SGCN_CONTRATOS.PLAZO`.
- [ ] F2-PDF: El PDF generado muestra el texto personalizado de plazo cuando el campo está poblado.
- [ ] F2-PDF: El PDF generado muestra el texto autogenerado cuando `plazo` es null (retrocompatibilidad).
- [ ] F2-FE: El formulario admin muestra el textarea "Plazo" con hint informativo.
- [ ] F2-FE: El detalle del contrato muestra el campo "Plazo" cuando tiene valor.
- [ ] Suite `mvn test`: BUILD SUCCESS, 0 failures.

---

## Stack de referencia

Java 8 · Spring Boot 2.7.18 · JPA/Hibernate · Oracle 19c · Angular 20 · PrimeNG 20
