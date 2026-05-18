# Plan de Implementación — SIGCON I8
## PDF Formato Institucional SED (Plantilla 11-IF-023 V1)

> **Spec de referencia:** `docs/specs/2026-05-18-sigcon-i8-spec.md`
> **Fecha:** 2026-05-18
> **Metodología:** SDD — TDD estricto por tarea
> **Estado:** EJECUTADO (merge en main, HEAD `ed426eb`)

---

## Resumen de tareas

| Tarea | Archivo(s) principal(es) | Gate |
|-------|--------------------------|------|
| T0 | Execution log | Docs only |
| T1 | DB + Entidad + DTOs + Servicio + Mapper | `PdfInformeServiceTest` GREEN |
| T2 | Logo + `InformePdfTemplateService` | Compilación + smoke PDF |
| T3 | Angular `informe.model.ts` + componentes | `ng build` sin errores |

---

## T0 — Documentación

Crear `docs/plans/2026-05-18-sigcon-i8-execution-log.md` con encabezado de incremento.

---

## T1 — Capa de datos

### T1.1 — Migración SQL (`db/05_add_fecha_elaboracion.sql`)

```sql
DECLARE v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME='SGCN_INFORMES' AND COLUMN_NAME='FECHA_ELABORACION';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SGCN_INFORMES ADD (FECHA_ELABORACION DATE DEFAULT NULL)';
  END IF;
END;
/
```

### T1.2 — `Informe.java`

```java
@Column(name = "FECHA_ELABORACION")
private LocalDate fechaElaboracion;
public LocalDate getFechaElaboracion() { return fechaElaboracion; }
public void setFechaElaboracion(LocalDate v) { this.fechaElaboracion = v; }
```

### T1.3 — DTOs

`InformeRequest`, `InformeUpdateDto`, `InformeResumenDto`: agregar `private LocalDate fechaElaboracion` con getter/setter.

### T1.4 — `InformeService`

En `crearInforme()`:
```java
informe.setFechaElaboracion(
    request.getFechaElaboracion() != null ? request.getFechaElaboracion() : LocalDate.now());
```

En `actualizar()` y `actualizarInforme()`:
```java
if (dto.getFechaElaboracion() != null) informe.setFechaElaboracion(dto.getFechaElaboracion());
```

### T1.5 — `InformeMapper.fillResumen()`

```java
dto.setFechaElaboracion(informe.getFechaElaboracion());
```

### T1.6 — Test

Actualizar helper `informe()` en `PdfInformeServiceTest`:
```java
i.setFechaElaboracion(LocalDate.of(2026, 2, 4));
```

**Gate:** `mvn test -Dtest=PdfInformeServiceTest` → 5/5 GREEN.

---

## T2 — `InformePdfTemplateService` — reescritura

### T2.1 — Logo

Colocar `logo-alcaldia.png` en `sigcon-backend/src/main/resources/`. Carga vía `@PostConstruct`:

```java
@PostConstruct
public void cargarLogo() {
    try {
        ClassPathResource res = new ClassPathResource("logo-alcaldia.png");
        if (res.exists()) {
            byte[] bytes = res.getInputStream().readAllBytes();
            logoBase64 = Base64.getEncoder().encodeToString(bytes);
        } else {
            log.warn("logo-alcaldia.png no encontrado — PDF sin logo institucional.");
        }
    } catch (Exception e) {
        log.warn("No se pudo cargar logo-alcaldia.png: {}", e.getMessage());
    }
}
```

### T2.2 — CSS (`appendCss`)

- `@page { margin: 90pt 36pt 54pt 36pt; }`
- `@page { @top-center { content: element(pageHeader) } @bottom-center { content: element(pageFooter) } }`
- Clases: `.running-header`, `.running-footer`, `.ph-wrap`, `.ph-logo`, `.ph-center`, `.ph-right`, `.sec-title`, `.bullet-list`, `.firma-table`, `.firma-img` (70pt/200pt), `.firma-line`, `.firma-cell`, `.firma-cell-full`

### T2.3 — Métodos del template

| Método | Descripción |
|--------|-------------|
| `appendRunningHeader(sb, informe)` | Tabla 3 cols: logo \| título+contrato \| período+código |
| `appendSeccion1(sb, informe)` | Tabla 2 cols con datos del contrato |
| `appendSeccion2(sb, actividades)` | Tabla 3 cols obligación/actividades/evidencia |
| `appendSeccion3(sb, informe, aportes)` | Tabla 5 cols SGSSI |
| `appendSeccion4(sb, informe)` | Texto SI/NO correspondencia + "01 folios" |
| `appendSeccion5(sb, informe)` | Declaración especial + desembolso + fechaElaboracion |
| `appendFirmas(sb, ...)` | Layout 2+1: contratista+supervisor / revisor centrado |
| `appendFirmaCell(sb, ...)` | Celda individual con img + línea + nombre + cargo + rol |
| `fila2(sb, label, value, alt)` | Fila de tabla 2 columnas |
| `nombreDia(fecha)` | Retorna numeral del día como String |
| `labelSgssi(item)` | SALUD / PENSIÓN / ARL en mayúsculas |

**Gate:** compilación + `PdfInformeServiceTest` 5/5 GREEN.

---

## T3 — Frontend Angular

### T3.1 — `core/models/informe.model.ts`

Agregar `fechaElaboracion?: string | null` en `InformeResumen`, `InformeRequest`, `InformeUpdateDto`.

### T3.2 — `informe-form.component.ts` (nuevo informe)

```typescript
readonly fechaElaboracion = signal<string>(new Date().toISOString().slice(0, 10));
// En crearInforme(): fechaElaboracion: this.fechaElaboracion() || null
```

Campo date picker en template después de `correspondenciaPendiente`.

### T3.3 — `corregir-informe.component.ts`

```typescript
readonly fechaElaboracion = signal<string>(new Date().toISOString().slice(0, 10));
// En poblarFormulario(): this.fechaElaboracion.set(informe.fechaElaboracion ?? hoy)
// En actualizarInforme(): fechaElaboracion: this.fechaElaboracion() || null
```

### T3.4 — `corregir-informe.component.html`

Date picker con mismo estilo que campos existentes.

**Gate:** `ng build --configuration development` sin errores TypeScript.

---

## Fixes post-review (`ed426eb`)

Aplicados tras code review de `superpowers:code-reviewer`:

| Hallazgo | Fix |
|----------|-----|
| I-1: tabla Sec 1 con filas de 2 y 4 columnas mezcladas | Reemplazado por 2 llamadas `fila2()` separadas |
| I-3: SQL `DEFAULT SYSDATE` back-fillea históricos | Cambiado a `DEFAULT NULL` |
| I-5: `nombreDia()` inconsistente días 1-5 vs 6-31 | Simplificado a numeral siempre |
| I-7: `throws Exception` en `generarPdf` | Cambiado a `throws IOException, DocumentException, ParserConfigurationException, SAXException` |
| M-1: signal `fechaElaboracion` iniciaba en `''` en corregir | Cambiado a `new Date().toISOString().slice(0,10)` |

---

## Historial de commits

| SHA | Descripción |
|-----|-------------|
| `95e6ac4` | feat(i8): add fechaElaboracion field to Informe data layer |
| `e71c541` | feat(i8): rewrite InformePdfTemplateService — formato institucional SED 11-IF-023 V1 |
| `51204c3` | feat(i8): add fechaElaboracion date field to informe forms |
| `ed426eb` | fix(i8): address code review findings — table structure, SQL default, throws clause, signal init |

---

*Plan generado mediante SDD — SIGCON — Incremento 8 — 2026-05-18.*
