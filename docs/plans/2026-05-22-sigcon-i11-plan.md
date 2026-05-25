# Plan de Implementacion — SIGCON I11
## Ajustes de Formato PDF 11-IF-023 V1

> Spec de referencia: `docs/specs/2026-05-22-sigcon-i11-spec.md`  
> Fecha: 2026-05-22  
> Estado: Listo para ejecucion

---

## Resumen

Los cambios son acotados a la generacion de PDF. Se modifican:

- `InformePdfTemplateService.java` para layout XHTML/CSS.
- `PdfInformeService.java` para validar firma de revisor asignado.
- `PdfInformeServiceTest.java` para cubrir la regla nueva.
- `docs/plans/2026-05-22-sigcon-i11-execution-log.md` para trazabilidad.

---

## Tareas

- [x] T1 — Documentacion SDD I11
  - Acceptance: spec y plan existen en `docs/specs/` y `docs/plans/`.
  - Verify: lectura de ambos archivos.
  - Files: `docs/specs/2026-05-22-sigcon-i11-spec.md`, `docs/plans/2026-05-22-sigcon-i11-plan.md`.

- [x] T2 — Header PDF
  - Acceptance: celda derecha muestra `Pagina X de Y` arriba, luego `PERIODO DEL INFORME`, luego Desde/Hasta.
  - Verify: revision de `appendRunningHeader()`.
  - Files: `InformePdfTemplateService.java`.

- [x] T3 — Seccion 1 fechas
  - Acceptance: `Fecha de Inicio` y `Fecha de Terminacion` usan fila de 4 columnas sin colapso.
  - Verify: revision de helper `fila4()`.
  - Files: `InformePdfTemplateService.java`.

- [x] T4 — Footer PDF
  - Acceptance: codigo arriba-derecha en dos lineas, paginacion centrada, direccion centrada abajo.
  - Verify: revision de `running-footer` y CSS.
  - Files: `InformePdfTemplateService.java`.

- [x] T5 — Firmas y validacion
  - Acceptance: revisor asignado sin firma bloquea PDF con `FIRMA_REQUERIDA`; firmas se renderizan con caja controlada.
  - Verify: `mvn test -Dtest=PdfInformeServiceTest`.
  - Files: `PdfInformeService.java`, `InformePdfTemplateService.java`, `PdfInformeServiceTest.java`.

- [x] T6 — Execution log
  - Acceptance: execution log I11 refleja GAPs nuevos, tareas y evidencia.
  - Verify: lectura de `docs/plans/2026-05-22-sigcon-i11-execution-log.md`.
  - Files: `docs/plans/2026-05-22-sigcon-i11-execution-log.md`.

---

## Riesgos

| Riesgo | Mitigacion |
|--------|------------|
| Flying Saucer interpreta mal anchos de tabla | Usar clases especificas de fila de fechas y `white-space:nowrap` |
| Footer se monta sobre contenido | Mantener `@page` con margen inferior suficiente y footer compacto |
| Firma horizontal domina la pagina | Usar contenedor fijo y maximos mas bajos |
| Revisor asignado sin archivo fisico | Si no tiene ruta de firma, falla `FIRMA_REQUERIDA`; si la ruta falla al cargar, falla la generacion |
