# Design Doc — SIGCON I9
## Visto Bueno Administrativo en el Flujo de Informes

> **Fecha:** 2026-05-19
> **Metodología:** SDD Spec-Anchored
> **Estado:** APROBADO (pendiente spec técnica formal)
> **Autor:** jescandonp
> **Design system:** `Prototipo/DESIGN.md` — SED Bogotá (Corporate / Modern)

---

## 1. Contexto y Motivación

El flujo actual de aprobación de informes en SIGCON tiene tres actores: **Contratista**, **Revisor** (opcional por contrato) y **Supervisor**. La Secretaría de Educación del Distrito requiere incorporar un paso intermedio de **Visto Bueno Administrativo** entre el Revisor y el Supervisor, dado que el rol de Supervisión contractual corresponde a la Jefa de la oficina y el equipo administrativo actúa como filtro previo antes de escalar a su nivel.

Este paso debe ser **desactivable globalmente** para soportar escenarios futuros donde ya no sea obligatorio, sin requerir un redespliegue del sistema.

---

## 2. Decisiones de Diseño

| # | Decisión | Alternativa descartada | Razón |
|---|----------|----------------------|-------|
| D1 | Pool compartido (cualquier ADMINISTRATIVO atiende VB) | Asignación individual por contrato | Mayor flexibilidad operativa para el equipo admin |
| D2 | 3 acciones: Dar VB / Devolver / Escalar | Solo aprobar | Refleja la realidad operativa: el equipo puede escalar sin dar VB formal |
| D3 | Switch global en `SGCN_PARAMETROS` (BD) | Property en `application.yml` | Operable en runtime sin redespliegue del WAR |
| D4 | Migración automática `EN_VISTO_BUENO → EN_REVISION` al desactivar | Manual / solo aplica a nuevos | Evita informes bloqueados en un estado huérfano |
| D5 | Nuevo estado `EN_VISTO_BUENO` en el enum | Campo `etapaActual` en el informe | Estado es el árbitro del flujo en SIGCON — patrón establecido |

---

## 3. Máquina de Estados

### 3.1 Flujo completo con VB activo

```
BORRADOR
   │ Contratista: Enviar
   ▼
ENVIADO
   │ Revisor: Aprobar (si hay Revisor asignado)
   │ — si no hay Revisor: transición directa —
   ▼
EN_VISTO_BUENO  ←── cola del equipo Administrativo
   │                    │                      │
   │ Dar VB             │ Devolver              │ Escalar
   ▼                    ▼                      ▼
EN_REVISION         DEVUELTO              EN_REVISION
(Supervisor)    (Contratista           (Supervisor, con
   │             corrige y             observación de
   │             reenvía → ENVIADO)    escalación)
   │
   ├── Supervisor: Devolver → DEVUELTO
   ▼
APROBADO  (genera PDF — estado terminal)
```

### 3.2 Flujo con VB desactivado (igual que hoy)

```
BORRADOR → ENVIADO → [Revisor opcional → EN_REVISION] → EN_REVISION → APROBADO
```

Al desactivar el switch: todos los informes en `EN_VISTO_BUENO` se migran automáticamente a `EN_REVISION` en la misma transacción.

### 3.3 Tabla completa de transiciones

| Desde | Acción | Actor | Hacia | Observación |
|-------|--------|-------|-------|-------------|
| `BORRADOR` | Enviar | Contratista | `ENVIADO` | — |
| `ENVIADO` | Aprobar revisión | Revisor | `EN_VISTO_BUENO`* / `EN_REVISION` | *Si VB activo |
| `ENVIADO` | Devolver | Revisor | `DEVUELTO` | Obligatoria |
| `ENVIADO` | — sin Revisor — | Sistema | `EN_VISTO_BUENO`* / `EN_REVISION` | *Si VB activo |
| `EN_VISTO_BUENO` | Dar Visto Bueno | Administrativo | `EN_REVISION` | Opcional |
| `EN_VISTO_BUENO` | Devolver | Administrativo | `DEVUELTO` | Obligatoria |
| `EN_VISTO_BUENO` | Escalar | Administrativo | `EN_REVISION` | Recomendada |
| `EN_REVISION` | Aprobar | Supervisor | `APROBADO` | — |
| `EN_REVISION` | Devolver | Supervisor | `DEVUELTO` | Obligatoria |
| `DEVUELTO` | Corregir y reenviar | Contratista | `ENVIADO` | — |

