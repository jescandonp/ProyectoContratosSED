# SIGCON I15 — Spec Técnica
## Correcciones de Formato PDF Informe 11-IF-023 V1

**Fecha:** 2026-06-09
**Incremento:** I15
**Origen:** Revisión funcional del PDF generado vía endpoint dev preview (`GET /api/dev/pdf-preview`) post-hotfixes PDF (commits 3591b19, f98590b, 4d7e4b9)
**Rama:** `main`

---

## Contexto

Tras la validación visual del PDF generado por el endpoint de desarrollo, se identificaron 6 desviaciones respecto al formato institucional 11-IF-023 V1. Todas las correcciones se concentran en `InformePdfTemplateService.java`.

---

## Hallazgos y requerimientos

### F1 — Fuente global Arial no embebida

**Hallazgo:** Flying Saucer/OpenPDF solo reconoce las 14 fuentes base PDF por defecto (Helvetica, Times, Courier, etc.). Aunque el CSS declara `font-family:Arial,sans-serif`, si Arial no está registrada en el `ITextFontResolver`, el renderer cae al fallback `sans-serif` → Helvetica. El formato institucional exige Arial.

**Corrección:** En `generarPdf()`, tras construir `ITextRenderer`, invocar un método auxiliar `registrarFuenteArial(renderer)` que busca `arial.ttf` en rutas candidatas del sistema operativo (Windows y Linux) y la registra con `IDENTITY_H` y `EMBEDDED`. Si no se encuentra ninguna, log de advertencia y continúa con Helvetica (comportamiento previo).

**Rutas candidatas (en orden):**
1. `C:/Windows/Fonts/arial.ttf`
2. `C:/Windows/Fonts/Arial.ttf`
3. `/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf`

---

### F2 — Tabla de encabezado sin bordes completos

**Hallazgo:** La tabla del running header (`.ph-wrap`) declara `border:0.8pt solid #000` a nivel de tabla, pero Flying Saucer con `border-collapse:collapse` no garantiza el borde exterior del `<table>`. Los bordes deben declararse en las celdas `<td>`.

**Corrección:** Añadir `border:0.8pt solid #000` a las clases `.ph-logo`, `.ph-center` y `.ph-right`. Eliminar los `border-right`/`border-left` individuales que ya quedan cubiertos por el borde completo de celda.

---

### F3 — "INFORME DE ACTIVIDADES" tamaño de fuente insuficiente

**Hallazgo:** La clase `.ph-center-title` tiene `font-size:9.5pt`. El formato requiere que el texto "INFORME DE ACTIVIDADES No. XX" sea de **14pt**.

**Corrección:** Cambiar `.ph-center-title` a `font-size:14pt`.

---

### F4 — Filas de encabezado de tabla con fondo negro

**Hallazgo:** El CSS define `th{background:#000;color:#fff;}`. El formato institucional requiere fondo `#C0C0C0` (gris plata) con texto `#000` (negro) para las filas de encabezado de las tablas de secciones 2, 3, 4, 5.

**Corrección:** Cambiar a `th{background:#C0C0C0;color:#000;}`. Ajustar el borde de `#555` a `#999` para mantener contraste adecuado con el nuevo fondo claro.

---

### F5 — Texto de firma: día solo en número, falta texto en letras

**Hallazgo:** El método `nombreDia()` retorna solo el número del día (`String.valueOf(fecha.getDayOfMonth())`). El formato 11-IF-023 V1 exige: "al **veintidós (22)** días del mes de…", es decir, el ordinal en letras seguido del número entre paréntesis.

**Corrección:**
- Añadir constante `private static final String[] DIAS_EN_LETRAS` con los nombres para días 1–31 en español, usando entidades HTML para caracteres acentuados (convención del archivo).
- Actualizar `nombreDia(LocalDate)` para retornar `DIAS_EN_LETRAS[dia] + " (" + dia + ")"`.

**Array DIAS_EN_LETRAS (índice 0 sin uso, 1–31):**

| Día | Texto |
|-----|-------|
| 1 | uno |
| 2 | dos |
| 3 | tres |
| 4 | cuatro |
| 5 | cinco |
| 6 | seis |
| 7 | siete |
| 8 | ocho |
| 9 | nueve |
| 10 | diez |
| 11 | once |
| 12 | doce |
| 13 | trece |
| 14 | catorce |
| 15 | quince |
| 16 | diecis&#233;is |
| 17 | diecisiete |
| 18 | dieciocho |
| 19 | diecinueve |
| 20 | veinte |
| 21 | veintiuno |
| 22 | veintid&#243;s |
| 23 | veintitr&#233;s |
| 24 | veinticuatro |
| 25 | veinticinco |
| 26 | veintis&#233;is |
| 27 | veintisiete |
| 28 | veintiocho |
| 29 | veintinueve |
| 30 | treinta |
| 31 | treinta y uno |

---

### F6 — Caracteres especiales (tildes) en Sección 3: doble escape

**Hallazgo:** En `appendSeccion3`, la celda del item SGSSI se construye así:

```java
sb.append("<td><b>").append(esc(labelSgssi(aporte.getItem()))).append("</b></td>");
```

`labelSgssi(PENSION)` retorna `"PENSI&#211;N"` (entidad HTML). Al pasar por `esc()`, el `&` se convierte en `&amp;`, produciendo `"PENSI&amp;#211;N"` en el HTML, que el parser renderiza como texto literal `PENSI&#211;N` en lugar de `PENSIÓN`.

**Corrección:** Eliminar la llamada a `esc()` sobre `labelSgssi()`, ya que el método ya retorna HTML válido con entidades correctas. Cambiar a:

```java
sb.append("<td><b>").append(labelSgssi(aporte.getItem())).append("</b></td>");
```

---

## Archivos a modificar

| Archivo | Tipo de cambio |
|---------|---------------|
| `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` | 6 correcciones (CSS, lógica de negocio, rendering) |
| `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java` | Ampliar aserciones existentes + 2 tests nuevos |

No se requieren cambios de esquema de BD, migraciones, ni cambios en Angular.

---

## Criterios de aceptación

- [ ] F1: El PDF generado usa Arial (o Liberation Sans si Arial no está en el sistema). Log confirma ruta usada.
- [ ] F2: La tabla del running header muestra bordes en los 4 lados de cada celda en todas las páginas.
- [ ] F3: El texto "INFORME DE ACTIVIDADES No. XX" se renderiza en 14pt.
- [ ] F4: Las filas `<th>` de todas las tablas del documento tienen fondo `#C0C0C0` y texto `#000`.
- [ ] F5: La sección de firma muestra "al **cuatro (4)** días del mes de…" (texto + número).
- [ ] F6: En Sección 3, "PENSIÓN" se renderiza correctamente, no como `PENSI&#211;N`.
- [ ] Suite `mvn test`: BUILD SUCCESS, 0 failures.

---

## Stack de referencia

Java 8 · Spring Boot 2.7.18 · Flying Saucer (xhtmlrenderer) · OpenPDF (com.lowagie)
