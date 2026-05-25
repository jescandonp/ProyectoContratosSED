# I11 — Correcciones Formato PDF 11-IF-023 V1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar 6 GAPs de presentación en el PDF institucional SED para que coincida exactamente con el formato 11-IF-023 V1.

**Architecture:** Todos los cambios están en un solo servicio — `InformePdfTemplateService.java` — que genera XHTML renderizado por Flying Saucer + OpenPDF. Los cambios son quirúrgicos: CSS, estructura HTML del header/footer y una fila de tabla. Sin cambios en lógica de negocio, entidades ni controladores.

**Tech Stack:** Java 8+, Flying Saucer (xhtmlrenderer) + OpenPDF, CSS paged media (`@page`, running elements, `counter(page)`, `counter(pages)`).

---

## Archivos que se modifican

| Archivo | Acción |
|---------|--------|
| `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` | Modificar: CSS, header, footer, sección 1, sección 2 |
| `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/PdfInformeServiceTest.java` | Revisar tests existentes — no deben romperse |

---

## GAPs que cierra este plan

| GAP | Descripción |
|-----|-------------|
| 1 | Header: agregar paginación "Página X de Y" |
| 2 | Mover "11-IF-023 V1" del header al footer |
| 3 | Fecha Inicio + Terminación en la misma fila (4 columnas) |
| 4 | Evidencia Verificable: URL como `<a href>` real |
| 5 | Footer: agregar paginación "Página X de Y" |
| 6 | Footer: validar/ajustar formato visual de la dirección SED |

---

## Task 1: CSS — paginación y posicionamiento del código de formato

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` método `appendCss()` (líneas 174–215)

**Contexto:** Flying Saucer soporta CSS paged media. Para mostrar el número de página en un running element, se usa `counter(page)` y `counter(pages)` dentro del contenido CSS de ese elemento. La clase `.page-num` ya está definida pero no se usa.

- [ ] **Paso 1: Localizar `appendCss()` en `InformePdfTemplateService.java`**

Abrir el archivo en la línea 174. Identificar las líneas que definen:
```
.page-num:before{content:counter(page);}
.page-total:before{content:counter(pages);}
```
Estas clases están definidas pero no hay elementos que las usen en el HTML.

- [ ] **Paso 2: Agregar clase CSS `.fmt-code` para el código de formato en el footer**

En `appendCss()`, después de la línea `.running-footer{...}`, agregar la clase que separará visualmente el código del resto del footer. Reemplazar el bloque de `.running-footer` así:

```java
sb.append(".running-footer{position:running(pageFooter);width:100%;");
sb.append("font-size:7.5pt;color:#444;border-top:0.5pt solid #ccc;padding-top:3pt;}");
sb.append(".footer-inner{display:table;width:100%;}");
sb.append(".footer-left{display:table-cell;text-align:left;width:70%;}");
sb.append(".footer-right{display:table-cell;text-align:right;width:30%;font-weight:bold;}");
```

- [ ] **Paso 3: Compilar para verificar que el archivo es sintácticamente válido**

```bash
cd sigcon-backend
mvn compile -q
```
Esperado: BUILD SUCCESS sin errores.

---

## Task 2: Footer — paginación + código de formato + dirección SED

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` método `buildHtml()` (líneas 154–157)

**Contexto:** El footer actual es una sola línea de texto con la dirección. Necesita: (a) la dirección SED a la izquierda, (b) "11-IF-023 V1" retirado del header y puesto en el footer, (c) "Página X de Y" en el footer.

- [ ] **Paso 1: Reemplazar el bloque del footer en `buildHtml()`**

Buscar las líneas (aproximadamente 154–157):
```java
sb.append("<div class=\"running-footer\">");
sb.append("Avenida El Dorado N&#176; 66-63 &nbsp; PBX: 3241000 &nbsp; ");
sb.append("www.educacionbogota.edu.co &nbsp; L&#237;nea 195");
sb.append("</div>");
```

Reemplazar con:
```java
sb.append("<div class=\"running-footer\">");
sb.append("<div class=\"footer-inner\">");
sb.append("<div class=\"footer-left\">");
sb.append("Avenida El Dorado N&#176; 66-63 &nbsp;&nbsp; PBX: 3241000 &nbsp;&nbsp; ");
sb.append("www.educacionbogota.edu.co &nbsp;&nbsp; L&#237;nea 195");
sb.append("</div>");
sb.append("<div class=\"footer-right\">");
sb.append("11-IF-023 V1 &nbsp; P&#225;gina <span class=\"page-num\"></span> de <span class=\"page-total\"></span>");
sb.append("</div>");
sb.append("</div>");
sb.append("</div>");
```

