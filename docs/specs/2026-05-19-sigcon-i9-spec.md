# Spec Técnica — SIGCON I9
## Visto Bueno Administrativo en el Flujo de Informes

> **Metodología:** Spec-Driven Development (SDD)
> **Versión:** 1.0 — **Fecha:** 2026-05-19
> **Estado:** APROBADO — pendiente plan de implementación
> **Rama:** `main`

---

## 1. Contexto

El flujo actual de aprobación de informes tiene tres actores: **Contratista**, **Revisor** (opcional por contrato) y **Supervisor**. La SED requiere incorporar un paso intermedio de **Visto Bueno Administrativo** entre el Revisor y el Supervisor, dado que el rol de Supervisión contractual corresponde a la Jefa de la oficina y el equipo administrativo actúa como filtro previo antes de escalar a su nivel.

Este paso debe ser **desactivable globalmente** sin requerir redespliegue del sistema.

---

## 2. Alcance

### Cambios de código

| ID | Área | Cambio |
|----|------|--------|
| VB-01 | Backend — Enums | Agregar `EN_VISTO_BUENO` a `EstadoInforme` y `ADMINISTRATIVO` a `Rol` |
| VB-02 | Backend — Servicio | Nuevo `ParametroService` (leer/escribir `SGCN_PARAMETROS` + migración de estados) |
| VB-03 | Backend — Servicio | Extender `InformeService`: `darVistosBueno()`, `escalar()`, bifurcación por `VB_ACTIVO` |
| VB-04 | Backend — API | Nuevos endpoints: cola VB, dar-visto-bueno, escalar; extender devolver; gestión parámetros |
| VB-05 | Backend — Seguridad | Rol `ADMINISTRATIVO` en `SecurityConfig` / `DevSecurityConfig`; nuevo guard de endpoint |
| VB-06 | Backend — Notificaciones | Nuevos eventos de notificación para el equipo administrativo y el supervisor |
| VB-07 | Frontend — Modelos | Agregar `EN_VISTO_BUENO` y `ADMINISTRATIVO` a los enums TypeScript |
| VB-08 | Frontend — Vistas | Feature `visto-bueno`: cola + detalle con 3 acciones |
| VB-09 | Frontend — Admin | Sección "Parámetros del sistema" con toggle VB + modal de confirmación |
| VB-10 | Frontend — Componentes | Extender badge de estado con chip `EN_VISTO_BUENO` |
| VB-11 | BD | `SGCN_PARAMETROS` (nueva tabla) + columna `ACCION` en `SGCN_OBSERVACIONES` |

### Decisiones documentadas (sin cambio de código)

| ID | Decisión |
|----|----------|
| D-01 | Pool compartido: cualquier usuario `ADMINISTRATIVO` puede atender el VB — sin asignación individual por contrato |
| D-02 | Switch global en BD (`SGCN_PARAMETROS`) en lugar de `application.yml` — operable en runtime sin redespliegue |
| D-03 | Al desactivar VB: migración automática `EN_VISTO_BUENO → EN_REVISION` en la misma transacción |
| D-04 | Escalación produce el mismo estado destino que Dar VB (`EN_REVISION`) pero con `ACCION = ESCALACION` en la observación para trazabilidad |

---

## 3. Máquina de Estados

### 3.1 Con VB activo

```
BORRADOR
   │ Contratista: Enviar
ENVIADO
   │ Revisor: Aprobar  (si no hay Revisor: transición directa)
EN_VISTO_BUENO          ← cola compartida rol ADMINISTRATIVO
   │              │                    │
   │ Dar VB       │ Devolver            │ Escalar
EN_REVISION    DEVUELTO            EN_REVISION
(Supervisor)  (Contratista        (Supervisor — bypass
   │           corrige y           con observación)
   │           reenvía → ENVIADO)
   ├── Supervisor: Devolver → DEVUELTO
APROBADO  (genera PDF — estado terminal)
```

### 3.2 Con VB inactivo

Flujo idéntico al actual. El estado `EN_VISTO_BUENO` nunca se activa.

### 3.3 Tabla de transiciones

| Desde | Acción | Actor | Hacia | Observación |
|-------|--------|-------|-------|-------------|
| `BORRADOR` | Enviar | Contratista | `ENVIADO` | — |
| `ENVIADO` | Aprobar revisión | Revisor | `EN_VISTO_BUENO` / `EN_REVISION`* | — |
| `ENVIADO` | Devolver | Revisor | `DEVUELTO` | Obligatoria |
| `ENVIADO` | — sin Revisor — | Sistema | `EN_VISTO_BUENO` / `EN_REVISION`* | — |
| `EN_VISTO_BUENO` | Dar Visto Bueno | Administrativo | `EN_REVISION` | Opcional |
| `EN_VISTO_BUENO` | Devolver | Administrativo | `DEVUELTO` | Obligatoria |
| `EN_VISTO_BUENO` | Escalar | Administrativo | `EN_REVISION` | Recomendada |
| `EN_REVISION` | Aprobar | Supervisor | `APROBADO` | — |
| `EN_REVISION` | Devolver | Supervisor | `DEVUELTO` | Obligatoria |
| `DEVUELTO` | Corregir y reenviar | Contratista | `ENVIADO` | — |