---

## 4. Modelo de Datos

### 4.1 Enums Java

```java
// EstadoInforme.java
EN_VISTO_BUENO   // nuevo

// Rol.java
ADMINISTRATIVO   // nuevo
```

### 4.2 Nueva tabla `SGCN_PARAMETROS`

```sql
-- db/06_sgcn_parametros.sql
CREATE TABLE SGCN_PARAMETROS (
  CLAVE        VARCHAR2(50)  NOT NULL,
  VALOR        VARCHAR2(200) NOT NULL,
  DESCRIPCION  VARCHAR2(500),
  CONSTRAINT PK_SGCN_PARAMETROS PRIMARY KEY (CLAVE)
);

INSERT INTO SGCN_PARAMETROS (CLAVE, VALOR, DESCRIPCION)
VALUES ('VB_ACTIVO', 'S', 'Visto Bueno Administrativo activo en el flujo de informes');
COMMIT;
```

### 4.3 Extensión de `SGCN_OBSERVACIONES`

```sql
-- db/07_observaciones_accion.sql
ALTER TABLE SGCN_OBSERVACIONES
  ADD ACCION VARCHAR2(20) DEFAULT NULL;
-- Valores: VISTO_BUENO | DEVOLUCION | ESCALACION | NULL (Revisor/Supervisor — retrocompatible)
```

### 4.4 Migración de estados en vuelo (al desactivar VB)

```sql
UPDATE SGCN_INFORMES
SET    ESTADO = 'EN_REVISION',
       UPDATED_AT = SYSDATE
WHERE  ESTADO = 'EN_VISTO_BUENO';
```

Ejecutado por `ParametroService.setVbActivo(false)` en la misma transacción @Transactional.

---

## 5. Backend

### 5.1 Servicios

**`ParametroService`** — nuevo:
- `boolean isVbActivo()` — lee `SGCN_PARAMETROS` (`CLAVE = 'VB_ACTIVO'`)
- `void setVbActivo(boolean activo)` — persiste + migra estados en vuelo si `activo = false`

**`InformeService`** — modificaciones:
- `enviar()` — consulta `isVbActivo()` para determinar estado destino tras transición del Revisor (o sin Revisor)
- `aprobarRevision()` — ídem
- `darVistosBueno(Long informeId, String observacion)` — nuevo; `EN_VISTO_BUENO → EN_REVISION`; `accion = VISTO_BUENO`
- `escalar(Long informeId, String observacion)` — nuevo; `EN_VISTO_BUENO → EN_REVISION`; `accion = ESCALACION`
- `devolver()` — extender autorización para `ADMINISTRATIVO` desde `EN_VISTO_BUENO`; `accion = DEVOLUCION`

### 5.2 Endpoints

| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| `GET` | `/api/informes/cola/visto-bueno` | ADMINISTRATIVO | Cola de informes en `EN_VISTO_BUENO` |
| `POST` | `/api/informes/{id}/dar-visto-bueno` | ADMINISTRATIVO | Da VB → `EN_REVISION` |
| `POST` | `/api/informes/{id}/escalar` | ADMINISTRATIVO | Escala → `EN_REVISION` con obs. |
| `POST` | `/api/informes/{id}/devolver` | ADMINISTRATIVO *(extender)* | Devuelve → `DEVUELTO` desde `EN_VISTO_BUENO` |
| `GET` | `/api/admin/parametros` | ADMIN | Lista parámetros del sistema |
| `PUT` | `/api/admin/parametros/vb-activo` | ADMIN | Activa/desactiva VB + migración |

### 5.3 Seguridad

- `ADMINISTRATIVO` puede acceder únicamente a informes en estado `EN_VISTO_BUENO`
- El endpoint `devolver` valida que si el actor es `ADMINISTRATIVO`, el estado debe ser `EN_VISTO_BUENO`
- `SGCN_PARAMETROS` solo es modificable por `ADMIN`
- Aislamiento de datos: un ADMINISTRATIVO ve todos los informes en VB (cola compartida, no filtrada por contrato)

### 5.4 Notificaciones