- [ ] **Paso 2: Compilar**

```bash
cd sigcon-backend
mvn compile -q
```
Esperado: BUILD SUCCESS.

---

## Task 3: Header — quitar "11-IF-023 V1" y agregar paginación

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` método `appendRunningHeader()` (líneas 219–254)

**Contexto:** La celda derecha del header (`ph-right`) actualmente tiene un `<div class="ph-code">11-IF-023 V1</div>` (línea 249) que debe eliminarse porque se movió al footer. En su lugar, el header debe mostrar "Página X de Y".

- [ ] **Paso 1: Eliminar el `ph-code` div del header**

Buscar (aproximadamente línea 249):
```java
sb.append("<div class=\"ph-code\">11-IF-023 V1</div>");
```
Eliminar esa línea completa.

- [ ] **Paso 2: Agregar paginación en la celda derecha del header**

Después de las líneas de `ph-right-period` (período del informe), agregar:
```java
sb.append("<div class=\"ph-right-period\" style=\"margin-top:3pt;font-weight:bold;\">");
sb.append("P&#225;g. <span class=\"page-num\"></span> / <span class=\"page-total\"></span>");
sb.append("</div>");
```

- [ ] **Paso 3: Eliminar la clase CSS `.ph-code` que ya no se necesita**

En `appendCss()`, buscar y eliminar:
```java
sb.append(".ph-code{text-align:right;font-size:7pt;color:#555;padding:1pt 2pt;border-top:0.5pt solid #aaa;}");
```

- [ ] **Paso 4: Compilar**

```bash
cd sigcon-backend
mvn compile -q
```
Esperado: BUILD SUCCESS.

---

## Task 4: Sección 1 — Fecha Inicio y Terminación en la misma fila

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` método `appendSeccion1()` (líneas 258–294)

**Contexto:** El helper `fila2()` genera una fila de 2 columnas (label | value). Para poner Fecha Inicio y Terminación en la misma fila se necesita una fila de 4 columnas: `[label1 | val1 | label2 | val2]`. Se crea el helper `fila4()` para este patrón.

- [ ] **Paso 1: Agregar el helper `fila4()` en la sección de helpers**

Al final de la sección de helpers (después de `fila2()`, aproximadamente línea 508), agregar:
```java
private static void fila4(StringBuilder sb, String label1, String value1,
                            String label2, String value2, boolean alt) {
    sb.append(alt ? "<tr style=\"background:#f7f7f7\">" : "<tr>");
    sb.append("<td class=\"lbl\" style=\"width:15%\">").append(label1).append("</td>");
    sb.append("<td class=\"val\" style=\"width:35%\">").append(value1).append("</td>");
    sb.append("<td class=\"lbl\" style=\"width:15%\">").append(label2).append("</td>");
    sb.append("<td class=\"val\" style=\"width:35%\">").append(value2).append("</td>");
    sb.append("</tr>");
}
```

- [ ] **Paso 2: Reemplazar las dos llamadas `fila2` por una llamada `fila4` en `appendSeccion1()`**

Buscar las líneas (aproximadamente 283–284):
```java
fila2(sb, "Fecha de Inicio:", c.getFechaInicio().format(DATE_FMT), false);
fila2(sb, "Fecha de Terminaci&#243;n:", c.getFechaFin().format(DATE_FMT), true);
```

Reemplazar con:
```java
fila4(sb,
    "Fecha de Inicio:", c.getFechaInicio().format(DATE_FMT),
    "Fecha de Terminaci&#243;n:", c.getFechaFin().format(DATE_FMT),
    false);
```

- [ ] **Paso 3: Compilar**

```bash
cd sigcon-backend
mvn compile -q
```
Esperado: BUILD SUCCESS.

---

## Task 5: Sección 2 — Evidencia Verificable como hipervínculo real

**Files:**
- Modify: `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java` método `appendSeccion2()` (líneas 296–350)

**Contexto:** Para soportes tipo `URL`, el campo `s.getReferencia()` contiene la URL completa pero actualmente no se usa como `href`. Flying Saucer sí renderiza `<a href>` en PDFs. La URL debe ir en el atributo `href` y el nombre en el texto visible. El método `esc()` escapa el HTML pero las URLs tienen caracteres que también deben escaparse en atributos.

