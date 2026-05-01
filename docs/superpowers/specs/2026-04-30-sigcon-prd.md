# PRD — SIGCON: Sistema de Gestión de Contratos · SED Bogotá

> **Metodología:** Spec-Driven Development (SDD) — Nivel Spec-Anchored  
> **Versión:** 1.0  
> **Fecha:** 2026-04-30  
> **Estado:** Aprobado — listo para descomposición en specs técnicas  
> **Stack de referencia:** Angular 20 + PrimeNG 21 · Spring Boot 2.7.x WAR (JDK 8) · Oracle 19c · Azure AD / MSAL · WebLogic 12.2.1.4

---

## Tabla de Contenido

1. [Visión General y Alcance](#1-visión-general-y-alcance)
2. [Perfiles de Usuario y Roles](#2-perfiles-de-usuario-y-roles)
3. [Módulos Funcionales por Incremento](#3-módulos-funcionales-por-incremento)
4. [Modelo de Datos Conceptual y Máquina de Estados](#4-modelo-de-datos-conceptual-y-máquina-de-estados)
5. [Requisitos No Funcionales y Criterios de Aceptación](#5-requisitos-no-funcionales-y-criterios-de-aceptación)

---

## 1. Visión General y Alcance

### 1.1 Nombre e Identidad del Sistema

**SIGCON — Sistema de Gestión de Contratos · Secretaría de Educación del Distrito · Bogotá, Colombia**

El nombre SIGCON es intencionalmente genérico para soportar el crecimiento del sistema más allá de los contratos OPS (Órdenes de Prestación de Servicios) hacia otros tipos contractuales en fases posteriores.

### 1.2 Visión

Plataforma web institucional que digitaliza y centraliza el ciclo de gestión de informes de contratos de prestación de servicios (OPS) en ejecución de la Secretaría de Educación del Distrito. Elimina el manejo manual en papel y correo electrónico, garantiza trazabilidad completa del proceso de aprobación y genera el documento formal de informe firmado en PDF.

**Referencia funcional:** La aplicación del Ministerio de Cultura de Colombia, cuyo flujo operativo sirvió como base conceptual para SIGCON.

### 1.3 Contexto Estratégico

Los contratos OPS de la SED Bogotá gestionan vinculaciones de personas naturales que prestan servicios a la entidad. Actualmente, el proceso de presentación y aprobación de informes de actividades se realiza de forma manual (papel, correo electrónico, archivos físicos), lo que genera:

- Falta de trazabilidad del estado de cada informe
- Riesgo de pérdida de soportes documentales
- Demoras en los ciclos de aprobación
- Dificultad de supervisión centralizada para los responsables

SIGCON resuelve estos problemas con un flujo digital completamente gestionado dentro del ecosistema tecnológico existente de la SED (Office 365, Oracle, WebLogic).

### 1.4 Alcance del MVP

**Incluido — Fase Inicial (persona natural — OPS):**

- Registro y consulta de contratos en ejecución por parte del equipo SED
- Presentación, corrección y seguimiento de informes de actividades por parte del contratista
- Flujo de verificación (Revisor) y aprobación (Supervisor) con devoluciones y observaciones
- Generación automática de PDF del informe con firmas digitales (imagen)
- Notificaciones por correo institucional (Office 365) y dentro de la aplicación

**Fuera de alcance del MVP:**

- Contratos de personas jurídicas / proveedores (Fase 2)
- Gestión precontractual: estudios previos, invitaciones, procesos de selección
- Integración directa con SECOP2 (los contratos se registran manualmente en SIGCON)
- Liquidación activa de contratos (los contratos liquidados o cerrados son solo consulta histórica de informes)
- Módulo de pagos u órdenes de pago

### 1.5 Proyección de Crecimiento

El diseño de SIGCON debe soportar, sin rediseño del núcleo, la incorporación en Fase 2 de:

- Contratos con personas jurídicas (proveedores)
- Diferentes tipos de informes según el tipo de contrato
- Posible integración con SECOP2 para importación automática de contratos

---

## 2. Perfiles de Usuario y Roles

Todos los usuarios se autentican vía **Azure AD / Office 365 (MSAL)** en producción. En entorno local de desarrollo se usa **HTTP Basic** con usuarios de prueba hardcodeados (perfil `local-dev`).

### 2.1 ROL: CONTRATISTA

**Descripción:** Persona natural vinculada a la SED mediante contrato OPS.  
**Acceso:** Exclusivamente sus propios contratos. No puede ver ni acceder a contratos de otros contratistas.

**Capacidades:**
- Ver listado de sus contratos activos e históricos
- Ver ficha completa de cada contrato (objeto, valor, vigencia, supervisor asignado)
- Crear y editar informes en estado `BORRADOR`
- Registrar actividades por obligación contractual (descripción + porcentaje de cumplimiento)
- Adjuntar soportes por obligación (archivo o URL externa)
- Adjuntar documentos adicionales según el catálogo parametrizable del contrato
- Enviar el informe al flujo de aprobación
- Ver observaciones y corregir informes en estado `DEVUELTO`
- Configurar imagen de firma digital en su perfil de usuario
- Descargar el PDF del informe en estado `APROBADO`

### 2.2 ROL: REVISOR

**Descripción:** Funcionario SED de apoyo a la supervisión contractual.  
**Acceso:** Informes de los contratos que tiene asignados para revisar.

**Capacidades:**
- Ver cola de informes en estado `ENVIADO` asignados a él
- Ver el detalle completo del informe con todos sus soportes adjuntos
- Aprobar la revisión → el informe avanza a `EN_REVISION` (pasa al Supervisor)
- Devolver con observaciones escritas → el informe pasa a `DEVUELTO` (vuelve al Contratista)

### 2.3 ROL: SUPERVISOR / APROBADOR

**Descripción:** Supervisor contractual designado. Responsable legal del contrato ante la SED.  
**Acceso:** Todos los contratos que supervisa y sus informes.

**Capacidades:**
- Ver cola de informes en estado `EN_REVISION` de sus contratos
- Ver el detalle completo del informe
- Aprobar el informe → dispara la generación del PDF → estado `APROBADO`
- Devolver con observaciones escritas → estado `DEVUELTO` (vuelve al Contratista)
- Configurar imagen de firma digital en su perfil de usuario
- Descargar el PDF del informe aprobado

### 2.4 ROL: ADMINISTRADOR SED

**Descripción:** Funcionario del área de sistemas o de contratación de la SED.  
**Acceso:** Panel de administración completo del sistema.

**Capacidades:**
- Registrar, editar y gestionar contratos (datos base, obligaciones contractuales, vigencia)
- Asignar Contratista, Revisor y Supervisor a cada contrato
- Gestionar el catálogo de documentos adicionales parametrizables por tipo de contrato
- Gestionar usuarios y asignación de roles
- Cambiar estado de contratos: `EN_EJECUCION` / `LIQUIDADO` / `CERRADO`
- Consultar el historial completo de informes de todos los contratos

### 2.5 Matriz de Permisos

| Acción | Contratista | Revisor | Supervisor | Admin |
|--------|:-----------:|:-------:|:----------:|:-----:|
| Ver mis contratos | ✓ | — | ✓ | ✓ |
| Ver todos los contratos | — | — | — | ✓ |
| Crear / editar informe (borrador) | ✓ | — | — | — |
| Enviar informe | ✓ | — | — | — |
| Revisar y aprobar / devolver (Revisor) | — | ✓ | — | — |
| Aprobar final / generar PDF | — | — | ✓ | — |
| Registrar / editar contratos | — | — | — | ✓ |
| Gestionar usuarios y roles | — | — | — | ✓ |
| Gestionar catálogo de documentos | — | — | — | ✓ |
| Descargar PDF aprobado | ✓ | — | ✓ | ✓ |
| Configurar imagen de firma | ✓ | — | ✓ | — |

---

## 3. Módulos Funcionales por Incremento

### INCREMENTO 1 — Fundación: Auth + Contratos + Vista Contratista

**Objetivo:** El sistema existe, los usuarios pueden autenticarse y el contratista puede ver sus contratos con toda su información base.

**Criterio de cierre:** Un admin puede registrar un contrato completo con obligaciones y asignaciones; un contratista puede autenticarse y ver sus contratos con toda la información. El ciclo de vida básico del contrato está operativo.

**Prerequisito de diseño:** El archivo `DESIGN.md` (prototipo UX/UI) debe estar generado y aprobado antes de escribir el primer componente Angular.

---

#### M1 — Autenticación y Perfiles de Usuario

- Login vía Azure AD (MSAL) en producción
- Login HTTP Basic en perfil `local-dev` (usuarios de prueba: `admin/admin123`, `contratista/contratista123`, `revisor/revisor123`, `supervisor/supervisor123`)
- Perfil de usuario: nombre completo, cargo, email corporativo
- Carga y actualización de imagen de firma digital (JPG/PNG) desde el perfil
- Gestión de sesión y cierre de sesión
- Redirección automática según rol al iniciar sesión

#### M2 — Administración de Contratos *(solo Admin SED)*

- CRUD de contratos con campos:
  - Número de contrato, objeto, tipo (`OPS`), valor total
  - Fecha de inicio, fecha de fin
  - Estado: `EN_EJECUCION` / `LIQUIDADO` / `CERRADO`
- Registro de obligaciones contractuales por contrato (lista ordenada, descripción de cada obligación)
- Asignación de Contratista, Revisor y Supervisor a cada contrato
- Gestión del catálogo de documentos adicionales parametrizables:
  - Nombre del documento, descripción, si es obligatorio
  - Asociación a tipo de contrato
- Listado paginado y buscable de todos los contratos

#### M3 — Portal del Contratista — Vista de Contratos

- Dashboard: listado de contratos activos del contratista con indicadores visuales
  - Informes pendientes por enviar
  - Último estado de informe
  - Alertas de contratos próximos a vencer
- Ficha de contrato: datos completos + supervisor asignado + historial de informes con fechas y estados
- Acceso al historial de informes aprobados con opción de descarga de PDF

---

### INCREMENTO 2 — Núcleo: Informes y Flujo de Aprobación

**Objetivo:** El flujo completo de informe funciona de extremo a extremo: creación → revisión → aprobación / devolución → corrección → aprobación.

**Criterio de cierre:** Un informe puede recorrer el ciclo completo (creación → devolución → corrección → aprobación) con todas las observaciones visibles y los estados correctamente gestionados. No existen transiciones de estado inválidas.

---

#### M4 — Gestión de Informes *(Contratista)*

- Crear nuevo informe asociado a un contrato activo:
  - Selección de periodo: fecha de inicio y fecha de fin
  - Estado inicial: `BORRADOR`
- Por cada obligación del contrato:
  - Registrar descripción de la actividad realizada en el periodo
  - Registrar porcentaje de cumplimiento (0–100%)
  - Adjuntar soportes: archivo (almacenamiento configurable) o URL externa
- Adjuntar documentos adicionales según catálogo parametrizable del contrato
- Guardar como `BORRADOR` (editable, no visible al flujo de aprobación)
- Previsualización del informe antes de enviar
- Enviar informe → transición a `ENVIADO` con confirmación explícita
- Editar y reenviar informes directamente desde estado `DEVUELTO` (las observaciones del Revisor / Supervisor son visibles en pantalla durante la corrección)
- Ver historial de versiones de devoluciones y observaciones por informe

#### M5 — Revisión de Informes *(Revisor)*

- Cola de trabajo: listado de informes en estado `ENVIADO` asignados al Revisor
- Vista detallada del informe: datos del periodo, actividades, porcentajes, soportes adjuntos
- Acción **Aprobar revisión** → informe pasa a `EN_REVISION` (notifica al Supervisor)
- Acción **Devolver con observaciones** → campo de texto obligatorio de observaciones → informe pasa a `DEVUELTO` (notifica al Contratista)

#### M6 — Aprobación Final *(Supervisor)*

- Cola de trabajo: listado de informes en estado `EN_REVISION` de los contratos supervisados
- Vista detallada del informe (idéntica a la del Revisor)
- Acción **Aprobar** → dispara generación de PDF → estado `APROBADO` (notifica al Contratista)
- Acción **Devolver con observaciones** → campo de texto obligatorio → estado `DEVUELTO` (notifica al Contratista)

---

### INCREMENTO 3 — Completitud: PDF + Firmas + Notificaciones

**Objetivo:** El sistema está listo para producción: genera documentos formales firmados y mantiene a todos los actores informados en cada transición de estado.

**Criterio de cierre:** Al aprobar un informe se genera el PDF con firmas incrustadas, es descargable, y todos los actores reciben notificación por ambos canales (email + in-app) en cada transición de estado relevante.

---

#### M7 — Generación de PDF

- Template institucional del informe basado en el formato de referencia de la SED (`06_Informe_actividades_06_Abril_2026_Juan_Escandon.docx`)
- Contenido del PDF:
  - Encabezado institucional SED
  - Datos del contrato (número, objeto, contratista, supervisor, vigencia)
  - Periodo del informe (fecha inicio / fecha fin)
  - Tabla de obligaciones con actividades realizadas y porcentaje de cumplimiento
  - Listado de soportes adjuntos referenciados
  - Listado de documentos adicionales adjuntos
  - Sección de firmas: imagen de firma del Contratista + imagen de firma del Supervisor
  - Nombre, cargo y fecha de cada firmante
  - Metadata: número de informe, fecha de generación, estado `APROBADO`
- Generación automática al momento de aprobación final del Supervisor
- PDF inmutable: una vez generado no puede modificarse

#### M8 — Notificaciones

**Notificaciones in-app:**
- Campana en la barra superior con badge de contador de notificaciones no leídas
- Centro de notificaciones: listado con título, descripción, fecha y estado leído/no leído
- Marcado como leída al hacer clic

**Notificaciones por email (Office 365):**

| Evento | Destinatario |
|--------|-------------|
| Contratista envía informe | Revisor asignado |
| Revisor aprueba revisión | Supervisor asignado |
| Revisor devuelve con observaciones | Contratista |
| Supervisor aprueba (PDF generado) | Contratista |
| Supervisor devuelve con observaciones | Contratista |

Los emails incluyen: nombre del contratista, número de contrato, periodo del informe y (cuando aplica) el texto de las observaciones.

---

## 4. Modelo de Datos Conceptual y Máquina de Estados

### 4.1 Entidades Principales

```
CONTRATO
├── número (único), objeto, tipo (OPS)
├── valor total, fecha inicio, fecha fin
├── estado: EN_EJECUCION | LIQUIDADO | CERRADO
├── → Contratista (USUARIO)
├── → Revisor (USUARIO)
├── → Supervisor (USUARIO)
├── → [OBLIGACION]
└── → [DOCUMENTO_CATALOGO] (documentos adicionales aplicables)

OBLIGACION
├── descripción, orden (entero para secuencia)
└── pertenece a → CONTRATO

INFORME
├── número (secuencial por contrato), periodo (fecha inicio / fecha fin)
├── estado: BORRADOR | ENVIADO | EN_REVISION | DEVUELTO | APROBADO
├── fecha creación, fecha último envío, fecha aprobación
├── → CONTRATO
├── → [ACTIVIDAD_INFORME]
├── → [DOCUMENTO_ADICIONAL]
└── → [OBSERVACION]

ACTIVIDAD_INFORME
├── descripción de la actividad realizada
├── porcentaje de cumplimiento (0–100)
├── → OBLIGACION
└── → [SOPORTE_ADJUNTO]

SOPORTE_ADJUNTO
├── tipo: ARCHIVO | URL
├── nombre descriptivo
├── referencia: ruta al archivo (almacenamiento configurable) o URL externa
└── pertenece a → ACTIVIDAD_INFORME

DOCUMENTO_ADICIONAL
├── → DOCUMENTO_CATALOGO (tipo de documento)
├── referencia al archivo adjunto
└── pertenece a → INFORME

DOCUMENTO_CATALOGO
├── nombre, descripción
├── obligatorio: S | N
└── aplica a → tipo de contrato

OBSERVACION
├── texto (libre)
├── fecha y hora
├── autor rol: REVISOR | SUPERVISOR
└── pertenece a → INFORME

USUARIO
├── email (UPN Office 365 — identificador único)
├── nombre completo, cargo
├── rol: CONTRATISTA | REVISOR | SUPERVISOR | ADMIN
├── activo: S | N
└── firma_imagen: ruta al archivo de imagen (JPG/PNG)

NOTIFICACION
├── título, descripción
├── leída: S | N
├── fecha y hora
├── → USUARIO (destinatario)
└── evento origen (tipo de transición de estado)
```

### 4.2 Prefijo de Tablas Oracle

Siguiendo las convenciones del ecosistema SED, el esquema usa el prefijo **`SGCN_`**:

| Entidad | Tabla Oracle |
|---------|-------------|
| Contrato | `SGCN_CONTRATOS` |
| Obligación | `SGCN_OBLIGACIONES` |
| Informe | `SGCN_INFORMES` |
| Actividad del informe | `SGCN_ACTIVIDADES` |
| Soporte adjunto | `SGCN_SOPORTES` |
| Documento del catálogo | `SGCN_DOCS_CATALOGO` |
| Documento adicional | `SGCN_DOCS_ADICIONALES` |
| Observación | `SGCN_OBSERVACIONES` |
| Usuario | `SGCN_USUARIOS` |
| Notificación | `SGCN_NOTIFICACIONES` |

Esquema Oracle: **`SED_SIGCON`**

Todos los scripts DDL irán en `db/00_setup.sql`. Datos de prueba en `db/01_datos_prueba.sql`.

### 4.3 Máquina de Estados del Informe

```
                    ┌─────────────────┐
                    │    BORRADOR     │
                    │  (editable)     │
                    └────────┬────────┘
                             │ Contratista: Enviar
                             │
                    ┌────────▼────────┐
                    │    ENVIADO      │
                    └────────┬────────┘
                             │
            ┌────────────────┴──────────────────┐
            │ Revisor: Aprobar                   │ Revisor: Devolver
            │                                    │ (con observaciones)
   ┌────────▼────────┐                  ┌────────▼────────┐
   │  EN_REVISION    │                  │    DEVUELTO     │
   └────────┬────────┘                  │  (editable +    │
            │                           │  observaciones  │
  ┌─────────┴──────────┐  Supervisor:   │  visibles)      │
  │ Supervisor: Aprobar│  Devolver ────►│                 │
  │                    │  (con obs.)    └────────┬────────┘
┌─▼────────────────┐                            │ Contratista:
│    APROBADO      │ → genera PDF               │ Corregir y Reenviar
│  (estado final)  │   automáticamente          │
└──────────────────┘               ┌────────────┘
                                   │
                          ┌────────▼────────┐
                          │    ENVIADO      │ (nuevo ciclo)
                          └─────────────────┘
```

**Reglas de transición:**

| Transición | Actor permitido | Condición |
|-----------|----------------|-----------|
| `BORRADOR → ENVIADO` | Contratista | Informe con al menos una actividad registrada |
| `ENVIADO → EN_REVISION` | Revisor asignado | — |
| `ENVIADO → DEVUELTO` | Revisor asignado | Observación obligatoria |
| `EN_REVISION → APROBADO` | Supervisor asignado | Genera PDF automáticamente |
| `EN_REVISION → DEVUELTO` | Supervisor asignado | Observación obligatoria |
| `DEVUELTO → ENVIADO` | Contratista | Corrige desde estado DEVUELTO y reenvía |

`APROBADO` es estado terminal — el informe no puede modificarse ni devolverse una vez aprobado.

---

## 5. Requisitos No Funcionales y Criterios de Aceptación

### 5.1 Seguridad

- Autenticación obligatoria en todas las rutas de la aplicación
- Autorización por rol: cada endpoint del backend valida el rol del usuario antes de ejecutar la operación
- Aislamiento de datos: un Contratista nunca puede acceder a contratos o informes de otro Contratista
- Los archivos adjuntos solo son accesibles por usuarios con acceso al contrato correspondiente
- Auditoría completa: toda acción queda registrada con usuario (`CREATED_BY` = email UPN), fecha y hora (`CREATED_AT`, `UPDATED_AT`)
- Tokens JWT de Azure AD validados en cada request al backend (perfil `weblogic`)

### 5.2 Rendimiento

- Listado de contratos del Contratista: respuesta < 2s para hasta 500 contratos
- Generación y descarga de PDF: completada en < 10s
- Carga de archivos de soporte: hasta 10MB por archivo
- Paginación obligatoria en todos los listados (mínimo 10/25/50 filas por página)

### 5.3 Compatibilidad

- Navegadores soportados: Chrome 120+, Edge 120+, Firefox 120+
- No se requiere soporte para Internet Explorer
- Resolución mínima: 1280×768 (aplicación de escritorio institucional)
- No se requiere versión móvil en el MVP

### 5.4 Disponibilidad e Infraestructura

- Despliegue sobre Oracle WebLogic 12.2.1.4.0 — infraestructura distrital SED
- Runtime: Oracle JDK 8 (restricción no negociable del servidor distrital)
- Base de datos: Oracle 19c+ — esquema `SED_SIGCON`
- La aplicación debe funcionar correctamente con perfil Spring `weblogic` (Oracle 19c + JWT Azure AD)
- El perfil `local-dev` (HTTP Basic + Oracle local) permite desarrollo sin dependencia de infraestructura distrital

### 5.5 Mantenibilidad

- Swagger UI siempre activo en todos los entornos (no solo desarrollo)
- Todos los endpoints documentados con `@Tag` y `@Operation` (SpringDoc OpenAPI 1.7.0)
- Código estructurado según arquitectura SED: `domain/`, `application/`, `web/`, `config/`
- WAR final: `sigcon-backend.war` — contexto WebLogic: `/sigcon`
- Scripts SQL versionados: `db/00_setup.sql` (DDL) y `db/01_datos_prueba.sql` (DML)

### 5.6 Diseño UX/UI — Design System

- Antes de iniciar el Incremento 1, se generará un archivo `DESIGN.md` como prototipo de diseño del sistema mediante el comando `/design-md`
- Este archivo selecciona una fuente de diseño externa de referencia (design system de marca real) y produce: tokens de color, tipografía, componentes clave y principios UX adaptados al contexto institucional
- El `DESIGN.md` actúa como guía viva de UX/UI para todos los incrementos: los componentes PrimeNG 21 y los tokens CSS se configuran siguiendo sus lineamientos
- La identidad visual institucional SED ("Civic Curator") tiene precedencia:
  - Azul SED: `#094cb2`
  - Dorado institucional: `#7e5700`
  - Rojo Bogotá: `#a21638`
- El `DESIGN.md` adapta y enriquece, no reemplaza, los tokens institucionales existentes

### 5.7 Almacenamiento de Archivos

El almacenamiento de archivos adjuntos (soportes e imágenes de firma) es un **parámetro de infraestructura configurable**. El backend implementará una interfaz abstracta `DocumentStorageService` con múltiples implementaciones posibles:

- Sistema de archivos del servidor (FileSystem)
- SharePoint / OneDrive (Office 365)
- Almacenamiento en BLOBs Oracle

La selección de implementación se realiza mediante configuración en `application.yml` sin cambios de código.

---

### 5.8 Criterios de Aceptación por Incremento

#### Incremento 1 — Aprobado cuando:

- [ ] `DESIGN.md` generado y aprobado antes de escribir el primer componente Angular
- [ ] Admin puede crear un contrato con todas sus obligaciones y asignaciones correctamente
- [ ] Contratista autenticado ve exclusivamente sus propios contratos (aislamiento verificado)
- [ ] Ficha de contrato muestra datos completos, supervisor asignado e historial de informes vacío
- [ ] El catálogo de documentos adicionales es parametrizable por tipo de contrato
- [ ] Perfil de usuario permite cargar y actualizar imagen de firma digital
- [ ] Sistema funciona en perfil `local-dev` (HTTP Basic) y `weblogic` (Azure AD JWT)

#### Incremento 2 — Aprobado cuando:

- [ ] Contratista puede crear un informe, registrar actividades en todas las obligaciones y enviarlo
- [ ] Adjuntar soportes (archivo o URL) por obligación funciona correctamente
- [ ] Adjuntar documentos adicionales del catálogo funciona correctamente
- [ ] Revisor ve cola de informes `ENVIADO` y puede aprobar o devolver con observaciones
- [ ] Supervisor ve cola de informes `EN_REVISION` y puede aprobar o devolver con observaciones
- [ ] Contratista ve observaciones completas en informes `DEVUELTO`, edita y reenvía
- [ ] Ciclo completo (borrador → devuelto → corrección → aprobado) funciona sin errores
- [ ] Transiciones de estado inválidas son rechazadas por el backend con error apropiado

#### Incremento 3 — Aprobado cuando:

- [ ] Al aprobar un informe el PDF se genera automáticamente con template institucional SED
- [ ] El PDF incluye imagen de firma del Contratista y del Supervisor correctamente incrustadas
- [ ] El PDF es descargable por Contratista, Supervisor y Admin desde el historial
- [ ] El PDF es inmutable (no se regenera ni modifica tras aprobación)
- [ ] Al enviar un informe, el Revisor recibe email institucional + notificación in-app
- [ ] Al aprobar/devolver, los actores correspondientes reciben ambas notificaciones
- [ ] Centro de notificaciones in-app muestra historial con estado leído/no leído
- [ ] Marcar como leída funciona correctamente

---

## 6. Plan de Iteración SDD

```
FASE ACTUAL (Hoy)
└── PRD aprobado ✓

SIGUIENTE PASO
└── /design-md → generar DESIGN.md → aprobación

INCREMENTO 1
└── Spec técnica I1 → aprobación → implementación
    └── M1: Auth + Perfiles
    └── M2: Administración de Contratos (Admin)
    └── M3: Portal Contratista — Vista de Contratos

INCREMENTO 2
└── Spec técnica I2 → aprobación → implementación
    └── M4: Gestión de Informes (Contratista)
    └── M5: Revisión de Informes (Revisor)
    └── M6: Aprobación Final (Supervisor)

INCREMENTO 3
└── Spec técnica I3 → aprobación → implementación
    └── M7: Generación de PDF con Firmas
    └── M8: Notificaciones Email + In-App

FASE 2 (FUTURO — fuera de alcance actual)
└── Contratos personas jurídicas / proveedores
└── Posible integración SECOP2
```

**Regla SDD Spec-Anchored:** Ningún incremento inicia implementación sin que su spec técnica esté escrita, revisada y aprobada. El PRD es el documento vivo que gobierna la visión; las specs técnicas por incremento son el contrato de implementación.

---

*Documento generado mediante Spec-Driven Development (SDD) — Nivel Spec-Anchored*  
*Ecosistema SED Bogotá — Revisión 1.0 — 2026-04-30*  
*Actualizar cuando cambien decisiones de alcance, roles o requisitos fundamentales.*
