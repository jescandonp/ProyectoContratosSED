# SIGCON I14 — Spec Técnica
## Correcciones Funcionales: % Ejecución y Vista Visto Bueno

**Fecha:** 2026-05-26
**Incremento:** I14
**Origen:** Hallazgos de validación funcional sobre I12
**Rama:** main

---

## Contexto

Durante las pruebas funcionales de I12 se identificaron dos comportamientos incorrectos:

1. El campo `% Ejecución Acumulada` no es editable en todos los estados y por todos los actores que lo requieren.
2. El perfil ADMINISTRATIVO en estado `EN_VISTO_BUENO` no puede ver el contenido completo del informe (actividades, soportes, documentos adjuntos) ni tiene los botones de acción integrados en la vista de detalle.

Adicionalmente, se detectó que la acción `escalar` del backend mueve el informe a `EN_REVISION` (cola del Supervisor) cuando debería moverlo a `ENVIADO` (cola del Revisor).

---

## Requerimiento 1 — % Ejecución: estados y roles ampliados

### Comportamiento esperado

| Estado del informe | Rol que puede editar |
|--------------------|----------------------|
| `BORRADOR` | `CONTRATISTA` |
| `ENVIADO` | `REVISOR` |
| `EN_REVISION` | `REVISOR`, `ADMIN`, `ADMINISTRATIVO` |
| `EN_VISTO_BUENO` | `ADMIN`, `ADMINISTRATIVO` |

El campo es de solo lectura en cualquier otra combinación estado/rol.

### Cambios backend

**Archivo:** `sigcon-backend/.../application/service/InformeService.java`

- Método `actualizarPorcentajeEjecucion`: ampliar la validación de estado de 2 a 4 valores permitidos (`BORRADOR`, `ENVIADO`, `EN_REVISION`, `EN_VISTO_BUENO`).
- La validación de que el rol del llamante sea compatible con el estado se realiza en el controller.

**Archivo:** `sigcon-backend/.../web/controller/InformeController.java`

- Endpoint `PATCH /{id}/porcentaje-ejecucion`: ampliar `@PreAuthorize` para incluir `CONTRATISTA`.
- Añadir validación explícita de compatibilidad rol-estado (tabla anterior). Retornar `403` si el rol no corresponde al estado actual del informe.

**Archivo:** `sigcon-backend/.../application/InformeServiceTest.java`

- Agregar casos: CONTRATISTA edita en BORRADOR ✅, REVISOR edita en ENVIADO ✅, CONTRATISTA intenta editar en ENVIADO ❌ (403), REVISOR intenta editar en BORRADOR ❌ (403).

### Cambios frontend

**Archivo:** `sigcon-angular/.../informes/detalle/informe-detalle.component.ts`

Reemplazar `puedeEditarPorcentajeEjecucion`:

```typescript
puedeEditarPorcentajeEjecucion(informe: InformeDetalle): boolean {
  const e = informe.estado;
  if (e === 'BORRADOR') return this.authService.hasRole('CONTRATISTA');
  if (e === 'ENVIADO') return this.authService.hasRole('REVISOR');
  if (e === 'EN_REVISION') return this.authService.hasRole('REVISOR')
    || this.authService.hasRole('ADMIN')
    || this.authService.hasRole('ADMINISTRATIVO');
  if (e === 'EN_VISTO_BUENO') return this.authService.hasRole('ADMIN')
    || this.authService.hasRole('ADMINISTRATIVO');
  return false;
}
```

---

## Requerimiento 2 — Corregir `escalar`: estado destino `ENVIADO`

### Comportamiento esperado

Cuando el ADMINISTRATIVO elige "Escalar al Revisor", el informe vuelve a la cola del Revisor (estado `ENVIADO`), no al Supervisor. El Revisor puede entonces:
- Editar `% ejecución` (cubierto por R1).
- Devolver al Contratista si detecta más errores (flujo existente `devolver-revision`).
- Re-aprobar la revisión, lo que retorna el informe al flujo VB o EN_REVISION según parámetro `VB_ACTIVO`.

### Cambios backend

**Archivo:** `sigcon-backend/.../application/service/InformeEstadoService.java`

- Método `escalar`: cambiar estado destino de `EN_REVISION` a `ENVIADO`.
- Mantener evento `VB_ESCALADO` y acción `ESCALACION` en observación para trazabilidad.
- El requisito de observación se mantiene (recomendada, no bloqueante).

```java
// Antes:
informe.setEstado(EstadoInforme.EN_REVISION);
// Después:
informe.setEstado(EstadoInforme.ENVIADO);
```

**Archivo:** test correspondiente: verificar que el estado resultante es `ENVIADO`.

