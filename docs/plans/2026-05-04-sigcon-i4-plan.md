# Plan de Implementacion - SIGCON Incremento 4
## Hallazgos Funcionales: Revisor Opcional, Contrato Editable, Informe Editable

> **Metodologia:** Spec-Driven Development (SDD) - Spec-Anchored  
> **Version:** 1.0 - **Fecha:** 2026-05-04  
> **Spec de referencia:** `docs/specs/2026-05-04-sigcon-i4-spec.md`  
> **Rama:** `feat/sigcon-i4` (base: `feat/sigcon-i3` HEAD `9be9c73`)  
> **Estado:** Listo para ejecucion

---

## Resumen Ejecutivo

Incremento de 8 tareas que resuelve 3 hallazgos funcionales:

| Tarea | Scope | Hallazgo |
|-------|-------|----------|
| T1 | Infraestructura — rama + ErrorCode | H1 + H2 + H3 |
| T2 | Backend H1 — ContratoService revisor opcional | H1 |
| T3 | Backend H1 — InformeEstadoService sin revisor | H1 |
| T4 | Backend H2 — Contrato editable (DTO + endpoint) | H2 |
| T5 | Backend H3 — Informe editable (DTO + endpoint) | H3 |
| T6 | Backend tests + security patch | H1 + H2 + H3 |
| T7 | Frontend H1 + H2 — Admin form revisor opcional + edicion | H1 + H2 |
| T8 | Frontend H3 + E2E + docs | H3 |

---

## T1 — Infraestructura: Rama y ErrorCode

**Archivos a modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/exception/ErrorCode.java`

**Archivos a crear:**
- *(ninguno — solo modificacion de ErrorCode)*

**Acciones:**
1. Crear rama `feat/sigcon-i4` desde HEAD de `feat/sigcon-i3`.
2. Agregar `FECHA_FIN_INVALIDA` al enum `ErrorCode` bajo el bloque `// I4`.

**Commit:** `chore: bootstrap I4 branch and add FECHA_FIN_INVALIDA error code`

**Validacion:** `mvn compile` sin errores.

---

## T2 — Backend H1: ContratoService Revisor Opcional

**Spec:** seccion 4.1 y 4.5

**Archivos a modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ContratoService.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoCreateDto.java`

**Acciones:**
1. En `ContratoCreateDto`: eliminar validacion `@NotNull` de `idRevisor` si existe.
2. En `ContratoService`: agregar metodo privado `findActiveUsuarioOpcional(Long id, RolUsuario)` que retorna `null` cuando `id == null` (ver spec §4.5).
3. En `ContratoService.crear()`: reemplazar llamada `findActiveUsuario(request.getIdRevisor(), REVISOR, "revisor")` por `findActiveUsuarioOpcional(request.getIdRevisor(), REVISOR)`.
4. Verificar que `listarContratos()` cuando `usuario.getRol() == REVISOR` y `contrato.getRevisor() == null` no genere NPE (usar `findByRevisorAndActivoTrue` — si revisor es null, el contratista no es REVISOR, no hay riesgo).

**Commit:** `feat(i4): make revisor optional in ContratoService`

**Validacion:** `mvn test` — tests existentes de ContratoService deben pasar.

---

## T3 — Backend H1: InformeEstadoService Sin Revisor

**Spec:** seccion 4.1

**Archivos a modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeEstadoService.java`

**Acciones:**

1. Modificar `aprobar(Long informeId, String supervisorEmail)`:

```java
boolean sinRevisor = informe.getContrato().getRevisor() == null;
if (sinRevisor) {
    assertState(informe, EstadoInforme.ENVIADO, EstadoInforme.EN_REVISION);
} else {
    assertState(informe, EstadoInforme.EN_REVISION);
}
```

2. Modificar `devolver(Long informeId, String supervisorEmail, String observacion)`:

```java
boolean sinRevisor = informe.getContrato().getRevisor() == null;
if (sinRevisor) {
    assertState(informe, EstadoInforme.ENVIADO, EstadoInforme.EN_REVISION);
} else {
    assertState(informe, EstadoInforme.EN_REVISION);
}
```

> `aprobarRevision()` y `devolverRevision()` no se modifican. El NPE ya no ocurre porque `assertAssignedEmail(null, email)` lanza `ACCESO_DENEGADO`.

**Commit:** `feat(i4): supervisor acts directly from ENVIADO when no revisor`

**Validacion:** `mvn test` — todos los tests existentes deben pasar.

---

## T4 — Backend H2: Contrato Editable

**Spec:** seccion 4.2

**Archivos a crear:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/contrato/ContratoUpdateDto.java`

**Archivos a modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/ContratoService.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/ContratoController.java`

**Acciones:**

