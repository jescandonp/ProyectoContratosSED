# SIGCON I15 — Execution Log

**Fecha:** 2026-06-09
**Spec:** `docs/specs/2026-06-09-sigcon-i15-spec.md`
**Plan:** `docs/plans/2026-06-09-sigcon-i15-plan.md`
**Rama:** `main`

## Resumen

I15 corrige 6 desviaciones del formato PDF 11-IF-023 V1 identificadas en la revisión funcional visual del endpoint dev preview (post-I14):

1. **F1** — Fuente Arial: registrar en ITextRenderer con búsqueda de ruta candidata (Windows/Linux).
2. **F2** — Bordes tabla encabezado: `border:0.8pt solid #000` en cada celda `.ph-logo/.ph-center/.ph-right`.
3. **F3** — Título "INFORME DE ACTIVIDADES": `font-size` 9.5pt → 14pt.
4. **F4** — Encabezados `<th>`: `background:#000/color:#fff` → `background:#C0C0C0/color:#000`.
5. **F5** — Firma: día solo en número → texto en letras + número entre paréntesis (e.g., "cuatro (4)").
6. **F6** — Encoding Sección 3: `esc(labelSgssi())` causaba doble escape; eliminado `esc()`.

## Commits

| Commit | Descripción |
|--------|-------------|
| `810e33f` | `fix(i15): corregir formato PDF — titulo 14pt, th #C0C0C0, firma con letras+numero, encoding PENSION, bordes header, registro Arial` |

## Tareas

| Tarea | Estado | Evidencia |
|-------|--------|-----------|
| Task 1 — Tests RED (F2-F6) | Cerrada | 2 fallos confirmados en `InformePdfTemplateServiceTest` antes de implementar |
| Task 2 — Impl F3 font-size 14pt | Cerrada | `font-size:14pt` en `.ph-center-title` |
| Task 2 — Impl F4 th #C0C0C0 | Cerrada | `background:#C0C0C0;color:#000` en `th` |
| Task 2 — Impl F5 firma letras+número | Cerrada | `DIAS_EN_LETRAS[1..31]` + `nombreDia()` retorna `"texto (n)"` |
| Task 2 — Impl F6 encoding PENSIÓN | Cerrada | Eliminado `esc()` sobre `labelSgssi()` |
| Task 3 — Impl F2 bordes header | Cerrada | `border:0.8pt solid #000` en `.ph-logo`, `.ph-center`, `.ph-right` |
| Task 4 — Impl F1 registro Arial | Cerrada | `registrarFuenteArial()` busca `arial.ttf` en rutas candidatas OS |
| Task 5 — Suite completa | Cerrada | `mvn test`: BUILD SUCCESS — 244 tests, 0 failures, 0 errors |

## Verificación final

| Comando | Resultado |
|---------|-----------|
| `mvn test "-Dtest=InformePdfTemplateServiceTest"` | BUILD SUCCESS — 4 tests OK (3 existentes + 1 nuevo F6) |
| `mvn test` en `sigcon-backend` | BUILD SUCCESS — 244 tests, 0 failures, 0 errors |

## Notas de implementación

- El doble escape de `PENSI&#211;N` ocurría porque `labelSgssi()` ya retorna HTML con entidades, pero se pasaba por `esc()` que convierte `&` en `&amp;`. Solución: no pasar valores que ya son HTML seguro por `esc()`.
- `DIAS_EN_LETRAS` usa entidades HTML para tildes (diecis&#233;is, veintid&#243;s, etc.) siguiendo la convención del archivo, que evita problemas de codificación en el stream XML.
- `registrarFuenteArial()` es tolerante a fallos: si no encuentra ninguna ruta, registra advertencia y continúa (el PDF usa Helvetica como fallback del sans-serif).
- Los bordes del running header ahora se declaran en las celdas `td` en lugar de en la tabla, lo que garantiza renderizado correcto con Flying Saucer y `border-collapse:collapse`.

## Estado de cierre

I15 implementado, verificado con suite completa (244/244) y listo para push a `origin/main`.