*Destino depende de `VB_ACTIVO` en `SGCN_PARAMETROS`.

---

## 4. Modelo de Datos

### 4.1 Scripts SQL

**`db/06_sgcn_parametros.sql`**
```sql
CREATE TABLE SGCN_PARAMETROS (
  CLAVE        VARCHAR2(50)  NOT NULL,
  VALOR        VARCHAR2(200) NOT NULL,
  DESCRIPCION  VARCHAR2(500),
  CONSTRAINT PK_SGCN_PARAMETROS PRIMARY KEY (CLAVE)
);

INSERT INTO SGCN_PARAMETROS (CLAVE, VALOR, DESCRIPCION)
VALUES ('VB_ACTIVO', 'S',
        'Visto Bueno Administrativo activo en el flujo de informes');
COMMIT;
```

**`db/07_observaciones_accion.sql`**
```sql
ALTER TABLE SGCN_OBSERVACIONES
  ADD ACCION VARCHAR2(20) DEFAULT NULL;
-- Valores: VISTO_BUENO | DEVOLUCION | ESCALACION
-- NULL para observaciones de Revisor y Supervisor (retrocompatible)
```

### 4.2 Migración de estados en vuelo

Ejecutada por `ParametroService.setVbActivo(false)` en transacción `@Transactional`:

```sql
UPDATE SGCN_INFORMES
SET    ESTADO = 'EN_REVISION', UPDATED_AT = SYSDATE
WHERE  ESTADO = 'EN_VISTO_BUENO';
```

---

## 5. Backend

### 5.1 Nuevos servicios

**`ParametroService`**
- `boolean isVbActivo()` — lee `SGCN_PARAMETROS` donde `CLAVE = 'VB_ACTIVO'`
- `void setVbActivo(boolean activo)` — persiste + ejecuta migración de estados si `activo = false`

### 5.2 Modificaciones a `InformeService`

| Método | Cambio |
|--------|--------|
| `enviar()` | Consulta `isVbActivo()` para determinar estado destino |
| `aprobarRevision()` | Ídem — destino `EN_VISTO_BUENO` o `EN_REVISION` según flag |
| `darVistosBueno(id, obs)` | Nuevo — `EN_VISTO_BUENO → EN_REVISION`; `accion = VISTO_BUENO` |
| `escalar(id, obs)` | Nuevo — `EN_VISTO_BUENO → EN_REVISION`; `accion = ESCALACION` |
| `devolver()` | Extender — aceptar actor `ADMINISTRATIVO` desde `EN_VISTO_BUENO`; `accion = DEVOLUCION` |

### 5.3 Endpoints

| Método | Ruta | Rol | Estado requerido |
|--------|------|-----|-----------------|
| `GET` | `/api/informes/cola/visto-bueno` | `ADMINISTRATIVO` | — |
| `POST` | `/api/informes/{id}/dar-visto-bueno` | `ADMINISTRATIVO` | `EN_VISTO_BUENO` |
| `POST` | `/api/informes/{id}/escalar` | `ADMINISTRATIVO` | `EN_VISTO_BUENO` |
| `POST` | `/api/informes/{id}/devolver` | extender a `ADMINISTRATIVO` | `EN_VISTO_BUENO` |
| `GET` | `/api/admin/parametros` | `ADMIN` | — |
| `PUT` | `/api/admin/parametros/vb-activo` | `ADMIN` | — |

### 5.4 Notificaciones

| Evento | Destinatario | Canal |
|--------|-------------|-------|
| Informe entra a `EN_VISTO_BUENO` | Todos los usuarios `ADMINISTRATIVO` | Email + in-app |
| Admin da VB o escala | Supervisor del contrato | Email + in-app |
| Admin devuelve | Contratista | Email + in-app |
| Switch VB desactivado (migración) | Supervisores de informes migrados | In-app |

---

## 6. Frontend Angular

### 6.1 Modelos

```typescript
// core/models/informe.model.ts
EN_VISTO_BUENO = 'EN_VISTO_BUENO'  // nuevo en enum EstadoInforme

// core/models/usuario.model.ts
ADMINISTRATIVO = 'ADMINISTRATIVO'  // nuevo en enum Rol
```

### 6.2 Feature `visto-bueno`

```
features/visto-bueno/
├── cola-visto-bueno.component.ts/.html   ← tabla PrimeNG (patrón cola Revisor)
└── detalle-visto-bueno.component.ts/.html ← readonly + barra de acciones
```

Barra de acciones del detalle:

