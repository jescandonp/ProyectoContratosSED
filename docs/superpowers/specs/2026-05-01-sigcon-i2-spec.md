# Spec Tecnica - SIGCON Incremento 2
## Nucleo: Informes y Flujo de Aprobacion

> **Metodologia:** Spec-Driven Development (SDD) - Spec-Anchored  
> **Version:** 1.0 - **Fecha:** 2026-05-01  
> **Constitucion:** `docs/superpowers/CONSTITUTION.md`  
> **Arquitectura:** `ARCHITECTURE.md`  
> **PRD de referencia:** `docs/superpowers/specs/2026-04-30-sigcon-prd.md`  
> **Spec base:** `docs/superpowers/specs/2026-04-30-sigcon-i1-spec.md`  
> **Design System:** `Prototipo/DESIGN.md`  
> **Estado:** Listo para revision

---

## 1. Alcance Del Incremento

### Modulos incluidos

| Modulo | Descripcion | Rol principal |
|--------|-------------|---------------|
| M4 - Gestion de Informes | Crear, editar, adjuntar soportes, previsualizar y enviar informes | CONTRATISTA |
| M5 - Revision de Informes | Cola de informes enviados, aprobar revision o devolver con observaciones | REVISOR |
| M6 - Aprobacion Final | Cola de informes en revision, aprobar o devolver con observaciones | SUPERVISOR |

### Fuera de este incremento

- Generacion real de PDF institucional.
- Firmas incrustadas en PDF.
- Notificaciones email e in-app.
- Centro de notificaciones.
- Integracion SECOP2.
- Contratos de personas juridicas.

### Entregable de cierre

Un informe puede recorrer el flujo completo:

`BORRADOR -> ENVIADO -> DEVUELTO -> ENVIADO -> EN_REVISION -> APROBADO`

Todas las observaciones quedan visibles, las transiciones invalidas son rechazadas por backend, y cada rol ve solamente los informes que le corresponden.

---

## 2. Decisiones De Coherencia Con I3

El PRD proyecta que la aprobacion final genera PDF y notificaciones. Para mantener incrementos verificables:

- I2 implementa la maquina de estados y deja el informe en `APROBADO`.
- I2 no genera PDF ni envia notificaciones.
- I2 prepara campos y eventos conceptuales para I3, pero no implementa servicios de PDF/email/in-app.
- I3 conectara la transicion `EN_REVISION -> APROBADO` con generacion de PDF y notificaciones sin cambiar estados.

Regla: en I2 un informe `APROBADO` puede existir sin `pdfRuta`. En I3, los informes aprobados despues de activar PDF deben tener `pdfRuta` no nula.

---

## 3. Base De Datos - DDL Incremento 2

Agregar al `db/00_setup.sql` despues de las tablas I1.

### 3.1 Tabla SGCN_INFORMES

