# Spec Técnica — SIGCON I8
## PDF Formato Institucional SED (Plantilla 11-IF-023 V1)

> **Fecha:** 2026-05-18
> **Metodología:** SDD
> **Estado:** APROBADO (implementado en rama `claude/distracted-vaughan-5186a6`, mergeado a main)

---

## 1. Contexto

El sistema genera actualmente un PDF simplificado de 4 secciones. La Secretaría de Educación del Distrito
exige que el PDF de informe de actividades aprobado corresponda exactamente a la Plantilla Institucional
**11-IF-023 V1**, que incluye encabezado y pie de página por página, 5 secciones numeradas,
tabla SGSSI de 5 columnas, secciones de correspondencia y declaración especial, y layout de firmas 2+1.

---

## 2. Campo `fechaElaboracion`

### 2.1 Descripción
Fecha en que el contratista elabora el informe. Campo nuevo en la entidad `Informe`.

### 2.2 Reglas de negocio
- Por defecto toma el valor de `LocalDate.now()` en el momento de creación si el cliente no lo envía.
- Es editable por el contratista en estado BORRADOR y DEVUELTO.
- Se incluye en la Sección 5 del PDF y en el texto introductorio de las firmas.

### 2.3 Cambios de datos
```sql
ALTER TABLE SGCN_INFORMES ADD (FECHA_ELABORACION DATE DEFAULT NULL);
```
- `DEFAULT NULL` para no back-fillear registros históricos con fecha incorrecta.
- La capa de servicio garantiza el default a `LocalDate.now()` vía Java.

---

## 3. PDF — Layout objetivo (11-IF-023 V1)

### 3.1 Encabezado por página (running header)
Tabla de 3 columnas que se repite en cada página via CSS Paged Media (`position:running(pageHeader)`):
- **Izquierda:** Logo Alcaldía Mayor (`logo-alcaldia.png` desde classpath, fallback graceful si ausente)
- **Centro:** "INFORME DE ACTIVIDADES No. NN", número de contrato y año
- **Derecha:** Período del informe (desde/hasta) + código "11-IF-023 V1"

### 3.2 Pie de página por página (running footer)
Texto institucional: "Avenida El Dorado N° 66-63 · PBX: 3241000 · www.educacionbogota.edu.co · Línea 195"

### 3.3 Sección 1 — Datos del Contrato
Tabla de 2 columnas con: contratista, objeto, valor (texto + cifra), forma de pago, plazo (texto institucional),
modificaciones, fecha de inicio, fecha de terminación, dependencia, supervisor - cargo.

### 3.4 Sección 2 — Ejecución de Actividades
Tabla de 3 columnas: Obligación Contractual | Actividades realizadas | Evidencia Verificable.
- Actividades: bullet list si la descripción contiene saltos de línea (`\n`), bullet simple si no.
- Evidencia Verificable: nombre subrayado si tipo = URL, nombre plano si tipo = ARCHIVO.

### 3.5 Sección 3 — Aportes SGSSI
Tabla de **5 columnas**: ITEM | PERÍODO PAGO (mm/aaaa) | FECHA DE PAGO | VALOR APORTADO | ENTIDAD.
- Período = mes anterior al inicio del informe (calculado desde `fechaInicio`).
- Items en MAYÚSCULAS: SALUD, PENSIÓN, ARL.

### 3.6 Sección 4 — Estado Radicación de la Correspondencia
Texto institucional fijo con marcas SI/NO según `correspondenciaPendiente > 0`.
Siempre menciona **"01 folios"** (valor fijo, no editable).

### 3.7 Sección 5 — Declaración Especial
- Párrafo 1: declaración del contratista (texto fijo).
- Párrafo 2: supervisión verifica cumplimiento, autoriza pago/desembolso No. X por valor Y, porcentaje Z%.
- Línea final: Fecha de elaboración.

### 3.8 Firmas — Layout 2+1
- **Fila 1 (2 celdas):** Contratista (izquierda) + "Vo. Bo [Nombre Supervisor]" (derecha)
- **Fila 2 (1 celda centrada, opcional):** Revisó — Apoyo a la Supervisión (solo si hay revisor con firma)
- Dimensiones: `max-height:70pt; max-width:200pt`
- Texto introductorio: "Para constancia se firma por quienes en ella intervinieron al **N** días del mes de **MES** de **AAAA**"

---

## 4. Frontend Angular

### 4.1 Nuevo campo en formularios
Campo `fechaElaboracion` (tipo `date`, ISO string `yyyy-MM-dd`) en:
- **Crear informe** (`informe-form.component.ts`): signal con default = hoy
- **Corregir informe** (`corregir-informe.component.ts`): signal inicializado desde el informe existente

### 4.2 Interfaces TypeScript actualizadas
- `InformeResumen.fechaElaboracion?: string | null`
- `InformeRequest.fechaElaboracion?: string | null`
- `InformeUpdateDto.fechaElaboracion?: string | null`

---

## 5. Criterios de aceptación

| # | Criterio | Verificación |
|---|----------|-------------|
| AC-1 | `fechaElaboracion` defaults a hoy en crear informe | Tests + smoke |
| AC-2 | `fechaElaboracion` es opcional en update (patch) | Tests |
| AC-3 | SQL idempotente — no back-fillea históricos | Revisión DDL |
| AC-4 | Logo carga desde classpath con WARN si ausente | Log en arranque |
| AC-5 | Encabezado institucional en cada página del PDF | Descarga manual |
| AC-6 | 5 secciones numeradas en orden correcto | Descarga manual |
| AC-7 | Sección 3 tiene 5 columnas con período calculado | Descarga manual |
| AC-8 | Sección 4 muestra SI o NO según correspondenciaPendiente | Descarga manual |
| AC-9 | Firmas en layout 2+1 con dimensiones 70pt/200pt | Descarga manual |
| AC-10 | ng build sin errores TypeScript | CI |
| AC-11 | PdfInformeServiceTest 5/5 GREEN | CI |

---

## 6. Archivos afectados

### Backend
| Archivo | Cambio |
|---------|--------|
| `db/05_add_fecha_elaboracion.sql` | Nuevo — migración DDL |
| `domain/entity/Informe.java` | + `fechaElaboracion` |
| `application/dto/informe/InformeRequest.java` | + `fechaElaboracion` |
| `application/dto/informe/InformeUpdateDto.java` | + `fechaElaboracion` |
| `application/dto/informe/InformeResumenDto.java` | + `fechaElaboracion` |
| `application/service/InformeService.java` | default + mapeo en actualizar |
| `application/mapper/InformeMapper.java` | + `fillResumen` |
| `application/service/InformePdfTemplateService.java` | Reescritura completa |
| `src/test/.../PdfInformeServiceTest.java` | helper `informe()` actualizado |

### Frontend
| Archivo | Cambio |
|---------|--------|
| `core/models/informe.model.ts` | + `fechaElaboracion` en 3 interfaces |
| `features/informes/nuevo/informe-form.component.ts` | signal + campo + envío |
| `features/informes/corregir/corregir-informe.component.ts` | signal + init + envío |
| `features/informes/corregir/corregir-informe.component.html` | date picker |

---

*Spec generada mediante SDD — SIGCON — Incremento 8 — 2026-05-18.*
