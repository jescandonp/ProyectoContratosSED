# Prompt de Implementacion — SIGCON I7 T11

> **Fecha:** 2026-05-12
> **Rama esperada:** `feat/sigcon-i7`
> **Modo:** Implementacion posterior a SDD
> **Fuente de verdad:** primero specs, luego plan, luego execution log

---

## Instruccion para la herramienta de codificacion

Vas a implementar la tarea **T11 — Mejora funcional post-pruebas 2026-05-12** del incremento SIGCON I7.

Antes de modificar codigo, lee en este orden:

1. `docs/specs/2026-05-11-sigcon-i7-spec.md`
   - Seccion clave: `0.2 Mejora Funcional Post-Pruebas — 2026-05-12`.
2. `docs/plans/2026-05-11-sigcon-i7-plan.md`
   - Seccion clave: `T11 — Mejora funcional post-pruebas 2026-05-12`.
3. `docs/plans/2026-05-11-sigcon-i7-execution-log.md`
   - Seccion clave: `Punto de Partida SDD Post-Pruebas 2026-05-12`.

No cambies el alcance sin actualizar primero esos documentos.

---

## Objetivo

Corregir dos frentes funcionales:

1. **Busqueda global administrativa**
   - Mantener texto libre como filtro opcional.
   - Agregar filtros combinables:
     - estado del contrato;
     - rango de periodo del informe;
     - contratista;
     - revisor;
     - estado del informe.
   - Retornar contratos e informes asociados.
   - Agregar paginacion con tamano inicial de 20 registros.
   - Aplicar ordenamiento default:
     1. periodo de informe mas reciente primero;
     2. prioridad operativa de estado: `EN_REVISION`, `ENVIADO`, `DEVUELTO`, `BORRADOR`, `APROBADO`;
     3. numero de contrato ascendente;
     4. contratista ascendente.

2. **Informe `DEVUELTO` editable por contratista**
   - En estado `DEVUELTO`, el contratista propietario puede editar integralmente:
     - actividades reportadas;
     - descripcion/detalle de actividades;
     - soportes asociados;
     - aportes a seguridad social;
     - documentos requeridos;
     - demas informacion editable del informe.
   - Al reenviar, el estado debe pasar de `DEVUELTO` a `ENVIADO`.
   - Los documentos requeridos cargados se conservan y pueden visualizarse, reemplazarse o eliminarse.
   - Los aportes de seguridad social se editan campo a campo.
   - Al crear/diligenciar informe, precargar datos predeterminados del usuario para `SALUD`, `PENSION` y `ARL`, si existen, permitiendo ajustes manuales.

---

## Restricciones

- No revertir cambios existentes.
- No tocar archivos no relacionados.
- No limpiar archivos no versionados del workspace.
- Mantener la maquina de estados vigente:
  - `BORRADOR`
  - `ENVIADO`
  - `EN_REVISION`
  - `DEVUELTO`
  - `APROBADO`
- No reintroducir seccion funcional de `Documentos adicionales`.
- Mantener una sola seccion documental: **Documentos Requeridos**.
- Preservar permisos por rol:
  - Contratista edita solo sus informes en `BORRADOR` o `DEVUELTO`.
  - Contratista en `ENVIADO` solo consulta/vista previa.
  - Revisor revisa desde `ENVIADO`.
  - Supervisor aprueba/devuelve desde `EN_REVISION`.

---

## Archivos candidatos

Revisar los nombres reales antes de editar.

Backend busqueda:

- `sigcon-backend/src/main/java/.../BusquedaAdminController.java`
- `sigcon-backend/src/main/java/.../BusquedaAdminService.java`
- DTOs de busqueda administrativa.
- Tests `*Busqueda*Test`.

Backend informes/estado:

- `InformeService`
- `InformeEstadoService`
- DTOs de crear/actualizar informe.
- Tests `InformeServiceTest`, `InformeEstadoServiceTest`.

Backend usuarios/aportes:

- modelo/DTO de usuario contratista con datos predeterminados de seguridad social.
- servicio que construye el formulario o respuesta inicial de informe.

Frontend busqueda:

- pantalla `/admin/busqueda`.
- servicio Angular de busqueda.
- specs de busqueda admin.

Frontend informe devuelto:

- `informe-detalle.component`
- `corregir-informe.component`
- `informe-form.component`
- servicios/modelos de informe y aportes.

---

## Validaciones esperadas

Backend:

```powershell
Set-Location sigcon-backend
mvn test "-Dtest=*Busqueda*Test,InformeServiceTest,InformeEstadoServiceTest"
```

Frontend:

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false --include src/app/features/admin/busqueda/*.spec.ts --include src/app/features/informes/**/*.spec.ts
```

Si el filtro `--include` con comodines no funciona en el entorno Angular, reemplazar por los archivos spec concretos encontrados con `Get-ChildItem`.

---

## Cierre esperado

Al terminar:

1. Actualizar `docs/plans/2026-05-11-sigcon-i7-execution-log.md` con:
   - cambios realizados;
   - comandos ejecutados;
   - resultados de pruebas;
   - commit generado;
   - pendientes funcionales si existen.
2. Hacer commit de implementacion sugerido:

```powershell
git add <archivos modificados>
git commit -m "fix: apply SIGCON I7 post-test functional fixes"
```

3. Publicar la rama:

```powershell
git push
```

---

## Prompt corto copiable

Implementa SIGCON I7 T11 en la rama `feat/sigcon-i7`. Antes de tocar codigo, lee `docs/specs/2026-05-11-sigcon-i7-spec.md` seccion `0.2`, `docs/plans/2026-05-11-sigcon-i7-plan.md` seccion `T11` y `docs/plans/2026-05-11-sigcon-i7-execution-log.md` seccion `Punto de Partida SDD Post-Pruebas 2026-05-12`. Implementa busqueda global con texto opcional, filtros por estado contrato, periodo informe, contratista, revisor y estado informe, resultados contrato+informes, paginacion de 20 y ordenamiento default definido. Corrige informe `DEVUELTO` para que contratista edite todo, incluyendo actividades, soportes, documentos requeridos y aportes; al reenviar pasa a `ENVIADO`; precarga `SALUD`, `PENSION` y `ARL` desde datos predeterminados del usuario al diligenciar informe. No reintroduzcas `Documentos adicionales`. Actualiza execution log con pruebas y commit. Ejecuta pruebas backend/frontend focalizadas indicadas en el plan.