```sql
CREATE SEQUENCE SGCN_INFORMES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_INFORMES (
    ID                  NUMBER          DEFAULT SGCN_INFORMES_SEQ.NEXTVAL PRIMARY KEY,
    ID_CONTRATO         NUMBER          NOT NULL,
    NUMERO              NUMBER          NOT NULL,
    FECHA_INICIO        DATE            NOT NULL,
    FECHA_FIN           DATE            NOT NULL,
    ESTADO              VARCHAR2(20)    DEFAULT 'BORRADOR' NOT NULL,
    FECHA_CREACION      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_ULTIMO_ENVIO  TIMESTAMP,
    FECHA_APROBACION    TIMESTAMP,
    PDF_RUTA            VARCHAR2(500),
    ACTIVO              NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY          VARCHAR2(200),
    UPDATED_AT          TIMESTAMP,
    CONSTRAINT FK_INFORMES_CONTRATO FOREIGN KEY (ID_CONTRATO) REFERENCES SGCN_CONTRATOS(ID),
    CONSTRAINT UQ_INFORMES_CONTRATO_NUMERO UNIQUE (ID_CONTRATO, NUMERO),
    CONSTRAINT CHK_INFORMES_ESTADO CHECK (ESTADO IN ('BORRADOR','ENVIADO','EN_REVISION','DEVUELTO','APROBADO')),
    CONSTRAINT CHK_INFORMES_PERIODO CHECK (FECHA_FIN >= FECHA_INICIO)
);

CREATE INDEX IDX_INFORMES_CONTRATO ON SGCN_INFORMES(ID_CONTRATO);
CREATE INDEX IDX_INFORMES_ESTADO   ON SGCN_INFORMES(ESTADO);

CREATE OR REPLACE TRIGGER TRG_INFORMES_AUDIT
BEFORE UPDATE ON SGCN_INFORMES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.2 Tabla SGCN_ACTIVIDADES

```sql
CREATE SEQUENCE SGCN_ACTIVIDADES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_ACTIVIDADES (
    ID              NUMBER          DEFAULT SGCN_ACTIVIDADES_SEQ.NEXTVAL PRIMARY KEY,
    ID_INFORME      NUMBER          NOT NULL,
    ID_OBLIGACION   NUMBER          NOT NULL,
    DESCRIPCION     VARCHAR2(3000)  NOT NULL,
    PORCENTAJE      NUMBER(5,2)     NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_ACTIVIDADES_INFORME FOREIGN KEY (ID_INFORME) REFERENCES SGCN_INFORMES(ID),
    CONSTRAINT FK_ACTIVIDADES_OBLIGACION FOREIGN KEY (ID_OBLIGACION) REFERENCES SGCN_OBLIGACIONES(ID),
    CONSTRAINT CHK_ACTIVIDADES_PORCENTAJE CHECK (PORCENTAJE >= 0 AND PORCENTAJE <= 100)
);

CREATE INDEX IDX_ACTIVIDADES_INFORME ON SGCN_ACTIVIDADES(ID_INFORME);

CREATE OR REPLACE TRIGGER TRG_ACTIVIDADES_AUDIT
BEFORE UPDATE ON SGCN_ACTIVIDADES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.3 Tabla SGCN_SOPORTES

```sql
CREATE SEQUENCE SGCN_SOPORTES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_SOPORTES (
    ID              NUMBER          DEFAULT SGCN_SOPORTES_SEQ.NEXTVAL PRIMARY KEY,
    ID_ACTIVIDAD    NUMBER          NOT NULL,
    TIPO            VARCHAR2(20)    NOT NULL,
    NOMBRE          VARCHAR2(200)   NOT NULL,
    REFERENCIA      VARCHAR2(1000)  NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_SOPORTES_ACTIVIDAD FOREIGN KEY (ID_ACTIVIDAD) REFERENCES SGCN_ACTIVIDADES(ID),
    CONSTRAINT CHK_SOPORTES_TIPO CHECK (TIPO IN ('ARCHIVO','URL'))
);

CREATE INDEX IDX_SOPORTES_ACTIVIDAD ON SGCN_SOPORTES(ID_ACTIVIDAD);

CREATE OR REPLACE TRIGGER TRG_SOPORTES_AUDIT
BEFORE UPDATE ON SGCN_SOPORTES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.4 Tabla SGCN_DOCS_ADICIONALES

```sql
CREATE SEQUENCE SGCN_DOCS_ADICIONALES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_DOCS_ADICIONALES (
    ID              NUMBER          DEFAULT SGCN_DOCS_ADICIONALES_SEQ.NEXTVAL PRIMARY KEY,
    ID_INFORME      NUMBER          NOT NULL,
    ID_CATALOGO     NUMBER          NOT NULL,
    REFERENCIA      VARCHAR2(1000)  NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_DOCS_ADICIONALES_INFORME FOREIGN KEY (ID_INFORME) REFERENCES SGCN_INFORMES(ID),
    CONSTRAINT FK_DOCS_ADICIONALES_CATALOGO FOREIGN KEY (ID_CATALOGO) REFERENCES SGCN_DOCS_CATALOGO(ID)
);

CREATE INDEX IDX_DOCS_ADICIONALES_INFORME ON SGCN_DOCS_ADICIONALES(ID_INFORME);

CREATE OR REPLACE TRIGGER TRG_DOCS_ADICIONALES_AUDIT
BEFORE UPDATE ON SGCN_DOCS_ADICIONALES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.5 Tabla SGCN_OBSERVACIONES

