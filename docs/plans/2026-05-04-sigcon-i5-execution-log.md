# Execution Log — SIGCON Incremento 5
## Edición de Actividades en Informe BORRADOR desde Vista de Detalle

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Spec:** `docs/specs/2026-05-04-sigcon-i5-spec.md`  
> **Plan:** `docs/plans/2026-05-04-sigcon-i5-plan.md`  
> **Rama:** `feat/sigcon-i5`  
> **Base:** `feat/sigcon-i4` HEAD `7b61d09`  
> **Inicio:** 2026-05-04  
> **Estado:** ✅ CERRADO — 2026-05-04

---

## Contexto del Incremento

Gap identificado en pruebas funcionales de I4: `InformeDetalleComponent` muestra actividades siempre en modo solo lectura. El contratista no puede editar actividades de un informe en BORRADOR sin navegar a otra pantalla.

**Solución:** Activar modo edición inline en `InformeDetalleComponent` exclusivamente para `estado === 'BORRADOR'`. El backend ya tiene todos los endpoints necesarios desde I2.

| Tarea | Descripción | Estado | Commit |
|-------|-------------|--------|--------|
| T1 | Rama `feat/sigcon-i5` | ✅ completo | rama creada desde `7b61d09` |
| T2 | Lógica del componente (TypeScript) | ✅ completo | `2f13d84` |
| T3 | Template + tests + docs | ✅ completo | `dddf92d` + `d2e31f8` |

**Leyenda:** ✅ completo | 🔄 en progreso | ⬜ pendiente | ❌ bloqueado

---

## Estado del Sistema al Inicio del Incremento

- Backend: 123 tests, 0 fallos (rama `feat/sigcon-i4`, commit `7b61d09`)
- Frontend: 78 specs, 0 fallos
- DDL: sin cambios pendientes
- Endpoints necesarios: todos existen desde I2

---

## Registro de Ejecución

### 2026-05-04 — I5 completado

- T1: Rama `feat/sigcon-i5` creada desde `feat/sigcon-i4` HEAD `7b61d09`.
- T2 `2f13d84`: `InformeDetalleComponent` refactorizado con:
  - Interfaz `ActividadEditState` (descripcion, porcentaje, guardando, error, soportes).
  - `Map<number, ActividadEditState>` indexado por `actividad.id`.
  - Metodos: `guardarActividad`, `eliminarSoporte`, `agregarDocumentoAdicional`, `eliminarDocumentoAdicional`.
  - Template separado en `informe-detalle.component.html` con modo dual (editable en BORRADOR, solo lectura en otros estados).
  - Servicios inyectados: `ActividadInformeService`, `SoporteAdjuntoService`, `DocumentoAdicionalService`, `DocumentoCatalogoService`.
- T3 `dddf92d` + `d2e31f8`:
  - 12 tests nuevos en `informe-detalle.component.spec.ts`.
  - Test existente `loads and renders the report detail` adaptado al nuevo modo edicion.
  - Seccion 13 agregada a `docs/GUIA_PRUEBAS_FUNCIONALES.md`.
- Validacion final: **90 specs, 0 fallos** ✅

---

## Proximo Punto de Retoma

**Incremento 5 cerrado.** No hay punto de retoma pendiente.

## Metricas de Cierre

| Metrica | Meta | Resultado |
|---------|------|-----------|
| Backend tests | Sin regresion (123) | **123** ✅ |
| Frontend specs | >= 90, 0 fallos | **90** ✅ |
| Nuevos tests I5 | >= 12 | **12** ✅ |
| Tests existentes InformeDetalleComponent | 8 pasan | **8** ✅ (adaptado 1) |
| Endpoints nuevos | 0 | **0** ✅ |
| DDL changes | 0 | **0** ✅ |

1. **Autoridad:** CONSTITUTION → ARCHITECTURE → PRD → spec I5 → plan I5 → código.
2. **Solo frontend** — ningún archivo de backend se modifica.
3. **Solo BORRADOR** — DEVUELTO sigue usando `CorregirInformeComponent` sin cambios.
4. **Commits pequeños y trazables** — un commit por tarea.
5. **Tests antes de marcar tarea completa** — `ng test` verde en cada tarea frontend.
6. **Sin features de I6+** — solo lo que está en la spec I5.
7. **Regresión** — los 8 tests existentes de `InformeDetalleComponent` deben pasar sin modificación.
8. **TypeScript strict** — sin `any` implícito, sin `!` innecesarios.

---

## Proximo Punto de Retoma

**Incremento 5 cerrado.** No hay punto de retoma pendiente.
