# SIGCON I15 — Implementation Plan
## Correcciones de Formato PDF 11-IF-023 V1

**Spec:** `docs/specs/2026-06-09-sigcon-i15-spec.md`
**Rama:** `main`
**Fecha:** 2026-06-09

---

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `sigcon-backend/.../service/InformePdfTemplateService.java` | F1–F6: fuente, bordes, tamaño título, color th, firma con letras, encoding |
| `sigcon-backend/src/test/.../InformePdfTemplateServiceTest.java` | Ampliar aserciones + 2 tests nuevos |

---

## Task 1 — Tests RED: añadir aserciones para F3, F4, F5, F6

**Archivo:** `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java`

### Step 1 — Ampliar test existente con aserciones F3, F4, F5

En el método `htmlMantieneFooterFueraDelFlujoSuperiorYFirmasAcotadas`, añadir al final:

```java
// F3: titulo INFORME DE ACTIVIDADES en 14pt
assertThat(html).contains("font-size:14pt");
// F4: th con fondo #C0C0C0
assertThat(html).contains("background:#C0C0C0");
// F5: dia en letras + numero (fechaElaboracion=2025-02-04 → "cuatro (4)")
assertThat(html).contains("cuatro (4)");
```

### Step 2 — Añadir test para F6 (encoding PENSIÓN)

Añadir un nuevo método de test al final de la clase:

```java
@Test
void seccion3NoDobleEscapeEntidadesHtmlEnItemSgssi() throws Exception {
    Informe informe = informe();
    AporteSgssi aporte = new AporteSgssi();
    aporte.setId(1L);
    aporte.setItem(co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi.PENSION);
    aporte.setEntidad("Colpensiones");
    aporte.setValorAportado(java.math.BigDecimal.valueOf(500000));
    aporte.setFechaPago(java.time.LocalDate.of(2025, 1, 10));
    aporte.setActivo(true);

    InformePdfTemplateService service = new InformePdfTemplateService(
        mock(ActividadInformeRepository.class),
        mock(SoporteAdjuntoRepository.class),
        mock(DocumentoAdicionalRepository.class),
        mock(AporteSgssiRepository.class)
    );

    java.lang.reflect.Method buildHtml = InformePdfTemplateService.class.getDeclaredMethod(
        "buildHtml",
        Informe.class,
        java.util.List.class,
        java.util.List.class,
        java.util.List.class,
        byte[].class, byte[].class, byte[].class
    );
    buildHtml.setAccessible(true);

    String html = (String) buildHtml.invoke(
        service, informe,
        Collections.emptyList(),
        Collections.emptyList(),
        java.util.List.of(aporte),
        new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 3}
    );

    // PENSI&#211;N debe aparecer tal cual (entidad HTML válida)
    assertThat(html).contains("PENSI&#211;N");
    // NO debe aparecer con doble escape
    assertThat(html).doesNotContain("PENSI&amp;#211;N");
}
```

### Step 3 — Verificar RED

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformePdfTemplateServiceTest"
```

Esperado: FAIL en las nuevas aserciones.

---

## Task 2 — Implementar F3, F4, F5, F6 en InformePdfTemplateService

**Archivo:** `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java`

### Step 1 — F3: font-size 14pt en ph-center-title

En `appendCss`, localizar:
```java
sb.append(".ph-center-title{font-weight:bold;font-size:9.5pt;}");
```
Reemplazar por:
```java
sb.append(".ph-center-title{font-weight:bold;font-size:14pt;}");
```

### Step 2 — F4: th background #C0C0C0

En `appendCss`, localizar:
```java
sb.append("th{background:#000;color:#fff;padding:3pt 5pt;text-align:left;font-size:8pt;font-weight:bold;border:0.5pt solid #555;}");
```
Reemplazar por:
```java
sb.append("th{background:#C0C0C0;color:#000;padding:3pt 5pt;text-align:left;font-size:8pt;font-weight:bold;border:0.5pt solid #999;}");
```

### Step 3 — F5: días en letras + número

Añadir constante estática antes del bloque `// ─── Helpers ────`:

```java
private static final String[] DIAS_EN_LETRAS = {
    "",           // 0 — sin uso
    "uno",        // 1
    "dos",        // 2
    "tres",       // 3
    "cuatro",     // 4
    "cinco",      // 5
    "seis",       // 6
    "siete",      // 7
    "ocho",       // 8
    "nueve",      // 9
    "diez",       // 10
    "once",       // 11
    "doce",       // 12
    "trece",      // 13
    "catorce",    // 14
    "quince",     // 15
    "diecis&#233;is",   // 16 — dieciséis
    "diecisiete", // 17
    "dieciocho",  // 18
    "diecinueve", // 19
    "veinte",     // 20
    "veintiuno",  // 21
    "veintid&#243;s",   // 22 — veintidós
    "veintitr&#233;s",  // 23 — veintitrés
    "veinticuatro",     // 24
    "veinticinco",      // 25
    "veintis&#233;is",  // 26 — veintiséis
    "veintisiete",      // 27
    "veintiocho",       // 28
    "veintinueve",      // 29
    "treinta",          // 30
    "treinta y uno"     // 31
};
```