```sql
CREATE SEQUENCE SGCN_OBSERVACIONES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_OBSERVACIONES (
    ID              NUMBER          DEFAULT SGCN_OBSERVACIONES_SEQ.NEXTVAL PRIMARY KEY,
    ID_INFORME      NUMBER          NOT NULL,
    TEXTO           VARCHAR2(2000)  NOT NULL,
    AUTOR_ROL       VARCHAR2(20)    NOT NULL,
    FECHA           TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_OBSERVACIONES_INFORME FOREIGN KEY (ID_INFORME) REFERENCES SGCN_INFORMES(ID),
    CONSTRAINT CHK_OBSERVACIONES_ROL CHECK (AUTOR_ROL IN ('REVISOR','SUPERVISOR'))
);

CREATE INDEX IDX_OBSERVACIONES_INFORME ON SGCN_OBSERVACIONES(ID_INFORME);

CREATE OR REPLACE TRIGGER TRG_OBSERVACIONES_AUDIT
BEFORE UPDATE ON SGCN_OBSERVACIONES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.6 Datos De Prueba I2

Agregar al `db/01_datos_prueba.sql`:

- Un informe `BORRADOR` para `OPS-2026-001`.
- Una actividad por obligacion.
- Un soporte URL y un soporte archivo de ejemplo.
- Un documento adicional asociado al catalogo obligatorio.

---

## 4. Backend - Spring Boot

### 4.1 Enumeraciones nuevas

```text
EstadoInforme { BORRADOR, ENVIADO, EN_REVISION, DEVUELTO, APROBADO }
TipoSoporte   { ARCHIVO, URL }
RolObservacion { REVISOR, SUPERVISOR }
```

### 4.2 Entidades JPA nuevas

```text
domain/entity/
├── Informe
├── ActividadInforme
├── SoporteAdjunto
├── DocumentoAdicional
└── Observacion
```

Reglas:

- `Informe` pertenece a `Contrato`.
- `ActividadInforme` pertenece a `Informe` y a `Obligacion`.
- `SoporteAdjunto` pertenece a `ActividadInforme`.
- `DocumentoAdicional` pertenece a `Informe` y a `DocumentoCatalogo`.
- `Observacion` pertenece a `Informe`.
- Todas las entidades usan auditoria y borrado logico cuando aplique.

### 4.3 Repositorios

```java
public interface InformeRepository extends JpaRepository<Informe, Long> {
    Page<Informe> findByContratoContratistaAndActivoTrue(Usuario contratista, Pageable pageable);
    Page<Informe> findByContratoRevisorAndEstadoAndActivoTrue(Usuario revisor, EstadoInforme estado, Pageable pageable);
    Page<Informe> findByContratoSupervisorAndEstadoAndActivoTrue(Usuario supervisor, EstadoInforme estado, Pageable pageable);
    Optional<Informe> findByIdAndActivoTrue(Long id);
    Integer countByContratoId(Long contratoId);
}

public interface ActividadInformeRepository extends JpaRepository<ActividadInforme, Long> {
    List<ActividadInforme> findByInformeIdAndActivoTrue(Long informeId);
}

public interface SoporteAdjuntoRepository extends JpaRepository<SoporteAdjunto, Long> {
    List<SoporteAdjunto> findByActividadInformeIdAndActivoTrue(Long actividadId);
}

public interface DocumentoAdicionalRepository extends JpaRepository<DocumentoAdicional, Long> {
    List<DocumentoAdicional> findByInformeIdAndActivoTrue(Long informeId);
}

