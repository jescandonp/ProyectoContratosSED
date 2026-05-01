# Spec Técnica — SIGCON Incremento 1
## Fundación: Auth + Contratos + Vista Contratista

> **Metodología:** Spec-Driven Development (SDD) — Spec-Anchored  
> **Versión:** 1.0 · **Fecha:** 2026-04-30  
> **PRD de referencia:** `docs/specs/2026-04-30-sigcon-prd.md`
> **Design System:** `Prototipo/DESIGN.md`  
> **Estado:** Listo para implementación

---

## Tabla de Contenido

1. [Alcance del Incremento](#1-alcance-del-incremento)
2. [Estructura del Proyecto](#2-estructura-del-proyecto)
3. [Base de Datos — DDL Incremento 1](#3-base-de-datos--ddl-incremento-1)
4. [Backend — Spring Boot](#4-backend--spring-boot)
5. [Frontend — Angular 20](#5-frontend--angular-20)
6. [Seguridad y Autenticación](#6-seguridad-y-autenticación)
7. [Pantallas de Referencia (Prototipo)](#7-pantallas-de-referencia-prototipo)
8. [Criterios de Aceptación](#8-criterios-de-aceptación)

---

## 1. Alcance del Incremento

### Módulos incluidos

| Módulo | Descripción | Rol principal |
|--------|-------------|---------------|
| M1 — Auth y Perfiles | Login, sesión, perfil, imagen de firma | Todos |
| M2 — Admin de Contratos | CRUD contratos, obligaciones, catálogo docs, asignación de roles | ADMIN |
| M3 — Portal Contratista | Dashboard contratos, ficha de contrato, historial de informes | CONTRATISTA |

### Módulos explícitamente fuera de este incremento

- Creación y gestión de informes (M4 — Incremento 2)
- Flujo de revisión y aprobación (M5, M6 — Incremento 2)
- Generación de PDF (M7 — Incremento 3)
- Notificaciones email e in-app (M8 — Incremento 3)

### Entregable de cierre

Sistema desplegable en `local-dev` donde un Admin puede registrar contratos completos y un Contratista puede autenticarse y ver sus contratos con toda la información.

---

## 2. Estructura del Proyecto

### Nombres y coordenadas

| Elemento | Valor |
|----------|-------|
| Nombre del sistema | SIGCON |
| Artefacto WAR | `sigcon-backend.war` |
| Contexto WebLogic | `/sigcon` |
| Paquete Java base | `co.gov.bogota.sed.sigcon` |
| Proyecto Angular | `sigcon-angular` |
| Esquema Oracle | `SED_SIGCON` |
| Prefijo de tablas | `SGCN_` |
| Perfiles Spring | `local-dev` · `weblogic` |

### Estructura de carpetas

```
ProyectoContratosSED/
├── docs/
│   ├── CONSTITUTION.md
│   ├── ARCHITECTURE.md
│   ├── TECNOLOGIAS.md
│   ├── ARRANQUE.md
│   ├── specs/
│   │   ├── 2026-04-30-sigcon-prd.md
│   │   └── 2026-04-30-sigcon-i1-spec.md (este documento)
│   └── plans/
├── Prototipo/
│   └── DESIGN.md                         ← referencia de diseño
├── sigcon-backend/                      ← Spring Boot WAR
│   ├── pom.xml
│   └── src/main/
│       ├── java/co/gov/bogota/sed/sigcon/
│       │   ├── domain/
│       │   │   ├── entity/
│       │   │   ├── enums/
│       │   │   └── repository/
│       │   ├── application/
│       │   │   ├── service/
│       │   │   ├── dto/
│       │   │   └── mapper/
│       │   ├── web/
│       │   │   ├── controller/
│       │   │   └── exception/
│       │   └── config/
│       └── resources/
│           └── application.yml
├── sigcon-angular/                      ← Angular 20 SPA
│   ├── package.json
│   ├── angular.json
│   ├── proxy.conf.json
│   └── src/app/
│       ├── core/
│       ├── shared/
│       └── features/
└── db/
    ├── 00_setup.sql                     ← DDL completo
    └── 01_datos_prueba.sql
```

---

## 3. Base de Datos — DDL Incremento 1

Todas las tablas en el esquema **`SED_SIGCON`**. Todas incluyen campos de auditoría estándar SED.

### 3.1 Tabla SGCN_USUARIOS

```sql
CREATE SEQUENCE SGCN_USUARIOS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_USUARIOS (
    ID              NUMBER          DEFAULT SGCN_USUARIOS_SEQ.NEXTVAL PRIMARY KEY,
    EMAIL           VARCHAR2(200)   NOT NULL UNIQUE,   -- UPN Office 365
    NOMBRE          VARCHAR2(200)   NOT NULL,
    CARGO           VARCHAR2(200),
    ROL             VARCHAR2(20)    NOT NULL,           -- CONTRATISTA|REVISOR|SUPERVISOR|ADMIN
    FIRMA_IMAGEN    VARCHAR2(500),                      -- ruta al archivo de imagen
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT CHK_USUARIOS_ROL CHECK (ROL IN ('CONTRATISTA','REVISOR','SUPERVISOR','ADMIN'))
);

CREATE INDEX IDX_USUARIOS_EMAIL ON SGCN_USUARIOS(EMAIL);
CREATE INDEX IDX_USUARIOS_ROL   ON SGCN_USUARIOS(ROL);

CREATE OR REPLACE TRIGGER TRG_USUARIOS_AUDIT
BEFORE UPDATE ON SGCN_USUARIOS FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.2 Tabla SGCN_CONTRATOS

```sql
CREATE SEQUENCE SGCN_CONTRATOS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_CONTRATOS (
    ID                  NUMBER          DEFAULT SGCN_CONTRATOS_SEQ.NEXTVAL PRIMARY KEY,
    NUMERO              VARCHAR2(50)    NOT NULL UNIQUE,
    OBJETO              VARCHAR2(1000)  NOT NULL,
    TIPO                VARCHAR2(20)    DEFAULT 'OPS' NOT NULL,
    VALOR_TOTAL         NUMBER(18,2)    NOT NULL,
    FECHA_INICIO        DATE            NOT NULL,
    FECHA_FIN           DATE            NOT NULL,
    ESTADO              VARCHAR2(20)    DEFAULT 'EN_EJECUCION' NOT NULL,
    ID_CONTRATISTA      NUMBER          NOT NULL,
    ID_REVISOR          NUMBER,
    ID_SUPERVISOR       NUMBER,
    ACTIVO              NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY          VARCHAR2(200),
    UPDATED_AT          TIMESTAMP,
    CONSTRAINT FK_CONTRATOS_CONTRATISTA FOREIGN KEY (ID_CONTRATISTA) REFERENCES SGCN_USUARIOS(ID),
    CONSTRAINT FK_CONTRATOS_REVISOR     FOREIGN KEY (ID_REVISOR)     REFERENCES SGCN_USUARIOS(ID),
    CONSTRAINT FK_CONTRATOS_SUPERVISOR  FOREIGN KEY (ID_SUPERVISOR)  REFERENCES SGCN_USUARIOS(ID),
    CONSTRAINT CHK_CONTRATOS_ESTADO     CHECK (ESTADO IN ('EN_EJECUCION','LIQUIDADO','CERRADO')),
    CONSTRAINT CHK_CONTRATOS_TIPO       CHECK (TIPO IN ('OPS'))
);

CREATE INDEX IDX_CONTRATOS_CONTRATISTA ON SGCN_CONTRATOS(ID_CONTRATISTA);
CREATE INDEX IDX_CONTRATOS_SUPERVISOR  ON SGCN_CONTRATOS(ID_SUPERVISOR);
CREATE INDEX IDX_CONTRATOS_ESTADO      ON SGCN_CONTRATOS(ESTADO);

CREATE OR REPLACE TRIGGER TRG_CONTRATOS_AUDIT
BEFORE UPDATE ON SGCN_CONTRATOS FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.3 Tabla SGCN_OBLIGACIONES

```sql
CREATE SEQUENCE SGCN_OBLIGACIONES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_OBLIGACIONES (
    ID              NUMBER          DEFAULT SGCN_OBLIGACIONES_SEQ.NEXTVAL PRIMARY KEY,
    ID_CONTRATO     NUMBER          NOT NULL,
    DESCRIPCION     VARCHAR2(2000)  NOT NULL,
    ORDEN           NUMBER          NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_OBLIGACIONES_CONTRATO FOREIGN KEY (ID_CONTRATO) REFERENCES SGCN_CONTRATOS(ID)
);

CREATE INDEX IDX_OBLIGACIONES_CONTRATO ON SGCN_OBLIGACIONES(ID_CONTRATO);

CREATE OR REPLACE TRIGGER TRG_OBLIGACIONES_AUDIT
BEFORE UPDATE ON SGCN_OBLIGACIONES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.4 Tabla SGCN_DOCS_CATALOGO

```sql
CREATE SEQUENCE SGCN_DOCS_CATALOGO_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_DOCS_CATALOGO (
    ID              NUMBER          DEFAULT SGCN_DOCS_CATALOGO_SEQ.NEXTVAL PRIMARY KEY,
    NOMBRE          VARCHAR2(200)   NOT NULL,
    DESCRIPCION     VARCHAR2(500),
    OBLIGATORIO     NUMBER(1)       DEFAULT 0 NOT NULL,
    TIPO_CONTRATO   VARCHAR2(20)    DEFAULT 'OPS' NOT NULL,
    ACTIVO          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT CHK_DOCS_CATALOGO_TIPO CHECK (TIPO_CONTRATO IN ('OPS'))
);

CREATE OR REPLACE TRIGGER TRG_DOCS_CATALOGO_AUDIT
BEFORE UPDATE ON SGCN_DOCS_CATALOGO FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.5 Datos de prueba (01_datos_prueba.sql)

```sql
-- Usuarios de prueba para local-dev
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('admin@educacionbogota.edu.co',       'Administrador SIGCON',  'Jefe de Sistemas',       'ADMIN');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('juan.escandon@educacionbogota.edu.co','Juan Escandón Pérez',  'Contratista OPS',        'CONTRATISTA');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('revisor1@educacionbogota.edu.co',    'María Revisora',        'Apoyo Supervisión',      'REVISOR');
INSERT INTO SGCN_USUARIOS (EMAIL, NOMBRE, CARGO, ROL) VALUES
  ('supervisor1@educacionbogota.edu.co', 'Carlos Supervisor',     'Supervisor Contractual', 'SUPERVISOR');

-- Contrato de prueba
INSERT INTO SGCN_CONTRATOS (NUMERO, OBJETO, TIPO, VALOR_TOTAL, FECHA_INICIO, FECHA_FIN,
                             ESTADO, ID_CONTRATISTA, ID_REVISOR, ID_SUPERVISOR)
VALUES ('OPS-2026-001',
        'Prestación de servicios de apoyo en gestión documental y administrativa',
        'OPS', 18000000, DATE '2026-01-15', DATE '2026-12-31',
        'EN_EJECUCION', 2, 3, 4);

-- Obligaciones del contrato
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Apoyar la organización y clasificación del archivo físico y digital de la dependencia', 1);
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Elaborar y radicar comunicaciones oficiales según instrucciones del supervisor', 2);
INSERT INTO SGCN_OBLIGACIONES (ID_CONTRATO, DESCRIPCION, ORDEN) VALUES
  (1, 'Asistir a reuniones de seguimiento y elaborar actas de las mismas', 3);

-- Catálogo de documentos
INSERT INTO SGCN_DOCS_CATALOGO (NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO) VALUES
  ('Planilla de aportes seguridad social', 'Comprobante de pago al sistema de seguridad social', 1, 'OPS');
INSERT INTO SGCN_DOCS_CATALOGO (NOMBRE, DESCRIPCION, OBLIGATORIO, TIPO_CONTRATO) VALUES
  ('Constancia de afiliación EPS', 'Certificado vigente de afiliación a EPS', 0, 'OPS');

COMMIT;
```

---

## 4. Backend — Spring Boot

### 4.1 Configuración pom.xml

Siguiendo exactamente la arquitectura SED (`docs/ARCHITECTURE.md`):

- `<packaging>war</packaging>`
- `<java.version>8</java.version>`
- `spring-boot-starter-tomcat` → `<scope>provided</scope>`
- Clase principal extiende `SpringBootServletInitializer`
- `weblogic.xml` con `<prefer-web-inf-classes>true</prefer-web-inf-classes>`
- Nombre del artefacto: `sigcon-backend`

### 4.2 Enumeraciones de Dominio

```
co.gov.bogota.sed.sigcon.domain.enums/
├── RolUsuario      { CONTRATISTA, REVISOR, SUPERVISOR, ADMIN }
├── EstadoContrato  { EN_EJECUCION, LIQUIDADO, CERRADO }
└── TipoContrato    { OPS }
```

### 4.3 Entidades JPA

#### `Usuario`

```java
// domain/entity/Usuario.java
@Entity @Table(name = "SGCN_USUARIOS")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = SEQUENCE, generator = "SGCN_USUARIOS_SEQ")
    @SequenceGenerator(name = "SGCN_USUARIOS_SEQ", sequenceName = "SGCN_USUARIOS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 200)
    private String email;                          // UPN Office 365

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Column(name = "CARGO", length = 200)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROL", nullable = false, length = 20)
    private RolUsuario rol;

    @Column(name = "FIRMA_IMAGEN", length = 500)
    private String firmaImagen;                    // ruta al archivo de imagen

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @CreatedDate  @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    @CreatedBy   @Column(name = "CREATED_BY", updatable = false, length = 200)
    private String createdBy;
    @LastModifiedDate @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### `Contrato`

```java
// domain/entity/Contrato.java
@Entity @Table(name = "SGCN_CONTRATOS")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Contrato {
    @Id @GeneratedValue(strategy = SEQUENCE, generator = "SGCN_CONTRATOS_SEQ")
    @SequenceGenerator(name = "SGCN_CONTRATOS_SEQ", sequenceName = "SGCN_CONTRATOS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NUMERO", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "OBJETO", nullable = false, length = 1000)
    private String objeto;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 20)
    private TipoContrato tipo;

    @Column(name = "VALOR_TOTAL", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "FECHA_INICIO", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "FECHA_FIN", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoContrato estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CONTRATISTA", nullable = false)
    private Usuario contratista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_REVISOR")
    private Usuario revisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SUPERVISOR")
    private Usuario supervisor;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Obligacion> obligaciones = new ArrayList<>();

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @CreatedDate  @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    @CreatedBy   @Column(name = "CREATED_BY", updatable = false, length = 200)
    private String createdBy;
    @LastModifiedDate @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### `Obligacion`

```java
// domain/entity/Obligacion.java
@Entity @Table(name = "SGCN_OBLIGACIONES")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Obligacion {
    @Id @GeneratedValue(strategy = SEQUENCE, generator = "SGCN_OBLIGACIONES_SEQ")
    @SequenceGenerator(name = "SGCN_OBLIGACIONES_SEQ", sequenceName = "SGCN_OBLIGACIONES_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CONTRATO", nullable = false)
    private Contrato contrato;

    @Column(name = "DESCRIPCION", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "ORDEN", nullable = false)
    private Integer orden;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @CreatedDate  @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    @CreatedBy   @Column(name = "CREATED_BY", updatable = false, length = 200)
    private String createdBy;
    @LastModifiedDate @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### `DocumentoCatalogo`

```java
// domain/entity/DocumentoCatalogo.java
@Entity @Table(name = "SGCN_DOCS_CATALOGO")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentoCatalogo {
    @Id @GeneratedValue(strategy = SEQUENCE, generator = "SGCN_DOCS_CATALOGO_SEQ")
    @SequenceGenerator(name = "SGCN_DOCS_CATALOGO_SEQ", sequenceName = "SGCN_DOCS_CATALOGO_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "OBLIGATORIO", nullable = false)
    private Boolean obligatorio = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CONTRATO", nullable = false, length = 20)
    private TipoContrato tipoContrato;

    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;

    @CreatedDate  @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
    @CreatedBy   @Column(name = "CREATED_BY", updatable = false, length = 200)
    private String createdBy;
    @LastModifiedDate @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

### 4.4 Repositorios

```java
// UsuarioRepository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);
    Page<Usuario> findByActivoTrue(Pageable pageable);
}

// ContratoRepository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    Page<Contrato> findByContratistaAndActivoTrue(Usuario contratista, Pageable pageable);
    Page<Contrato> findBySupervisorAndActivoTrue(Usuario supervisor, Pageable pageable);
    Page<Contrato> findByActivoTrue(Pageable pageable);
    Optional<Contrato> findByNumeroAndActivoTrue(String numero);
}

// ObligacionRepository
public interface ObligacionRepository extends JpaRepository<Obligacion, Long> {
    List<Obligacion> findByContratoIdAndActivoTrueOrderByOrdenAsc(Long contratoId);
}

// DocumentoCatalogoRepository
public interface DocumentoCatalogoRepository extends JpaRepository<DocumentoCatalogo, Long> {
    List<DocumentoCatalogo> findByTipoContratoAndActivoTrue(TipoContrato tipo);
    Page<DocumentoCatalogo> findByActivoTrue(Pageable pageable);
}
```

### 4.5 DTOs

```
application/dto/
├── usuario/
│   ├── UsuarioDto          { id, email, nombre, cargo, rol, firmaImagen, activo }
│   ├── UsuarioRequest      { email, nombre, cargo, rol }   -- Admin crea/edita
│   └── PerfilUpdateRequest { nombre, cargo }               -- Usuario edita su perfil
│
├── contrato/
│   ├── ContratoResumenDto  { id, numero, objeto, tipo, estado, fechaInicio, fechaFin,
│   │                          valorTotal, contratistaNombre, supervisorNombre }
│   ├── ContratoDetalleDto  { ...resumen, contratista, revisor, supervisor,
│   │                          obligaciones[], docsAplicables[] }
│   └── ContratoRequest     { numero, objeto, tipo, valorTotal, fechaInicio, fechaFin,
│                              idContratista, idRevisor, idSupervisor }
│
├── obligacion/
│   ├── ObligacionDto       { id, descripcion, orden }
│   └── ObligacionRequest   { descripcion, orden }
│
└── catalogo/
    ├── DocumentoCatalogoDto     { id, nombre, descripcion, obligatorio, tipoContrato }
    └── DocumentoCatalogoRequest { nombre, descripcion, obligatorio, tipoContrato }
```

### 4.6 API REST — Endpoints del Incremento 1

#### Usuarios y Perfil

```
GET    /api/usuarios/me
       → UsuarioDto (usuario autenticado)
       → 200 OK | 401 si no autenticado

PUT    /api/usuarios/me
       Body: PerfilUpdateRequest
       → UsuarioDto actualizado
       → 200 OK | 400 si datos inválidos

POST   /api/usuarios/me/firma
       Body: multipart/form-data { file: imagen JPG/PNG, max 2MB }
       → UsuarioDto con firmaImagen actualizada
       → 200 OK | 400 si formato inválido

GET    /api/usuarios                    [ADMIN]
       Params: page, size, rol (opcional)
       → Page<UsuarioDto>
       → 200 OK | 403 si no es ADMIN

POST   /api/usuarios                    [ADMIN]
       Body: UsuarioRequest
       → UsuarioDto creado
       → 201 Created | 400 | 409 si email duplicado

PUT    /api/usuarios/{id}               [ADMIN]
       Body: UsuarioRequest
       → UsuarioDto actualizado
       → 200 OK | 404 | 403

PATCH  /api/usuarios/{id}/estado        [ADMIN]
       Body: { activo: boolean }
       → 204 No Content | 404 | 403
```

#### Contratos

```
GET    /api/contratos
       → Page<ContratoResumenDto>
       Comportamiento por rol:
         CONTRATISTA → solo sus contratos (ID_CONTRATISTA = usuario actual)
         SUPERVISOR  → contratos que supervisa
         ADMIN       → todos los contratos
         REVISOR     → contratos asignados para revisión
       Params: page, size, estado (opcional), search (opcional, busca en numero/objeto)
       → 200 OK | 403

GET    /api/contratos/{id}
       → ContratoDetalleDto (incluye obligaciones y docsAplicables)
       Validación: CONTRATISTA solo puede ver sus propios contratos
       → 200 OK | 404 | 403

POST   /api/contratos                   [ADMIN]
       Body: ContratoRequest
       → ContratoDetalleDto creado
       → 201 Created | 400 | 403 | 409 si número duplicado

PUT    /api/contratos/{id}              [ADMIN]
       Body: ContratoRequest
       → ContratoDetalleDto actualizado
       → 200 OK | 400 | 404 | 403

PATCH  /api/contratos/{id}/estado       [ADMIN]
       Body: { estado: "EN_EJECUCION" | "LIQUIDADO" | "CERRADO" }
       → 204 No Content | 400 | 404 | 403
```

#### Obligaciones

```
GET    /api/contratos/{id}/obligaciones
       → List<ObligacionDto> ordenada por orden ASC
       → 200 OK | 404

POST   /api/contratos/{id}/obligaciones     [ADMIN]
       Body: ObligacionRequest
       → ObligacionDto creado
       → 201 Created | 400 | 403

PUT    /api/contratos/{contratoId}/obligaciones/{id}   [ADMIN]
       Body: ObligacionRequest
       → ObligacionDto actualizado
       → 200 OK | 404 | 403

DELETE /api/contratos/{contratoId}/obligaciones/{id}   [ADMIN]
       Borrado lógico (activo = false)
       → 204 No Content | 404 | 403
```

#### Catálogo de Documentos

```
GET    /api/documentos-catalogo
       Params: tipoContrato (default OPS), page, size
       → Page<DocumentoCatalogoDto>
       → 200 OK

POST   /api/documentos-catalogo         [ADMIN]
       Body: DocumentoCatalogoRequest
       → DocumentoCatalogoDto creado
       → 201 Created | 400 | 403

PUT    /api/documentos-catalogo/{id}    [ADMIN]
       Body: DocumentoCatalogoRequest
       → DocumentoCatalogoDto actualizado
       → 200 OK | 404 | 403

DELETE /api/documentos-catalogo/{id}    [ADMIN]
       Borrado lógico
       → 204 No Content | 404 | 403
```

#### Respuesta de error estándar

```json
{
  "error": "CODIGO_ERROR",
  "mensaje": "Descripción legible del error",
  "timestamp": "2026-04-30T10:00:00"
}
```

Códigos de error usados en I1: `USUARIO_NO_ENCONTRADO`, `CONTRATO_NO_ENCONTRADO`,
`NUMERO_CONTRATO_DUPLICADO`, `EMAIL_DUPLICADO`, `ACCESO_DENEGADO`,
`FORMATO_IMAGEN_INVALIDO`, `ESTADO_INVALIDO`.

### 4.7 Estructura de Controllers

```
web/controller/
├── UsuarioController       @RequestMapping("/api/usuarios")
├── ContratoController      @RequestMapping("/api/contratos")
├── ObligacionController    @RequestMapping("/api/contratos/{id}/obligaciones")
└── DocumentoCatalogoController  @RequestMapping("/api/documentos-catalogo")
```

### 4.8 application.yml

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILE:local-dev}
  application:
    name: sigcon-backend
  jpa:
    open-in-view: false

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    display-request-duration: true

---
spring:
  config:
    activate:
      on-profile: local-dev
  datasource:
    url: jdbc:oracle:thin:@localhost:1521/XEPDB1
    username: ${DB_USERNAME:SED_SIGCON}
    password: ${DB_PASSWORD:Sigcon_2026#}
    driver-class-name: oracle.jdbc.OracleDriver
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect
    hibernate:
      ddl-auto: validate
    show-sql: false

---
spring:
  config:
    activate:
      on-profile: weblogic
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect
    hibernate:
      ddl-auto: validate
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
          jwk-set-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/discovery/v2.0/keys
```

---

## 5. Frontend — Angular 20

### 5.1 Dependencias package.json

```json
{
  "dependencies": {
    "@angular/core": "^20.0.0",
    "@angular/common": "^20.0.0",
    "@angular/forms": "^20.0.0",
    "@angular/router": "^20.0.0",
    "@angular/platform-browser": "^20.0.0",
    "primeng": "^21.0.0",
    "@primeng/themes": "^21.0.0",
    "primeicons": "^7.0.0",
    "@azure/msal-angular": "^3.0.0",
    "@azure/msal-browser": "^3.0.0",
    "tailwindcss": "^3.4.0",
    "rxjs": "^7.8.0"
  }
}
```

### 5.2 Estructura Angular

```
src/app/
├── core/
│   ├── auth/
│   │   ├── auth.service.ts           ← facade MSAL + DevSession
│   │   ├── auth.guard.ts             ← MsalGuard (producción) / basicGuard (dev)
│   │   ├── role.guard.ts             ← canActivate por rol
│   │   ├── msal.interceptor.ts       ← Bearer token en cada request
│   │   └── dev-session.service.ts    ← HTTP Basic para local-dev
│   ├── models/
│   │   ├── usuario.model.ts
│   │   ├── contrato.model.ts
│   │   ├── obligacion.model.ts
│   │   └── documento-catalogo.model.ts
│   └── services/
│       ├── usuario.service.ts
│       ├── contrato.service.ts
│       ├── obligacion.service.ts
│       └── documento-catalogo.service.ts
│
├── shared/
│   ├── components/
│   │   ├── sidebar/                  ← PanelMenu navegación lateral
│   │   ├── topbar/                   ← Usuario O365, logout, campana notif.
│   │   ├── kpi-card/                 ← Tarjeta métricas dashboard
│   │   ├── status-chip/              ← p-tag estados contrato/informe
│   │   └── empty-state/              ← Pantalla vacía parametrizable
│   ├── design-tokens.scss            ← Variables CSS del DESIGN.md
│   └── app-shell.component.ts        ← Layout: sidebar + topbar + router-outlet
│
└── features/
    ├── auth/
    │   └── login/
    │       └── login.component.ts     ← Pantalla login institucional
    │
    ├── perfil/
    │   └── perfil.component.ts        ← Mi perfil + carga imagen firma
    │
    ├── contratos/                     ← CONTRATISTA / SUPERVISOR / ADMIN (vista)
    │   ├── contratos-list/
    │   │   └── contratos-list.component.ts   ← tabla p-table con filtros
    │   └── contrato-detalle/
    │       └── contrato-detalle.component.ts ← ficha + historial informes
    │
    └── admin/                         ← Solo ROL ADMIN
        ├── contratos/
        │   ├── admin-contratos-list/
        │   │   └── admin-contratos-list.component.ts
        │   └── admin-contrato-form/
        │       └── admin-contrato-form.component.ts   ← crear/editar
        ├── usuarios/
        │   ├── admin-usuarios-list/
        │   │   └── admin-usuarios-list.component.ts
        │   └── admin-usuario-form/
        │       └── admin-usuario-form.component.ts
        └── documentos-catalogo/
            ├── admin-catalogo-list/
            │   └── admin-catalogo-list.component.ts
            └── admin-catalogo-form/
                └── admin-catalogo-form.component.ts
```

### 5.3 Rutas (app.routes.ts)

```typescript
export const APP_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component'),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'contratos', pathMatch: 'full' },

      // Perfil (todos los roles)
      {
        path: 'perfil',
        loadComponent: () => import('./features/perfil/perfil.component'),
      },

      // Portal contratista / supervisor (vista de contratos)
      {
        path: 'contratos',
        loadComponent: () => import('./features/contratos/contratos-list/contratos-list.component'),
      },
      {
        path: 'contratos/:id',
        loadComponent: () => import('./features/contratos/contrato-detalle/contrato-detalle.component'),
      },

      // Módulo Admin — protegido por roleGuard('ADMIN')
      {
        path: 'admin',
        canActivate: [roleGuard('ADMIN')],
        children: [
          { path: '', redirectTo: 'contratos', pathMatch: 'full' },
          {
            path: 'contratos',
            loadComponent: () => import('./features/admin/contratos/admin-contratos-list/admin-contratos-list.component'),
          },
          {
            path: 'contratos/nuevo',
            loadComponent: () => import('./features/admin/contratos/admin-contrato-form/admin-contrato-form.component'),
          },
          {
            path: 'contratos/:id/editar',
            loadComponent: () => import('./features/admin/contratos/admin-contrato-form/admin-contrato-form.component'),
          },
          {
            path: 'usuarios',
            loadComponent: () => import('./features/admin/usuarios/admin-usuarios-list/admin-usuarios-list.component'),
          },
          {
            path: 'documentos-catalogo',
            loadComponent: () => import('./features/admin/documentos-catalogo/admin-catalogo-list/admin-catalogo-list.component'),
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: 'contratos' },
];
```

### 5.4 Modelos TypeScript

```typescript
// core/models/usuario.model.ts
export interface Usuario {
  id: number;
  email: string;
  nombre: string;
  cargo?: string;
  rol: 'CONTRATISTA' | 'REVISOR' | 'SUPERVISOR' | 'ADMIN';
  firmaImagen?: string;
  activo: boolean;
}

// core/models/contrato.model.ts
export type EstadoContrato = 'EN_EJECUCION' | 'LIQUIDADO' | 'CERRADO';

export interface ContratoResumen {
  id: number;
  numero: string;
  objeto: string;
  tipo: 'OPS';
  estado: EstadoContrato;
  fechaInicio: string;   // ISO date
  fechaFin: string;
  valorTotal: number;
  contratistaNombre: string;
  supervisorNombre?: string;
}

export interface ContratoDetalle extends ContratoResumen {
  contratista: Usuario;
  revisor?: Usuario;
  supervisor?: Usuario;
  obligaciones: Obligacion[];
  docsAplicables: DocumentoCatalogo[];
}

export interface ContratoRequest {
  numero: string;
  objeto: string;
  tipo: 'OPS';
  valorTotal: number;
  fechaInicio: string;
  fechaFin: string;
  idContratista: number;
  idRevisor?: number;
  idSupervisor?: number;
}

// core/models/obligacion.model.ts
export interface Obligacion {
  id: number;
  descripcion: string;
  orden: number;
}

// core/models/documento-catalogo.model.ts
export interface DocumentoCatalogo {
  id: number;
  nombre: string;
  descripcion?: string;
  obligatorio: boolean;
  tipoContrato: 'OPS';
}
```

### 5.5 Design Tokens — variables CSS (DESIGN.md → CSS)

```scss
// shared/design-tokens.scss
:root {
  // Paleta institucional SED (del DESIGN.md)
  --color-primary:           #002869;
  --color-primary-container: #0b3d91;
  --color-secondary:         #7e5700;
  --color-secondary-container: #feb300;
  --color-tertiary:          #5f001b;
  --color-surface:           #f8f9ff;
  --color-surface-container: #e5eeff;
  --color-on-surface:        #0b1c30;
  --color-on-surface-variant: #434652;
  --color-outline:           #747783;
  --color-error:             #ba1a1a;

  // Tipografía — Public Sans
  --font-family: 'Public Sans', 'Inter', sans-serif;
  --font-size-base: 16px;
  --font-size-sm:   14px;
  --font-size-xs:   13px;

  // Espaciado (4px base unit)
  --space-xs:  4px;
  --space-sm:  8px;
  --space-md:  16px;
  --space-lg:  24px;
  --space-xl:  40px;
  --space-gutter: 16px;
  --space-margin: 32px;

  // Radios
  --radius-sm:  2px;
  --radius-md:  4px;
  --radius-lg:  8px;
  --radius-full: 9999px;

  // Elevación (tonal layers sin sombras pesadas)
  --shadow-card:    none;
  --border-card:    1px solid #e2e8f0;
  --shadow-overlay: 0px 4px 12px rgba(0, 0, 0, 0.05);

  // Override PrimeNG con tokens SED
  --p-primary-color:          #0b3d91;
  --p-primary-contrast-color: #ffffff;
  --p-primary-hover-color:    #002869;
  --p-surface-card:           #ffffff;
  --p-surface-ground:         #f8f9ff;
}
```

### 5.6 Comportamiento de componentes clave del Incremento 1

#### `contratos-list.component.ts`
- Usa `p-table` con `[lazy]="true"` — paginación server-side
- Columnas: Número, Objeto, Estado (`status-chip`), Valor (COP), Fecha Fin, Acciones
- Filtro de búsqueda por número/objeto (debounce 300ms)
- Filtro por estado (dropdown)
- Click en fila → navega a `/contratos/:id`
- Muestra solo los contratos según rol del usuario autenticado (el backend filtra)

#### `contrato-detalle.component.ts`
- Ficha superior: datos del contrato + KPIs (valor, vigencia, días restantes)
- Sección Supervisor y Revisor asignados (card con nombre y cargo)
- Sección Obligaciones: lista numerada con descripción
- Sección Historial de Informes: tabla vacía en I1 (se llena en I2) con mensaje "Sin informes registrados"
- Botón "Nuevo Informe" visible solo para CONTRATISTA y solo si estado = `EN_EJECUCION` (deshabilitado en I1, habilitado en I2)

#### `admin-contrato-form.component.ts`
- Formulario reactivo (`ReactiveFormsModule`)
- Campos obligatorios validados: número (único), objeto, valor, fechas
- Dropdown con lista de contratistas (ROL=CONTRATISTA), revisores y supervisores (cargados desde `/api/usuarios?rol=X`)
- Sección de Obligaciones: lista dinámica con `FormArray` — agregar/eliminar/reordenar
- Botón Guardar → POST `/api/contratos` o PUT `/api/contratos/:id`

#### `perfil.component.ts`
- Muestra datos del usuario autenticado
- Campo editable: nombre y cargo
- Upload de imagen de firma: `p-fileUpload` con preview de imagen actual
- Validación: solo JPG/PNG, máximo 2MB
- Envío al endpoint `POST /api/usuarios/me/firma` (multipart)

---

## 6. Seguridad y Autenticación

### 6.1 Perfil `weblogic` — Azure AD JWT

`SecurityConfig.java` (idéntico al de docs/ARCHITECTURE.md) con los siguientes matchers:

```java
.authorizeHttpRequests(auth -> auth
    .antMatchers("/actuator/health").permitAll()
    .antMatchers("/swagger-ui.html", "/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
    // /api/usuarios/me y /api/usuarios/me/** (firma) → cualquier usuario autenticado
    .antMatchers("/api/usuarios/me", "/api/usuarios/me/**").authenticated()
    .antMatchers(HttpMethod.GET,  "/api/contratos/**").hasAnyRole("CONTRATISTA","REVISOR","SUPERVISOR","ADMIN")
    .antMatchers(HttpMethod.POST, "/api/contratos/**").hasRole("ADMIN")
    .antMatchers(HttpMethod.PUT,  "/api/contratos/**").hasRole("ADMIN")
    .antMatchers(HttpMethod.DELETE,"/api/contratos/**").hasRole("ADMIN")
    .antMatchers(HttpMethod.PATCH, "/api/contratos/**").hasRole("ADMIN")
    .antMatchers("/api/usuarios/**").hasRole("ADMIN")
    // catálogo: GET → cualquier rol autenticado; POST/PUT/DELETE → solo ADMIN
    .antMatchers(HttpMethod.GET, "/api/documentos-catalogo/**").authenticated()
    .antMatchers("/api/documentos-catalogo/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

### 6.2 Perfil `local-dev` — HTTP Basic

`DevSecurityConfig.java` con usuarios en memoria:

```java
User.withDefaultPasswordEncoder().username("admin")        .password("admin123")        .roles("ADMIN").build()
User.withDefaultPasswordEncoder().username("contratista1") .password("contratista123")  .roles("CONTRATISTA").build()
User.withDefaultPasswordEncoder().username("revisor1")     .password("revisor123")      .roles("REVISOR").build()
User.withDefaultPasswordEncoder().username("supervisor1")  .password("supervisor123")   .roles("SUPERVISOR").build()
```

### 6.3 Sincronización de usuario al login

Al recibir el primer request autenticado, el backend verifica si el email del JWT existe en `SGCN_USUARIOS`. Si no existe, crea el registro con los datos del token (`preferred_username`, `name`) y rol por defecto `CONTRATISTA`. El Admin puede cambiar el rol posteriormente desde el módulo de usuarios.

---

## 7. Pantallas de Referencia (Prototipo)

| Pantalla prototipo | Módulo | Ruta Angular |
|--------------------|--------|-------------|
| `login_institucional_sigcon` | M1 Auth | `/login` |
| `mi_perfil_y_firma_sigcon` | M1 Perfil | `/perfil` |
| `panel_del_contratista_sigcon` | M3 Lista contratos | `/contratos` |
| `detalle_del_contrato_sigcon` | M3 Detalle contrato | `/contratos/:id` |
| `dashboard_administrador_sigcon` | M2 Admin dashboard | `/admin` |
| `gesti_n_de_contratos_sigcon` | M2 Admin CRUD | `/admin/contratos` |

Todas las pantallas están en `Prototipo/<nombre>/screen.png` (visual) y `Prototipo/<nombre>/code.html` (implementación HTML de referencia). Los componentes Angular deben respetar el layout, jerarquía y densidad de información de estos prototipos.

---

## 8. Criterios de Aceptación

### Backend

- [ ] `GET /api/contratos` devuelve solo los contratos del contratista autenticado cuando el rol es CONTRATISTA
- [ ] `GET /api/contratos` devuelve todos los contratos cuando el rol es ADMIN
- [ ] `POST /api/contratos` falla con 403 si el usuario no es ADMIN
- [ ] `POST /api/contratos` falla con 409 si el número de contrato ya existe
- [ ] `GET /api/contratos/{id}` falla con 403 si un CONTRATISTA intenta acceder a un contrato que no es suyo
- [ ] `POST /api/usuarios/me/firma` acepta JPG y PNG, rechaza otros formatos con 400
- [ ] Todos los endpoints documentados en Swagger UI (accesible en `/sigcon/swagger-ui.html`)
- [ ] `GET /actuator/health` responde 200 sin autenticación
- [ ] La sincronización automática de usuario crea el registro en `SGCN_USUARIOS` en el primer login

### Frontend

- [ ] El login redirige correctamente según el rol: ADMIN → `/admin`, resto → `/contratos`
- [ ] Un CONTRATISTA no ve la sección "Admin" en el sidebar
- [ ] La tabla de contratos pagina correctamente (server-side) y el filtro de búsqueda funciona
- [ ] La ficha de contrato muestra todas las obligaciones en orden correcto
- [ ] El formulario de creación de contrato valida campos obligatorios antes de enviar
- [ ] La carga de imagen de firma muestra preview y persiste correctamente
- [ ] Los design tokens del DESIGN.md se aplican: fuente Public Sans, colores SED, tabla con zebra stripes
- [ ] Los componentes son consistentes con las pantallas del prototipo (`Prototipo/*/screen.png`)

### General

- [ ] El sistema funciona en perfil `local-dev` con los 4 usuarios de prueba
- [ ] El sistema funciona en perfil `weblogic` con Azure AD JWT (validado con tenant de prueba SED)
- [ ] El WAR `sigcon-backend.war` se despliega correctamente en WebLogic 12 sin errores
- [ ] No hay regresiones: todos los endpoints existentes pasan sus validaciones tras cada cambio

---

*Spec técnica generada mediante SDD Spec-Anchored · SIGCON · SED Bogotá · 2026-04-30*  
*Actualizar si cambian decisiones de diseño durante el Incremento 1.*