| Evento | Destinatario | Canal |
|--------|-------------|-------|
| Informe entra a `EN_VISTO_BUENO` | Todos los usuarios `ADMINISTRATIVO` | Email + in-app |
| Admin da VB o escala | Supervisor del contrato | Email + in-app |
| Admin devuelve | Contratista | Email + in-app |
| Switch VB desactivado (migración) | Supervisores de informes migrados | In-app |

---

## 6. Frontend

### 6.1 Modelos TypeScript

```typescript
// core/models/informe.model.ts
export enum EstadoInforme {
  // ... existentes ...
  EN_VISTO_BUENO = 'EN_VISTO_BUENO'
}

// core/models/usuario.model.ts
export enum Rol {
  // ... existentes ...
  ADMINISTRATIVO = 'ADMINISTRATIVO'
}
```

### 6.2 Nuevas vistas

```
features/visto-bueno/
├── cola-visto-bueno.component.ts
├── cola-visto-bueno.component.html
├── detalle-visto-bueno.component.ts
└── detalle-visto-bueno.component.html
```

**Cola:** tabla PrimeNG con columnas contratista, número de contrato, período, fecha de llegada a VB. Patrón idéntico a las colas de Revisor y Supervisor existentes.

**Detalle:** vista de solo lectura del informe completo con barra de acciones inferior:

| Botón | Estilo DESIGN.md | Comportamiento |
|-------|-----------------|----------------|
| Dar Visto Bueno | Primary — `#0B3D91` fondo blanco | Sin campo de texto requerido |
| Escalar a Supervisor | Secondary — `#FFB300` borde dorado | Campo de texto recomendado (no obligatorio) |
| Devolver al Contratista | Danger — `#92032E` / error | Campo de texto obligatorio |

### 6.3 Navegación y guards

- `administrativoGuard` — igual que `revisorGuard` y `supervisorGuard`
- Menú lateral: ítem **"Visto Bueno"** visible solo si rol `ADMINISTRATIVO`
- Rutas: `/visto-bueno` (cola) y `/visto-bueno/:id` (detalle)

### 6.4 Panel de Admin — switch VB

Sección nueva **"Parámetros del sistema"** en la vista de administración:

- `p-inputSwitch` de PrimeNG con label "Visto Bueno Administrativo"
- Al desactivar: modal de confirmación con advertencia explícita:
  > *"Los informes actualmente en espera de Visto Bueno serán enviados automáticamente al Supervisor."*
- Botones del modal: `[Cancelar]` (Secondary) y `[Confirmar]` (Primary `#0B3D91`)
- Solo llama a `PUT /api/admin/parametros/vb-activo` tras confirmación

### 6.5 Chip de estado `EN_VISTO_BUENO`

Siguiendo la convención de `Chips & Status Indicators` del DESIGN.md:

| Estado | Color fondo | Color texto | Etiqueta |
|--------|------------|-------------|----------|
| `EN_VISTO_BUENO` | `#FFB300` (Institutional Gold) | `#281900` (on-secondary-fixed) | En Visto Bueno |

El dorado institucional es el color semántico correcto: indica un ítem que **requiere atención** sin la urgencia del rojo (`DEVUELTO`) ni la neutralidad del azul (`EN_REVISION`).

---

## 7. Retrocompatibilidad

- Informes existentes en cualquier estado distinto a `EN_VISTO_BUENO` no son afectados
- Columna `ACCION` en `SGCN_OBSERVACIONES` tiene `DEFAULT NULL` — registros existentes permanecen válidos
- Si VB está inactivo, el flujo es exactamente igual al actual — sin cambio de comportamiento para contratos en curso
- La tabla `SGCN_PARAMETROS` arranca con `VB_ACTIVO = 'S'` — el paso se activa desde el primer despliegue de I9

---

## 8. Scripts SQL del Incremento

| Archivo | Propósito |
|---------|-----------|
| `db/06_sgcn_parametros.sql` | Crear tabla + INSERT inicial `VB_ACTIVO = S` |
| `db/07_observaciones_accion.sql` | `ALTER TABLE SGCN_OBSERVACIONES ADD ACCION` |

---

*Design doc generado mediante SDD Spec-Anchored — SIGCON I9 — 2026-05-19*
