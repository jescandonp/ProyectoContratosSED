# SIGCON I14 — Execution Log

**Fecha:** 2026-05-26  
**Spec:** `docs/specs/2026-05-26-sigcon-i14-spec.md`  
**Plan:** `docs/plans/2026-05-26-sigcon-i14-plan.md`  
**Rama:** `main`

## Resumen

I14 corrige tres frentes funcionales:

1. `% Ejecucion Acumulada` editable por combinacion correcta de estado y rol.
2. Accion `escalar` desde Visto Bueno mueve el informe a `ENVIADO` para retornar a la cola del Revisor.
3. Vista de detalle del informe expone al actor `ADMIN` / `ADMINISTRATIVO` en `EN_VISTO_BUENO` actividades, soportes, documentos requeridos y panel de acciones VB.

## Commits

| Commit | Descripcion |
|--------|-------------|
| `672674d` | `fix(i14-r1): ampliar estados y roles para edicion de porcentaje ejecucion` |
| `75f5893` | `fix(i14-r2): escalar mueve informe a ENVIADO en lugar de EN_REVISION` |
| `0386ead` | `fix(i14-r1): corregir logica puedeEditarPorcentajeEjecucion por rol y estado` |
| `7827be3` | `feat(i14-r3): anadir senales y metodos VB al componente detalle` |
| `6e367fb` | `feat(i14-r3): vista VB completa con actividades, documentos y panel de acciones` |

## Tareas

| Tarea | Estado | Evidencia |
|-------|--------|-----------|
| Task 1 — Backend porcentaje por estados y roles | Cerrada | RED focalizado fallo por `SigconBusinessException`; GREEN `mvn test "-Dtest=InformeServiceTest"` con 25 tests OK |
| Task 2 — Backend `escalar` a `ENVIADO` | Cerrada | RED focalizado fallo por estado `EN_REVISION`; GREEN `mvn test "-Dtest=InformeServiceVbAccionesTest"` con 5 tests OK |
| Task 3 — Frontend `puedeEditarPorcentajeEjecucion` | Cerrada | `npm test -- --watch=false --include src/app/features/informes/detalle/informe-detalle.component.spec.ts` con 63 specs OK |
| Task 4 — Frontend senales y metodos VB | Cerrada | `npm run build` OK |
| Task 5 — Frontend template VB completo | Cerrada | `npm run build` OK |
| Task 6 — Verificacion final | Cerrada | Backend, Angular tests y build produccion OK |

## Verificacion final

| Comando | Resultado |
|---------|-----------|
| `mvn test` en `sigcon-backend` | BUILD SUCCESS — 243 tests, 0 failures, 0 errors |
| `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false` en `sigcon-angular` | TOTAL: 166 SUCCESS |
| `node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build` en `sigcon-angular` | Build OK — salida en `sigcon-angular/dist/sigcon-angular` |

## Notas de implementacion

- El controller ahora valida explicitamente compatibilidad rol-estado antes de delegar el PATCH de porcentaje.
- La suite completa requirio ajustar `InformeControllerVbTest` para mockear `findActiveInforme`, porque la nueva validacion consulta el estado antes de llamar a `actualizarPorcentajeEjecucion`.
- En Angular se uso el metodo existente `darVistosBueno` del `InformeService`, aunque el plan lo nombraba como `darVistoBueno`.
- La devolucion VB en el detalle reutiliza `observacionService.devolverInforme`; el backend ya enruta `ADMIN` / `ADMINISTRATIVO` hacia `devolverDesdeVistoBueno`.

## Estado de cierre

I14 queda implementado y verificado localmente. Queda pendiente push a `origin/main` como paso de publicacion.