---

## Requerimiento 3 — Vista completa para ADMINISTRATIVO en `EN_VISTO_BUENO`

### Comportamiento esperado

Cuando un usuario con rol `ADMIN` o `ADMINISTRATIVO` abre un informe en estado `EN_VISTO_BUENO`, el componente `informe-detalle` debe mostrar:

1. **Datos generales** — período, contrato, contratista (ya visible).
2. **% Ejecución Acumulada editable** (cubierto por R1).
3. **Actividades con soportes** — lista completa de actividades del informe, con sus soportes adjuntos (URLs y archivos), en modo solo lectura. Reutilizar la estructura de renderizado de `informe-preview`.
4. **Documentos requeridos** — lista de documentos obligatorios con indicador de estado (adjunto / pendiente), en modo solo lectura.
5. **Observaciones históricas** — historial de observaciones del informe.
6. **Panel de acciones VB** — tres botones con sus comportamientos:

| Botón | Observación | Acción de servicio | Estado destino |
|-------|-------------|-------------------|----------------|
| Dar Visto Bueno | Opcional | `informeService.darVistoBueno(id, obs)` | `EN_REVISION` |
| Escalar al Revisor | Obligatoria | `informeService.escalar(id, obs)` | `ENVIADO` (post R2) |
| Devolver al Contratista | Obligatoria | `observacionService.devolverDesdeVistoBueno(id, obs)` | `DEVUELTO` |

### Cambios frontend

**Archivo:** `sigcon-angular/.../informes/detalle/informe-detalle.component.ts`

- Añadir función `puedeActuarVB(informe)`: retorna `true` si `(ADMIN || ADMINISTRATIVO) && estado === EN_VISTO_BUENO`.
- Añadir señales: `procesandoVB`, `errorVB`, `observacionVB`.
- Añadir métodos: `darVistoBueno()`, `escalarAlRevisor()`, `devolverDesdeVB()`.
- En `ngOnInit / cargar()`: cuando `puedeActuarVB`, cargar también `documentosRequeridos` (ya existe `documentoRequeridoService.listar()`).

**Template del componente detalle** — nueva sección condicional `*ngIf="puedeActuarVB(informe())"`:

```
[Actividades con soportes — solo lectura, estructura de informe-preview]
[Documentos requeridos — solo lectura]
[Panel de acciones: Dar VB / Escalar / Devolver con campo de observación]
```

La sección de actividades en modo VB es **solo lectura**: sin botones de edición, sin upload de soportes. Se reutiliza el bloque de renderizado existente en `informe-preview.component.ts` como referencia de estructura, no como componente importado.

**Archivo:** `sigcon-angular/.../services/observacion.service.ts`

- Verificar que exista `devolverDesdeVistoBueno(informeId, request)`. Si no existe, añadirlo apuntando al endpoint `/informes/{id}/devolver-desde-visto-bueno`.

---

## Criterios de aceptación

### R1 — % Ejecución
- [ ] Contratista ve campo editable en BORRADOR, solo lectura en ENVIADO y posteriores.
- [ ] Revisor ve campo editable en ENVIADO y EN_REVISION, solo lectura en BORRADOR.
- [ ] ADMINISTRATIVO ve campo editable en EN_VISTO_BUENO.
- [ ] Backend rechaza (403) combinaciones rol-estado no permitidas.

### R2 — Escalar
- [ ] Al escalar desde EN_VISTO_BUENO, el informe queda en estado ENVIADO.
- [ ] El informe aparece en la cola del Revisor tras escalar.
- [ ] La observación del ADMINISTRATIVO queda registrada con acción ESCALACION.
- [ ] El Revisor puede re-aprobar o devolver desde ENVIADO.

### R3 — Vista VB
- [ ] ADMINISTRATIVO ve actividades con soportes (solo lectura) en EN_VISTO_BUENO.
- [ ] ADMINISTRATIVO ve documentos requeridos (solo lectura) en EN_VISTO_BUENO.
- [ ] Botón "Dar Visto Bueno" avanza el informe a EN_REVISION.
- [ ] Botón "Escalar al Revisor" requiere observación y mueve a ENVIADO.
- [ ] Botón "Devolver al Contratista" requiere observación y mueve a DEVUELTO.
- [ ] Los tres botones desaparecen si el informe cambia de estado.

---

## Scripts de BD

Ninguno. I14 es un incremento de corrección de lógica de negocio y presentación; no requiere cambios de esquema.

---

## Stack de referencia

Java 8 · Spring Boot 2.7.18 · Spring Security 5 · Oracle 19c · Angular 20 · PrimeNG 20 · TypeScript