1. Crear `ContratoUpdateDto` con todos los campos de la spec §4.2.
2. En `ContratoService`: agregar metodo `actualizar(Long id, ContratoUpdateDto dto)`:
   - Cargar contrato (`CONTRATO_NO_ENCONTRADO` si no existe).
   - Validar unicidad de `numero` excluyendo `id` propio.
   - Resolver usuarios con `findActiveUsuario` / `findActiveUsuarioOpcional`.
   - Validar `fechaFin >= fechaInicio` (lanzar `FECHA_FIN_INVALIDA`).
   - Actualizar campos y persistir.
3. En `ContratoController`: agregar endpoint `PUT /api/admin/contratos/{id}` con `@PreAuthorize("hasRole('ADMIN')")`.

**Commit:** `feat(i4): add PUT /api/admin/contratos/{id} for contract update`

**Validacion:** `mvn test`.

---

## T5 — Backend H3: Informe Editable

**Spec:** seccion 4.6

**Archivos a crear:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/dto/informe/InformeUpdateDto.java`

**Archivos a modificar:**
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/InformeService.java`
- `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/web/controller/InformeController.java`

**Acciones:**

1. Crear `InformeUpdateDto` con `fechaInicio` y `fechaFin` (`@NotNull` en ambos).
2. En `InformeService`: agregar metodo `actualizar(Long id, InformeUpdateDto dto, String contratistaEmail)`:
   - Cargar informe (`INFORME_NO_ENCONTRADO` si no existe).
   - Validar estado BORRADOR o DEVUELTO (`INFORME_NO_EDITABLE`).
   - Validar propietario (`ACCESO_DENEGADO`).
   - Validar `fechaFin >= fechaInicio` (`FECHA_FIN_INVALIDA`).
   - Actualizar y persistir.
3. En `InformeController`: agregar endpoint `PATCH /api/informes/{id}` con `@PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")`.
   > El control de propietario se hace en el servicio, no en el controlador.

**Commit:** `feat(i4): add PATCH /api/informes/{id} for draft informe update`

**Validacion:** `mvn test`.

---

## T6 — Backend Tests y Security Patch

**Spec:** seccion 7 (Tests requeridos) y seccion 5 (Seguridad)

**Archivos a crear:**
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeEstadoServiceSinRevisorTest.java`

**Archivos a modificar:**
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/ContratoServiceTest.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/application/InformeServiceTest.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/SigconBackendSecurityTest.java`
- `sigcon-backend/src/test/java/co/gov/bogota/sed/sigcon/web/InformeSecurityTest.java`

**Tests a agregar — InformeEstadoServiceSinRevisorTest:**
- `aprobarSinRevisorDesdeEnviado`
- `devolverSinRevisorDesdeEnviado`
- `aprobarConRevisorDesdeEnviadoFalla`
- `supervisorNoAsignadoFallaEnFlujoSinRevisor`
- `revisorNoPuedeActuarEnContratoSinRevisor`
- `revisorRemovidoConInformeEnRevision`

**Tests a agregar — ContratoServiceTest:**
- `crearContratoSinRevisorExitoso`
- `actualizarContratoExitoso`
- `actualizarContratoNumeroDuplicadoFalla`
- `actualizarContratoAgregaRevisor`
- `actualizarContratoQuitaRevisor`

**Tests a agregar — InformeServiceTest:**
- `actualizarInformeBorradorExitoso`
- `actualizarInformeDevueltoExitoso`
- `actualizarInformeEnviadoFalla`
- `actualizarInformeAprobadoFalla`
- `actualizarInformeContratistaIncorrectoFalla`
- `actualizarInformeFechaFinInvalidaFalla`

**Parche de seguridad:**
- `SigconBackendSecurityTest`: agregar casos para `PUT /api/admin/contratos/{id}` y `PATCH /api/informes/{id}`.
- `InformeSecurityTest`: agregar casos para `PATCH /api/informes/{id}`.

**Commit:** `test(i4): add I4 unit tests and security patches`

**Validacion:** `mvn test` — todos los tests deben pasar (meta: >= 115 tests, 0 fallos).

---

## T7 — Frontend H1 + H2: Admin Form

**Spec:** secciones 6.1 y 6.2

**Archivos a modificar:**
- `sigcon-angular/src/app/features/admin/services/contrato.service.ts`
- `sigcon-angular/src/app/features/admin/components/admin-contrato-form/admin-contrato-form.component.ts`
- `sigcon-angular/src/app/features/admin/components/admin-contrato-form/admin-contrato-form.component.html`
- `sigcon-angular/src/app/features/admin/models/contrato.model.ts` *(agregar `ContratoUpdateDto`)*

**Archivos a crear:**
- *(ninguno — modificaciones sobre existentes)*