- [ ] **Paso 1: Reemplazar el bloque de renderizado de soportes tipo URL**

Buscar las líneas (aproximadamente 335–339):
```java
if (s.getTipo() == co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte.URL
        && notEmpty(s.getReferencia())) {
    sb.append("<div><u>").append(esc(s.getNombre())).append("</u></div>");
} else {
    sb.append("<div>").append(esc(s.getNombre())).append("</div>");
}
```

Reemplazar con:
```java
if (s.getTipo() == co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte.URL
        && notEmpty(s.getReferencia())) {
    sb.append("<div><a href=\"").append(esc(s.getReferencia())).append("\"")
      .append(" style=\"color:#0a0e5a;text-decoration:underline;\">")
      .append(esc(s.getNombre())).append("</a></div>");
} else {
    sb.append("<div>").append(esc(s.getNombre())).append("</div>");
}
```

- [ ] **Paso 2: Compilar**

```bash
cd sigcon-backend
mvn compile -q
```
Esperado: BUILD SUCCESS.

---

## Task 6: Verificación visual y tests

**Files:**
- Read: `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/PdfInformeServiceTest.java`

- [ ] **Paso 1: Ejecutar los tests existentes de PDF**

```bash
cd sigcon-backend
mvn test -pl . -Dtest=PdfInformeServiceTest -q
```
Esperado: BUILD SUCCESS, todos los tests pasan. Si algún test usa `contains("11-IF-023 V1")` en el header, actualizarlo para buscar en el footer.

- [ ] **Paso 2: Verificar que el test de la plantilla HTML no rompe**

```bash
cd sigcon-backend
mvn test -q
```
Esperado: BUILD SUCCESS, sin tests fallidos.

- [ ] **Paso 3: Commit final**

```bash
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java
git commit -m "fix(pdf): cerrar GAPs formato 11-IF-023 V1 — paginacion, footer, fechas fila, hipervinculos"
```

---

## Task 7: Actualizar execution log y README

**Files:**
- Create: `docs/plans/2026-05-22-sigcon-i11-execution-log.md`
- Modify: `README.md`

- [ ] **Paso 1: Crear execution log de I11**

Crear el archivo `docs/plans/2026-05-22-sigcon-i11-execution-log.md` con contenido:

```markdown
# SIGCON I11 — Execution Log

**Incremento:** I11 — Correcciones Formato PDF 11-IF-023 V1
**Inicio:** 2026-05-22
**Estado:** ✅ CERRADO

## Resumen de tareas

| Task | Descripción | Estado |
|------|-------------|--------|
| T1 | CSS: paginación y clase footer-inner | ✅ Completado |
| T2 | Footer: paginación + código formato + dirección | ✅ Completado |
| T3 | Header: quitar ph-code, agregar paginación | ✅ Completado |
| T4 | Sección 1: Fecha Inicio/Terminación misma fila | ✅ Completado |
| T5 | Sección 2: Evidencia Verificable como hipervínculo | ✅ Completado |
| T6 | Tests y verificación | ✅ Completado |

## GAPs cerrados

- GAP 1: Header — paginación "Pág. X / Y"
- GAP 2: "11-IF-023 V1" movido del header al footer
- GAP 3: Fecha Inicio y Terminación en misma fila de 4 columnas
- GAP 4: Evidencia Verificable con `<a href>` real para tipo URL
- GAP 5: Footer — paginación "Página X de Y"
- GAP 6: Footer — formato dirección SED validado
```

- [ ] **Paso 2: Actualizar `README.md`**

En la tabla de incrementos, cambiar la fila de I11:
```
| I11 | Correcciones Formato PDF 11-IF-023 V1 | Cerrado |
```

Y actualizar: `**Último incremento cerrado: I11 — Correcciones Formato PDF 11-IF-023 V1**`

- [ ] **Paso 3: Commit de documentación**

```bash
git add docs/plans/2026-05-22-sigcon-i11-execution-log.md README.md
git commit -m "docs(i11): cerrar incremento correcciones formato PDF"
```

---

## Checklist de cobertura de GAPs

| GAP | Task que lo cierra |
|-----|--------------------|
| 1 — Header paginación | Task 3 |
| 2 — "11-IF-023 V1" al footer | Task 2 + Task 3 |
| 3 — Fechas misma fila | Task 4 |
| 4 — URL como hipervínculo | Task 5 |
| 5 — Footer paginación | Task 2 |
| 6 — Footer dirección formato | Task 2 |