public interface ObservacionRepository extends JpaRepository<Observacion, Long> {
    List<Observacion> findByInformeIdAndActivoTrueOrderByFechaAsc(Long informeId);
}
```

### 4.4 DTOs

```text
application/dto/informe/
├── InformeResumenDto
├── InformeDetalleDto
├── InformeRequest
├── ActividadInformeDto
├── ActividadInformeRequest
├── SoporteAdjuntoDto
├── SoporteUrlRequest
├── DocumentoAdicionalDto
├── DocumentoAdicionalRequest
├── ObservacionDto
└── ObservacionRequest
```

Campos minimos:

- `InformeResumenDto`: id, numero, contratoNumero, periodo, estado, fechaUltimoEnvio, fechaAprobacion.
- `InformeDetalleDto`: resumen, contrato, actividades, documentosAdicionales, observaciones.
- `InformeRequest`: idContrato, fechaInicio, fechaFin.
- `ActividadInformeRequest`: idObligacion, descripcion, porcentaje.
- `SoporteUrlRequest`: nombre, url.
- `DocumentoAdicionalRequest`: idCatalogo, referencia.
- `ObservacionRequest`: texto.

### 4.5 Servicios

```text
application/service/
├── InformeService
├── ActividadInformeService
├── SoporteAdjuntoService
├── DocumentoAdicionalInformeService
├── ObservacionService
└── InformeEstadoService
```

Reglas de negocio:

- Solo el contratista asignado puede crear informes sobre sus contratos activos.
- Solo `BORRADOR` y `DEVUELTO` son editables por contratista.
- `BORRADOR -> ENVIADO` exige al menos una actividad.
- `DEVUELTO -> ENVIADO` exige conservar historial de observaciones y actualizar `fechaUltimoEnvio`.
- `ENVIADO -> EN_REVISION` solo puede hacerlo el revisor asignado.
- `ENVIADO -> DEVUELTO` solo puede hacerlo el revisor asignado y exige observacion.
- `EN_REVISION -> APROBADO` solo puede hacerlo el supervisor asignado.
- `EN_REVISION -> DEVUELTO` solo puede hacerlo el supervisor asignado y exige observacion.
- `APROBADO` es terminal.
- Los soportes de tipo URL deben ser URLs absolutas `http` o `https`.
- Los soportes de tipo archivo usan el mismo patron de almacenamiento configurable definido para firma en I1.

### 4.6 Controllers

```text
web/controller/
├── InformeController            @RequestMapping("/api/informes")
├── ActividadInformeController   @RequestMapping("/api/informes/{informeId}/actividades")
├── SoporteAdjuntoController     @RequestMapping("/api/actividades/{actividadId}/soportes")
└── DocumentoAdicionalInformeController @RequestMapping("/api/informes/{informeId}/documentos-adicionales")
```

Endpoints:

```text
GET    /api/informes
GET    /api/informes/{id}
POST   /api/informes                         [CONTRATISTA]
PUT    /api/informes/{id}                    [CONTRATISTA, editable]
POST   /api/informes/{id}/enviar             [CONTRATISTA]
POST   /api/informes/{id}/aprobar-revision   [REVISOR]
POST   /api/informes/{id}/devolver-revision  [REVISOR]
POST   /api/informes/{id}/aprobar            [SUPERVISOR]
POST   /api/informes/{id}/devolver           [SUPERVISOR]

POST   /api/informes/{id}/actividades
PUT    /api/informes/{id}/actividades/{actividadId}
DELETE /api/informes/{id}/actividades/{actividadId}

POST   /api/actividades/{actividadId}/soportes/url
POST   /api/actividades/{actividadId}/soportes/archivo
DELETE /api/actividades/{actividadId}/soportes/{soporteId}