Reemplazar el método `nombreDia`:
```java
// Antes:
private static String nombreDia(LocalDate fecha) {
    return String.valueOf(fecha.getDayOfMonth());
}

// Después:
private static String nombreDia(LocalDate fecha) {
    int dia = fecha.getDayOfMonth();
    return DIAS_EN_LETRAS[dia] + " (" + dia + ")";
}
```

### Step 4 — F6: eliminar esc() sobre labelSgssi

En `appendSeccion3`, localizar:
```java
sb.append("<td><b>").append(esc(labelSgssi(aporte.getItem()))).append("</b></td>");
```
Reemplazar por:
```java
sb.append("<td><b>").append(labelSgssi(aporte.getItem())).append("</b></td>");
```

### Step 5 — Verificar GREEN (tests F3-F6)

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformePdfTemplateServiceTest"
```

Esperado: BUILD SUCCESS, todos los tests del servicio pasan.

### Step 6 — Commit

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java
git commit -m "fix(i15): corregir formato PDF — titulo 14pt, th #C0C0C0, firma con letras, encoding PENSION"
```

---

## Task 3 — Implementar F2: bordes tabla encabezado running header

**Archivo:** `sigcon-backend/.../service/InformePdfTemplateService.java`

### Step 1 — Actualizar CSS ph-logo, ph-center, ph-right

En `appendCss`, localizar y reemplazar las tres clases del header:

```java
// Antes:
sb.append(".ph-logo{width:14%;text-align:center;padding:2pt;border-right:0.8pt solid #000;vertical-align:middle;}");
// ...
sb.append(".ph-center{width:60%;text-align:center;vertical-align:top;padding:2pt 4pt;}");
// ...
sb.append(".ph-right{width:26%;text-align:center;vertical-align:top;padding:2pt;border-left:0.8pt solid #000;}");

// Después:
sb.append(".ph-logo{width:14%;text-align:center;padding:2pt;border:0.8pt solid #000;vertical-align:middle;}");
// ...
sb.append(".ph-center{width:60%;text-align:center;vertical-align:top;padding:2pt 4pt;border:0.8pt solid #000;}");
// ...
sb.append(".ph-right{width:26%;text-align:center;vertical-align:top;padding:2pt;border:0.8pt solid #000;}");
```

### Step 2 — Añadir aserción de test para F2

En `InformePdfTemplateServiceTest`, en `htmlMantieneFooterFueraDelFlujoSuperiorYFirmasAcotadas`, añadir:

```java
// F2: celdas del header con borde completo
assertThat(html).contains(".ph-logo{width:14%;text-align:center;padding:2pt;border:0.8pt solid #000");
```

### Step 3 — Verificar GREEN suite completa

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

Esperado: BUILD SUCCESS, 0 failures.

### Step 4 — Commit

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java
git add sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformePdfTemplateServiceTest.java
git commit -m "fix(i15): bordes completos en tabla encabezado running header"
```

---

## Task 4 — Implementar F1: registro de fuente Arial en ITextRenderer

**Archivo:** `sigcon-backend/.../service/InformePdfTemplateService.java`

### Step 1 — Añadir método auxiliar registrarFuenteArial

Añadir antes del bloque `// ─── Helpers ────`:

```java
private void registrarFuenteArial(ITextRenderer renderer) {
    String[] candidatos = {
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/Arial.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
    };
    for (String ruta : candidatos) {
        if (new java.io.File(ruta).exists()) {
            try {
                renderer.getFontResolver().addFont(
                    ruta,
                    com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                    com.lowagie.text.pdf.BaseFont.EMBEDDED);
                log.debug("Fuente Arial registrada desde: {}", ruta);
                return;
            } catch (Exception e) {
                log.warn("No se pudo registrar fuente desde {}: {}", ruta, e.getMessage());
            }
        }
    }
    log.warn("Arial no encontrada en rutas candidatas; el PDF usara la fuente sans-serif por defecto.");
}
```

### Step 2 — Invocar registrarFuenteArial en generarPdf

En el método `generarPdf` (el que recibe listas), tras `ITextRenderer renderer = new ITextRenderer();`:

```java
ITextRenderer renderer = new ITextRenderer();
registrarFuenteArial(renderer);
renderer.setDocument(xmlDoc, null);
```

### Step 3 — Suite completa backend

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

Esperado: BUILD SUCCESS, 0 failures.

### Step 4 — Commit

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED
git add sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformePdfTemplateService.java
git commit -m "fix(i15): registrar fuente Arial en ITextRenderer para embedding correcto"
```

---

## Task 5 — Verificación final y execution log

### Step 1 — Suite completa

```powershell
Set-Location C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test
```

### Step 2 — Crear execution log

Crear `docs/plans/2026-06-09-sigcon-i15-execution-log.md`.

### Step 3 — Commit de cierre

```powershell
git add docs/specs/2026-06-09-sigcon-i15-spec.md
git add docs/plans/2026-06-09-sigcon-i15-plan.md
git add docs/plans/2026-06-09-sigcon-i15-execution-log.md
git commit -m "docs(i15): spec, plan y execution log — correcciones formato PDF"
git push origin main
```