**Acciones:**

1. **H1 — Revisor opcional:**
   - En el modelo/DTO de creacion: `revisorId?: number | null`.
   - En el formulario: quitar validador `Validators.required` del campo revisor.
   - Mostrar etiqueta "Revisor (opcional)" sin asterisco.

2. **H2 — Modo edicion:**
   - Agregar boton "Editar contrato" en la vista de detalle del contrato.
   - Variable booleana `modoEdicion` que habilita/deshabilita campos del formulario.
   - Boton "Guardar": llama a `contratoService.actualizarContrato(id, dto)`.
   - Boton "Cancelar": restaura valores del formulario al estado original (sin HTTP).
   - Manejo de error 409 (`NUMERO_CONTRATO_DUPLICADO`): mensaje inline bajo el campo numero.
   - Toast de exito tras guardar, regreso a modo lectura.

3. **Agregar metodo en servicio:**

```typescript
actualizarContrato(id: number, dto: ContratoUpdateDto): Observable<ContratoDetalleDto> {
  return this.http.put<ContratoDetalleDto>(`${this.apiUrl}/${id}`, dto);
}
```

**Tests Angular:**
- `AdminContratoForm crea contrato sin revisor`
- `AdminContratoForm modo edicion activa campos`
- `AdminContratoForm guarda cambios y vuelve a lectura`

**Commit:** `feat(i4): admin contrato form - optional revisor and edit mode`

**Validacion:** `ng test` — todos los specs deben pasar.

---

## T8 — Frontend H3 + E2E + Documentacion

**Spec:** seccion 6.3

**Archivos a modificar:**
- `sigcon-angular/src/app/features/informes/services/informe.service.ts`
- `sigcon-angular/src/app/features/informes/components/informe-detail/informe-detail.component.ts`
- `sigcon-angular/src/app/features/informes/components/informe-detail/informe-detail.component.html`
- `sigcon-angular/src/app/features/informes/models/informe.model.ts` *(agregar `InformeUpdateDto`)*
- `docs/GUIA_PRUEBAS_FUNCIONALES.md`
- `docs/plans/2026-05-04-sigcon-i4-execution-log.md` *(cierre)*

**Acciones:**

1. **H3 — Informe editable:**
   - En `informe.model.ts`: agregar `InformeUpdateDto { fechaInicio: string; fechaFin: string }`.
   - En `informe.service.ts`: agregar `actualizarInforme(id, dto)` → `PATCH /api/informes/{id}`.
   - En `informe-detail.component`: detectar `estado == 'BORRADOR' || estado == 'DEVUELTO'` para mostrar PrimeNG Calendar editable en fechaInicio y fechaFin.
   - Boton "Guardar periodo" que llama al servicio y actualiza el estado local.
   - En otros estados: campos en modo solo lectura.

2. **E2E — Guia de pruebas funcionales:**
   - Agregar escenarios I4 en `docs/GUIA_PRUEBAS_FUNCIONALES.md`:
     - H1: Flujo completo sin revisor.
     - H2: Edicion de contrato (agregar/quitar revisor, cambiar numero).
     - H3: Edicion de periodo del informe en BORRADOR y DEVUELTO.
   - Actualizar tabla de diagnostico de errores comunes.

3. **Cierre del execution log.**

**Tests Angular:**
- `InformeDetail muestra campos editables en BORRADOR`
- `InformeDetail no muestra edicion en ENVIADO`
- `InformeDetail guarda periodo correctamente`

**Commit:** `feat(i4): informe detail period edit + I4 functional test guide`

**Validacion final:**
- `mvn test` → >= 115 tests, 0 fallos
- `ng test` → >= 80 specs, 0 fallos
- Revision manual: flujo sin revisor end-to-end, edicion de contrato, edicion de informe

---

## Orden de Ejecucion y Dependencias

```
T1 (rama + ErrorCode)
  └─ T2 (ContratoService revisor opcional)
  │    └─ T3 (InformeEstadoService sin revisor)
  │         └─ T4 (Contrato editable)
  │              └─ T5 (Informe editable)
  │                   └─ T6 (Tests + security)
  │                        └─ T7 (Frontend admin)
  │                             └─ T8 (Frontend informe + E2E + docs)
  └─ (lineal — cada tarea depende de la anterior)
```

---

## Metricas De Cierre

| Metrica | Meta |
|---------|------|
| Backend tests | >= 115, 0 fallos |
| Frontend specs | >= 80, 0 fallos |
| Cobertura H1 | Flujo sin revisor end-to-end |
| Cobertura H2 | Edicion de contrato con roles cambiados |
| Cobertura H3 | Edicion de periodo en BORRADOR y DEVUELTO |
| Sin regresion | Flujo con revisor identico al de I3 |