POST   /api/informes/{id}/documentos-adicionales
DELETE /api/informes/{id}/documentos-adicionales/{documentoId}
```

### 4.7 Codigos De Error Nuevos

```text
INFORME_NO_ENCONTRADO
INFORME_NO_EDITABLE
TRANSICION_INVALIDA
OBSERVACION_REQUERIDA
ACTIVIDAD_REQUERIDA
PORCENTAJE_INVALIDO
SOPORTE_INVALIDO
DOCUMENTO_ADICIONAL_REQUERIDO
CONTRATO_NO_ACTIVO
```

---

## 5. Frontend - Angular 20

### 5.1 Estructura

```text
src/app/features/informes/
├── informe-form/
├── informe-detalle/
├── informe-preview/
├── corregir-informe/
├── cola-revision/
└── cola-aprobacion/
```

### 5.2 Rutas

```typescript
{ path: 'contratos/:contratoId/informes/nuevo', loadComponent: ... }
{ path: 'informes/:id', loadComponent: ... }
{ path: 'informes/:id/editar', loadComponent: ... }
{ path: 'informes/:id/preview', loadComponent: ... }
{ path: 'informes/:id/corregir', loadComponent: ... }
{ path: 'revision/informes', canActivate: [roleGuard('REVISOR')], loadComponent: ... }
{ path: 'aprobacion/informes', canActivate: [roleGuard('SUPERVISOR')], loadComponent: ... }
```

### 5.3 Pantallas De Referencia

| Pantalla prototipo | Modulo | Ruta Angular |
|--------------------|--------|-------------|
| `nuevo_informe_de_actividades_optimizado_sigcon` | M4 | `/contratos/:contratoId/informes/nuevo` |
| `corregir_informe_devuelto_optimizado_sigcon` | M4 | `/informes/:id/corregir` |
| `cola_de_revisi_n_de_informes_sigcon` | M5/M6 | `/revision/informes`, `/aprobacion/informes` |
| `detalle_del_contrato_sigcon` | M4 historial | `/contratos/:id` |

### 5.4 Comportamiento UX

- El formulario de informe lista obligaciones del contrato en orden.
- Cada obligacion permite descripcion, porcentaje y soportes.
- El porcentaje acepta 0 a 100.
- Los documentos adicionales se cargan desde el catalogo OPS.
- Antes de enviar hay confirmacion explicita.
- En estado `DEVUELTO`, las observaciones se muestran junto al formulario de correccion.
- Revisor y supervisor ven cola de trabajo paginada con filtros por contrato, contratista y periodo.
- Las acciones de devolver exigen texto antes de enviar.

---

## 6. Seguridad

- `CONTRATISTA`: crea, edita, envia y corrige sus informes.
- `REVISOR`: ve y actua solo sobre informes `ENVIADO` de contratos asignados.
- `SUPERVISOR`: ve y actua solo sobre informes `EN_REVISION` de contratos supervisados.
- `ADMIN`: consulta historica de informes, sin aprobar ni devolver en I2.
- Todos los endpoints validan rol y pertenencia al contrato.

---

## 7. Criterios De Aceptacion

### Backend

- [ ] Contratista crea informe `BORRADOR` sobre contrato activo propio.
- [ ] Contratista no crea informe sobre contrato ajeno o inactivo.
- [ ] Contratista agrega actividades por obligacion con porcentaje 0-100.
- [ ] Soportes URL y archivo quedan asociados a actividad.
- [ ] Documentos adicionales quedan asociados al informe.
- [ ] `BORRADOR -> ENVIADO` falla si no hay actividades.
- [ ] Revisor ve solo informes `ENVIADO` asignados.
- [ ] Revisor aprueba y el estado pasa a `EN_REVISION`.
- [ ] Revisor devuelve con observacion obligatoria y el estado pasa a `DEVUELTO`.
- [ ] Supervisor ve solo informes `EN_REVISION` supervisados.
- [ ] Supervisor aprueba y el estado pasa a `APROBADO`.
- [ ] Supervisor devuelve con observacion obligatoria y el estado pasa a `DEVUELTO`.
- [ ] Transiciones invalidas retornan `TRANSICION_INVALIDA`.
- [ ] `APROBADO` no permite edicion ni devolucion.

### Frontend

- [ ] Boton "Nuevo Informe" queda habilitado para contratista con contrato `EN_EJECUCION`.
- [ ] Formulario de informe muestra obligaciones en orden.
- [ ] El informe se guarda como borrador y se puede reabrir.
- [ ] Envio de informe requiere confirmacion.
- [ ] Correccion muestra observaciones historicas.
- [ ] Cola del revisor muestra informes enviados asignados.
- [ ] Cola del supervisor muestra informes en revision asignados.
- [ ] Acciones de devolver bloquean envio si la observacion esta vacia.

### General

- [ ] El flujo completo de I2 funciona en `local-dev`.
- [ ] No se implementa PDF real ni notificaciones.
- [ ] Swagger documenta todos los endpoints nuevos.
- [ ] No se rompen criterios I1.

---

## 8. Impacto Sobre I1 E I3

- I1 debe prever historial de informes en detalle de contrato, pero puede mostrarlo vacio hasta I2.
- I2 agrega datos y endpoints consumidos por I3.
- I3 se conecta a la aprobacion final sin modificar los estados de I2.
- Si I3 requiere campos adicionales de PDF o notificacion, deben agregarse con migracion compatible.

---

*Spec tecnica generada mediante SDD Spec-Anchored - SIGCON - Incremento 2 - 2026-05-01.*
