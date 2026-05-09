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

---

## 2026-05-09 — Hallazgos Post-I5 Para Hardening

Se reciben hallazgos funcionales posteriores al cierre de I5. Se tratan como hardening sobre la rama vigente `feat/sigcon-i5` porque corrigen comportamiento comprometido por specs ya implementadas:

- I2 ya exige que el Revisor pueda aprobar la revision (`ENVIADO -> EN_REVISION`) o devolver con observacion obligatoria (`ENVIADO -> DEVUELTO`).
- I2 ya marca los documentos adicionales como parte del informe; el hallazgo ajusta la regla operativa a que todos los documentos adicionales aplicables sean obligatorios antes de enviar.
- I3 gobierna la barra/centro de notificaciones; el hallazgo de lectura inicial es ajuste UX sin cambio de dominio.
- Inconsistencia detectada antes de resolver: I2/I5 permitian soporte tipo `ARCHIVO` y `URL`, pero el hallazgo redefine el flujo actual de captura para soporte por obligacion como solo `nombre + URL`, dejando la carga de archivo como capacidad futura. La resolucion debe retirar la carga de archivo de la UI actual y conservar el backend/DDL compatible para evolucion posterior.

Punto de retoma operativo:

1. Ajustar informe nuevo y detalle BORRADOR para exigir soporte URL por obligacion y retirar input de archivo.
2. Exigir documentos adicionales aplicables antes de guardar/enviar y validar en backend al transicionar a `ENVIADO`.
3. Hacer accesibles las acciones de revision desde el detalle abierto por notificacion, no solo desde la cola.
4. Mejorar contraste del menu de notificaciones.
5. Actualizar pruebas y registrar resultados.

### Resolucion aplicada

- Informe nuevo:
  - Se retiro la carga de archivo en soportes de actividades.
  - Cada obligacion exige `nombre soporte` y `URL soporte`.
  - Todos los documentos adicionales del catalogo aplicable se tratan como obligatorios.
- Detalle de informe BORRADOR:
  - Se retiro la carga de archivo al agregar soporte.
  - Si una actividad no tiene soporte URL registrado, no permite guardar sin `nombre + URL`.
  - Antes de enviar valida soporte URL por actividad y documentos adicionales completos.
- Backend:
  - `InformeEstadoService.enviar()` valida al transicionar a `ENVIADO` que cada actividad tenga soporte tipo `URL`.
  - `InformeEstadoService.enviar()` valida que todos los documentos de catalogo aplicables al tipo de contrato esten registrados.
- Revision:
  - `InformeDetalleComponent` ahora expone acciones de Revisor para informes `ENVIADO`: aprobar revision o devolver con observacion obligatoria. Esto cubre el caso de entrar al detalle desde una notificacion.
- Notificaciones:
  - El menu de notificaciones cambia el fondo de no leidas a superficie clara, elimina truncamiento agresivo del titulo y permite mas lineas de descripcion.

### Validaciones 2026-05-09

- Backend enfocado: `mvn test "-Dtest=InformeEstadoServiceTest,InformeEstadoServiceI3Test,InformeEstadoServiceSinRevisorTest"` -> **25 tests, 0 fallos**.
- Backend completo: `mvn test` -> **125 tests, 0 fallos**.
- Frontend build: `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build` -> **exitoso**.
- Frontend specs enfocados: `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include ...` -> no concluye en 6 minutos fuera del sandbox. Se mantiene como limitacion de runner/browser del entorno; la compilacion Angular si quedo validada.

### Proximo Punto de Retoma

Revisar visualmente el flujo local-dev en navegador:

1. Crear informe: confirmar que cada obligacion exige nombre/URL de soporte y que no aparece input de archivo.
2. Crear informe: confirmar que todos los documentos adicionales exigen referencia.
3. Enviar informe: confirmar bloqueo si falta soporte URL o documento adicional.
4. Entrar como Revisor desde notificacion/detalle: confirmar botones `Aprobar revision` y `Devolver`.
5. Abrir campana de notificaciones: confirmar contraste legible en notificaciones no leidas.

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