| Botón | Color (DESIGN.md) | Observación |
|-------|-------------------|-------------|
| Dar Visto Bueno | Primary `#0B3D91` | Opcional |
| Escalar a Supervisor | Secondary `#FFB300` | Recomendada |
| Devolver al Contratista | Danger `#92032E` | Obligatoria |

### 6.3 Navegación

- Guard `administrativoGuard` — patrón de `revisorGuard` y `supervisorGuard`
- Ítem de menú: **"Visto Bueno"** — visible solo para rol `ADMINISTRATIVO`
- Rutas: `/visto-bueno` (cola) · `/visto-bueno/:id` (detalle)

### 6.4 Panel de Admin — toggle VB

Nueva sección **"Parámetros del sistema"** en la vista de administración existente:

- `p-inputSwitch` de PrimeNG con label "Visto Bueno Administrativo"
- Al desactivar: modal `p-dialog` de confirmación con advertencia:
  > *"Los informes en espera de Visto Bueno serán enviados automáticamente al Supervisor."*
- Solo ejecuta `PUT /api/admin/parametros/vb-activo` tras confirmación

### 6.5 Chip de estado

Seguiendo `Chips & Status Indicators` del `Prototipo/DESIGN.md`:

| Estado | Fondo | Texto | Etiqueta |
|--------|-------|-------|----------|
| `EN_VISTO_BUENO` | `#FFB300` (Institutional Gold) | `#281900` | En Visto Bueno |

El dorado es el token semántico correcto: requiere atención sin urgencia de rojo (`DEVUELTO`) ni neutralidad de azul (`EN_REVISION`).

---

## 7. Criterios de Aceptación

| # | Criterio | Verificación |
|---|----------|-------------|
| AC-1 | Con VB activo, el informe pasa a `EN_VISTO_BUENO` tras aprobación del Revisor (o sin Revisor) | Tests servicio |
| AC-2 | Con VB inactivo, el flujo es idéntico al actual (`EN_REVISION` directo) | Tests servicio |
| AC-3 | Dar VB mueve informe de `EN_VISTO_BUENO` a `EN_REVISION` | Tests servicio |
| AC-4 | Devolver desde VB mueve a `DEVUELTO` con observación obligatoria | Tests servicio |
| AC-5 | Escalar mueve a `EN_REVISION` con `accion = ESCALACION` registrado | Tests servicio |
| AC-6 | Desactivar VB migra automáticamente todos los `EN_VISTO_BUENO → EN_REVISION` | Tests servicio + integración |
| AC-7 | Un actor sin rol `ADMINISTRATIVO` no puede acceder a endpoints VB | Tests seguridad |
| AC-8 | Notificación llega a todos los `ADMINISTRATIVO` al entrar un informe a `EN_VISTO_BUENO` | Tests notificación |
| AC-9 | Cola `/cola/visto-bueno` muestra solo informes `EN_VISTO_BUENO` | Tests controlador |
| AC-10 | Chip `EN_VISTO_BUENO` visible con color dorado institucional en todas las vistas de estado | Build Angular |
| AC-11 | Toggle VB en panel admin muestra modal de confirmación antes de ejecutar la acción | Smoke test UI |
| AC-12 | `mvn test` — 0 fallos | CI |
| AC-13 | `ng build` — 0 errores TypeScript | CI |

---

## 8. Archivos Afectados

### Backend

| Archivo | Cambio |
|---------|--------|
| `db/06_sgcn_parametros.sql` | Nuevo — tabla + INSERT inicial |
| `db/07_observaciones_accion.sql` | Nuevo — ALTER TABLE |
| `domain/entity/enums/EstadoInforme.java` | + `EN_VISTO_BUENO` |
| `domain/entity/enums/Rol.java` | + `ADMINISTRATIVO` |
| `application/service/ParametroService.java` | Nuevo |
| `application/service/InformeService.java` | Modificar: bifurcación VB, nuevos métodos |
| `web/controller/InformeController.java` | + endpoints VB |
| `web/controller/ParametroController.java` | Nuevo |
| `config/SecurityConfig.java` | + rol ADMINISTRATIVO |
| `config/DevSecurityConfig.java` | + usuario `admin-vb` de prueba |
| `application/service/NotificacionService.java` | + eventos VB |

### Frontend

| Archivo | Cambio |
|---------|--------|
| `core/models/informe.model.ts` | + `EN_VISTO_BUENO` |
| `core/models/usuario.model.ts` | + `ADMINISTRATIVO` |
| `features/visto-bueno/` | Nuevo feature module (4 archivos) |
| `core/guards/administrativo.guard.ts` | Nuevo |
| `app.routes.ts` | + rutas `/visto-bueno` |
| `layout/sidebar/sidebar.component.ts` | + ítem menú ADMINISTRATIVO |
| `features/admin/parametros/` | Nueva sección toggle VB |
| `shared/components/estado-badge/` | + chip `EN_VISTO_BUENO` |

---

*Spec generada mediante SDD — SIGCON — Incremento 9 — 2026-05-19.*
