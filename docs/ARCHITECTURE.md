# Arquitectura de Referencia — SIGCON / Ecosistema SED Bogotá

> Documento vivo adaptado para **SIGCON — Sistema de Gestion de Contratos**.  
> La base tecnica proviene de la arquitectura reutilizable del ecosistema SED.  
> Secretaría de Educación del Distrito — Bogotá, Colombia  
> Sirve como arquitectura tecnica rectora para SIGCON y como referencia reutilizable para proyectos compatibles.
>
> **Revisión 3 — 01/05/2026** — Adaptacion SIGCON: coordenadas canonicas, autoridad visual en `Prototipo/DESIGN.md`, limpieza de referencias heredadas de Portal Pagos.
> **Revisión 2 — 30/04/2026** — Ajustes por Líder de Desarrollo:  
> Angular 20 + PrimeNG 21 · Auth Office 365 (MSAL) · Swagger siempre activo · WAR para WebLogic 12 · JDK 8 · Docker/nginx/Keycloak omitidos

---

## Tabla de Contenido

0. [Adaptacion SIGCON](#0-adaptacion-sigcon)
1. [Visión General](#1-visión-general)
2. [Stack Tecnológico](#2-stack-tecnológico)
3. [Arquitectura por Capas](#3-arquitectura-por-capas)
4. [Backend — Spring Boot (WAR / WebLogic)](#4-backend--spring-boot-war--weblogic)
5. [Frontend — Angular 20 + PrimeNG 21](#5-frontend--angular-20--primeng-21)
6. [Base de Datos — Oracle](#6-base-de-datos--oracle)
7. [Seguridad y Autenticación — Office 365 / Azure AD](#7-seguridad-y-autenticación--office-365--azure-ad)
8. [Infraestructura — WebLogic 12](#8-infraestructura--weblogic-12)
9. [CI/CD](#9-cicd)
10. [Patrones de Diseño](#10-patrones-de-diseño)
11. [Sistema de Diseño — SIGCON](#11-sistema-de-diseño--sigcon)
12. [Convenciones y Estándares](#12-convenciones-y-estándares)
13. [Checklist para Nuevos Proyectos](#13-checklist-para-nuevos-proyectos)

---

## 0. Adaptacion SIGCON

Esta arquitectura se interpreta para SIGCON con las siguientes coordenadas canonicas. Si una plantilla o ejemplo generico dentro de este documento usa `[nombre-si]`, `[modulo]`, `PRTL_`, `SED_PORTAL`, `PortalPagos` o `/portal-pagos`, en SIGCON se reemplaza por estos valores:

| Coordenada | Valor SIGCON |
|------------|--------------|
| Sistema | `SIGCON` |
| Backend | `sigcon-backend` |
| WAR | `sigcon-backend.war` |
| Contexto WebLogic | `/sigcon` |
| Paquete Java base | `co.gov.bogota.sed.sigcon` |
| Frontend | `sigcon-angular` |
| Esquema Oracle MVP | `SED_SIGCON` |
| Prefijo de tablas | `SGCN_` |
| Perfil local | `local-dev` |
| Perfil servidor | `weblogic` |

La autoridad documental se mantiene en `docs/CONSTITUTION.md`. Para UX/UI, la fuente visual primaria es `Prototipo/DESIGN.md`; esta arquitectura define integracion tecnica con Angular 20, PrimeNG 21 y Tailwind, pero no reemplaza los tokens visuales aprobados.

---

## 1. Visión General

### Descripción del Ecosistema

Los proyectos del ecosistema SED son **aplicaciones web empresariales** orientadas a gestión y transparencia institucional. Comparten:

- Usuarios internos de la SED (funcionarios, contratistas, supervisores)
- Autenticación corporativa a través de **Office 365 / Azure Active Directory**
- Despliegue en infraestructura del Distrito (**Oracle WebLogic 12** + Oracle Database)
- Identidad visual institucional uniforme gobernada por `Prototipo/DESIGN.md`

### Diagrama de Contexto

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ECOSISTEMA SED BOGOTÁ                            │
│                                                                     │
│  ┌──────────────────┐   ┌──────────────────┐   ┌────────────────┐  │
│  │  SIGCON          │   │  [Otro Proyecto] │   │  [Otro SI]     │  │
│  │  (este repo)     │   │                  │   │                │  │
│  └────────┬─────────┘   └────────┬─────────┘   └───────┬────────┘  │
│           │                      │                      │           │
│           └──────────────────────┴──────────────────────┘           │
│                                  │                                  │
│                    ┌─────────────▼─────────────┐                   │
│                    │  Microsoft Azure AD         │                   │
│                    │  (Office 365 / MSAL)        │                   │
│                    │  Tenant SED Bogotá          │                   │
│                    └───────────────────────────┘                   │
│                                                                     │
│                    ┌───────────────────────────┐                   │
│                    │  Oracle Database 19c+      │                   │
│                    │  (Servidor Distrital Prod) │                   │
│                    └───────────────────────────┘                   │
│                                                                     │
│                    ┌───────────────────────────┐                   │
│                    │  Oracle WebLogic 12.2.1.4  │                   │
│                    │  (Servidor de Aplicaciones)│                   │
│                    └───────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────┘
```

### Flujo de Datos Simplificado

```
Usuario (navegador)
   │
   │ HTTPS
   ▼
[Angular SPA]  ──── OAuth2 PKCE ───▶  [Azure AD / Office 365]
   │                                   (access_token JWT)
   │ Authorization: Bearer <JWT>
   ▼
[Spring Boot API — WAR en WebLogic 12]
   │  valida JWT contra Azure AD JWKS
   │
   ▼
[Oracle Database 19c]
```

---

## 2. Stack Tecnológico

### Versiones Canónicas

| Capa | Tecnología | Versión | Notas |
|------|-----------|---------|-------|
| **Frontend** | Angular | 20.x | Standalone components, signals |
| | TypeScript | 5.8.x | Strict mode |
| | PrimeNG | 21.x | Componentes UI principales |
| | Tailwind CSS | 3.4.x | Utilidades layout/espaciado |
| | RxJS | 7.8.x | Observables |
| | Angular CDK | 20.x | Overlay, keyboard |
| | @azure/msal-angular | 3.x | SSO Office 365 |
| | @azure/msal-browser | 3.x | Cliente OIDC/OAuth2 Microsoft |
| | Node.js (build) | 20 LTS | Solo build/desarrollo |
| **Backend** | Java (JDK) | **8** (Oracle JDK 8) | Runtime obligatorio |
| | Spring Boot | 2.7.18 | Compatible con JDK 8 + WAR |
| | Spring Security | 5.7.x | Auth y CORS |
| | Spring Data JPA | 2.7.x | ORM |
| | Hibernate | 5.6.x | ORM provider |
| | SpringDoc OpenAPI | 1.7.0 | **Swagger UI (siempre activo)** |
| | Lombok | 1.18.x | Boilerplate reduction |
| | Oracle JDBC | 19.x (ojdbc8) | Driver Oracle compatible JDK 8 |
| | Maven | 3.9.x | Build system (`packaging: war`) |
| **Base de Datos** | Oracle Database | 19c+ | Producción distrital |
| **Servidor App** | Oracle WebLogic | **12.2.1.4.0** | Despliegue distrital (WAR) |
| **Identidad** | Microsoft Azure AD | Office 365 tenant | SSO corporativo SED |

### Restricción Crítica de JDK

> **El backend DEBE compilar y ejecutar sobre Oracle JDK 8.**  
> Spring Boot 2.7.x soporta Java 8 como versión mínima.  
> WebLogic 12.2.1.4.0 es compatible con JDK 8.  
> **No usar Java 11, 17 ni 21** hasta que WebLogic y el servidor distrital sean actualizados.

```xml
<!-- pom.xml -->
<properties>
    <java.version>8</java.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```

---

## 3. Arquitectura por Capas

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENTE                                   │
│              Angular 20 SPA (archivos estáticos)                 │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  features/ (lazy loaded)  │  shared/  │  core/             │  │
│  │  PrimeNG 21 Components    │ Tailwind  │ MSAL Auth          │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬───────────────────────────────────┘
                               │ HTTPS / Bearer JWT (Azure AD)
┌──────────────────────────────▼───────────────────────────────────┐
│                       API REST                                   │
│         Spring Boot 2.7.18 — desplegado como WAR                 │
│         Oracle WebLogic 12.2.1.4.0 — JDK 8                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ Web Layer    │  │ Application  │  │ Domain Layer         │   │
│  │ Controllers  │→ │ Services/DTOs│→ │ Entities/Repositories│   │
│  │ Exceptions   │  │ Mappers      │  │ Enums/Domain Logic   │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │ Config: SecurityConfig (Azure AD JWT) · AuditConfig      │    │
│  │         OpenApiConfig (Swagger siempre activo)           │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────────────────────────┬───────────────────────────────────┘
                               │ JDBC (ojdbc8 — Oracle 19c)
┌──────────────────────────────▼───────────────────────────────────┐
│                      PERSISTENCIA                                │
│                   Oracle Database 19c+                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ Tablas       │  │ Vistas       │  │ Sequences/Triggers   │   │
│  │ (prefijo     │  │ (V_*)        │  │ Auditoría automática  │   │
│  │  SGCN_)      │  │ Reporting    │  │ ON UPDATE            │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                    IDENTIDAD                                     │
│              Microsoft Azure AD (Office 365)                     │
│         Tenant SED Bogotá — OIDC / OAuth2 PKCE                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Backend — Spring Boot (WAR / WebLogic)

### Estructura de Paquetes

```
src/main/java/co/gov/bogota/sed/{nombre-si}/
├── domain/                        ← Capa de dominio
│   ├── entity/                    ← Entidades JPA (@Entity)
│   ├── enums/                     ← Enumeraciones del dominio
│   └── repository/                ← Interfaces Spring Data JPA
│
├── application/                   ← Lógica de negocio
│   ├── service/                   ← @Service + @Transactional
│   ├── dto/                       ← Request/Response POJOs
│   └── mapper/                    ← Mapping Entity ↔ DTO
│
├── web/                           ← Capa de presentación HTTP
│   ├── controller/                ← @RestController + @RequestMapping("/api")
│   └── exception/                 ← GlobalExceptionHandler (@RestControllerAdvice)
│
└── config/
    ├── SecurityConfig.java        ← OAuth2 ResourceServer Azure AD
    ├── AuditConfig.java           ← JPA Auditing (@EnableJpaAuditing)
    └── OpenApiConfig.java         ← Swagger UI (siempre activo)
```

### Empaquetado WAR para WebLogic 12

**`pom.xml` — configuración crítica:**

```xml
<project>
    <packaging>war</packaging>

    <properties>
        <java.version>8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Tomcat embebido marcado como PROVIDED (WebLogic provee el contenedor) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Spring Boot starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <!-- Oracle JDBC (compatible JDK 8) -->
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
            <version>19.18.0.0</version>
        </dependency>

        <!-- Swagger / OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>1.7.0</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
        <!-- Nombre del WAR para despliegue en WebLogic -->
        <finalName>${project.artifactId}</finalName>
    </build>
</project>
```

**Clase de arranque para WAR:**

```java
// SigconBackendApplication.java
@SpringBootApplication
public class SigconBackendApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // Requerido para despliegue en contenedor externo (WebLogic)
        return builder.sources(SigconBackendApplication.class);
    }

    public static void main(String[] args) {
        // Permite ejecución directa con mvn spring-boot:run en desarrollo
        SpringApplication.run(SigconBackendApplication.class, args);
    }
}
```

**Descriptor WebLogic (`src/main/webapp/WEB-INF/weblogic.xml`):**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<weblogic-web-app
    xmlns="http://xmlns.oracle.com/weblogic/weblogic-web-app"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.oracle.com/weblogic/weblogic-web-app
        http://xmlns.oracle.com/weblogic/weblogic-web-app/1.9/weblogic-web-app.xsd">

    <weblogic-version>12.2.1</weblogic-version>

    <container-descriptor>
        <!-- Forzar uso de librerías Spring empaquetadas en el WAR,
             no las del servidor WebLogic (evita conflictos de versión) -->
        <prefer-web-inf-classes>true</prefer-web-inf-classes>
    </container-descriptor>

    <context-root>/sigcon</context-root>
</weblogic-web-app>
```

### Swagger UI — Siempre Activo

```java
// config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SIGCON SED — API")
                .description("API REST del sistema de información")
                .version("v1.0")
                .contact(new Contact()
                    .name("SED Bogotá — Oficina de Sistemas")
                    .email("sistemas@educacionbogota.edu.co")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtenido de Office 365 (Azure AD)")));
    }
}
```

```yaml
# application.yml — Swagger disponible siempre (no solo en local-dev)
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    display-request-duration: true
  show-actuator: false
```

Acceso: `https://<servidor>/[contexto]/swagger-ui.html`

### Perfiles Spring

| Perfil | Uso | Auth | BD |
|--------|-----|------|----|
| `local-dev` | Estación de desarrollo | HTTP Basic (hardcoded) | Oracle local / TNS |
| `weblogic` | Servidor distrital SED | OAuth2 JWT Azure AD | Oracle 19c Distrital |

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILE:local-dev}

---
spring:
  config:
    activate:
      on-profile: weblogic
  datasource:
    url: ${DB_URL}                     # jdbc:oracle:thin:@//host:1521/service
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

---
spring:
  config:
    activate:
      on-profile: local-dev
  datasource:
    url: jdbc:oracle:thin:@localhost:1521/XEPDB1
    username: ${DB_USERNAME:SED_SIGCON}
    password: ${DB_PASSWORD:Sigcon_2026#}
  jpa:
    database-platform: org.hibernate.dialect.Oracle12cDialect
    hibernate:
      ddl-auto: validate
  # DevSecurityConfig activo via @Profile("local-dev")
```

### Convenciones Backend

**Entidades JPA**
```java
@Entity
@Table(name = "SGCN_ENTIDAD")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Entidad {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "SGCN_ENTIDAD_SEQ")
    @SequenceGenerator(name = "SGCN_ENTIDAD_SEQ",
                       sequenceName = "SGCN_ENTIDAD_SEQ", allocationSize = 1)
    private Long id;

    @CreatedDate
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "CREATED_BY", updatable = false)
    private String createdBy;             // email corporativo Office 365

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

**Controllers REST**
```java
@RestController
@RequestMapping("/api/entidades")
@RequiredArgsConstructor
@Tag(name = "Entidades", description = "Gestión de entidades")   // Swagger tag
public class EntidadController {

    private final EntidadService service;

    @Operation(summary = "Listar entidades paginadas")
    @GetMapping
    public ResponseEntity<Page<EntidadDto>> listar(Pageable pageable) { ... }

    @Operation(summary = "Obtener detalle")
    @GetMapping("/{id}")
    public ResponseEntity<EntidadDto> detalle(@PathVariable Long id) { ... }

    @Operation(summary = "Crear entidad")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntidadDto crear(@Valid @RequestBody EntidadRequest req) { ... }
}
```

**Manejo de Excepciones Global**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        return ErrorResponse.of(ex.getMessage());
    }
    // Constraint violations → 400, excepciones de negocio → 422
}
```

### Endpoints Estándar

```
GET  /actuator/health       ← Health check (balanceador / WebLogic)
GET  /api-docs              ← OpenAPI JSON (siempre activo)
GET  /swagger-ui.html       ← Swagger UI (siempre activo)
```

---

## 5. Frontend — Angular 20 + PrimeNG 21

### Estructura de Directorios

```
src/app/
├── core/                          ← Servicios singleton (providedIn: 'root')
│   ├── auth/
│   │   ├── auth.service.ts        ← Facade: MSAL + DevSession
│   │   ├── auth.guard.ts          ← canActivate: requiere login (MsalGuard)
│   │   ├── role.guard.ts          ← canActivate: requiere rol específico
│   │   ├── msal.interceptor.ts    ← Inyecta access_token Azure AD
│   │   └── dev-session.service.ts ← HTTP Basic mock (local-dev)
│   ├── models/                    ← Interfaces TypeScript del dominio
│   └── services/                  ← HttpClient services → API
│
├── shared/                        ← Componentes DUMB reutilizables
│   ├── components/
│   │   ├── sidebar/               ← Menú de navegación persistente
│   │   ├── topbar/                ← Barra superior (usuario O365, logout)
│   │   ├── kpi-card/              ← Tarjeta de indicador (KPI)
│   │   └── status-chip/           ← Badge de estado (p-tag de PrimeNG)
│   ├── design-tokens.scss         ← Variables CSS (colores, tipografía SED)
│   └── app-shell.component.ts     ← Layout root: sidebar + topbar + router-outlet
│
├── features/                      ← Módulos de funcionalidad (lazy loaded)
│   ├── auth/
│   │   └── login/                 ← Pantalla de login (MSAL redirect / dev demo)
│   ├── dashboard/                 ← KPIs, métricas, alertas
│   ├── [entidad-principal]/       ← List + Detail + Form (PrimeNG p-table, p-dialog)
│   └── [otras-features]/
│
├── app.component.ts               ← Root component (standalone)
├── app.config.ts                  ← Providers globales (MSAL + PrimeNG)
├── app.routes.ts                  ← Rutas con lazy loading
└── main.ts                        ← bootstrapApplication()
```

### Configuración Standalone (Angular 20)

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import {
  providePublicClientApplication,
  InteractionType,
  IPublicClientApplication,
  PublicClientApplication,
} from '@azure/msal-browser';
import { provideMsal, MsalInterceptor } from '@azure/msal-angular';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { APP_ROUTES } from './app.routes';
import { environment } from '../environments/environment';

export function msalInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication(environment.msalConfig);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(APP_ROUTES, withComponentInputBinding()),
    provideHttpClient(withInterceptors([])),
    provideAnimationsAsync(),

    // PrimeNG 21 — tema Aura con tokens SED
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          prefix: 'p',
          darkModeSelector: '.dark-mode',
          cssLayer: false,
        },
      },
      ripple: true,
    }),

    // MSAL Office 365
    providePublicClientApplication(msalInstanceFactory()),
    provideMsal({
      interactionType: InteractionType.Redirect,
      authRequest: { scopes: environment.apiScopes },
    }),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true,
    },
  ],
};
```

### Rutas con Lazy Loading

```typescript
// app.routes.ts
import { MsalGuard } from '@azure/msal-angular';

export const APP_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component'),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [MsalGuard],              // Guard nativo MSAL
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component'),
      },
      {
        path: 'entidades',
        loadChildren: () => import('./features/entidades/entidades.routes'),
        // canActivate: [roleGuard('ROLE_APROBADOR')] para rutas protegidas
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
```

### Componentes con PrimeNG 21

```typescript
// features/contratos/contratos-list.component.ts
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';

@Component({
  selector: 'app-contratos-list',
  standalone: true,
  imports: [TableModule, ButtonModule, TagModule, InputTextModule, DialogModule],
  template: `
    <p-table
      [value]="contratos()"
      [paginator]="true"
      [rows]="10"
      [rowsPerPageOptions]="[10, 25, 50]"
      [sortMode]="'multiple'"
      [loading]="loading()"
      styleClass="p-datatable-striped p-datatable-sm">

      <ng-template pTemplate="header">
        <tr>
          <th pSortableColumn="objeto">Objeto <p-sortIcon field="objeto"/></th>
          <th pSortableColumn="tipo">Tipo <p-sortIcon field="tipo"/></th>
          <th pSortableColumn="faseActual">Fase <p-sortIcon field="faseActual"/></th>
          <th pSortableColumn="valorTotal">Valor <p-sortIcon field="valorTotal"/></th>
          <th>Acciones</th>
        </tr>
      </ng-template>

      <ng-template pTemplate="body" let-contrato>
        <tr>
          <td>{{ contrato.objeto }}</td>
          <td>{{ contrato.tipo }}</td>
          <td>
            <p-tag
              [value]="contrato.faseActual"
              [severity]="getFaseSeverity(contrato.faseActual)"/>
          </td>
          <td>{{ contrato.valorTotal | currency:'COP' }}</td>
          <td>
            <p-button icon="pi pi-eye" [text]="true" (onClick)="verDetalle(contrato)"/>
          </td>
        </tr>
      </ng-template>
    </p-table>
  `,
})
export class ContratosListComponent {
  contratos = signal<ContratoDto[]>([]);
  loading = signal(false);
  // ...
}
```

### Servicios HTTP

```typescript
// core/services/entidad.service.ts
@Injectable({ providedIn: 'root' })
export class EntidadService {
  private readonly apiUrl = `${environment.apiUrl}/entidades`;

  constructor(private http: HttpClient) {}

  listar(params?: HttpParams): Observable<Page<EntidadDto>> {
    return this.http.get<Page<EntidadDto>>(this.apiUrl, { params });
  }

  crear(req: EntidadRequest): Observable<EntidadDto> {
    return this.http.post<EntidadDto>(this.apiUrl, req);
  }
}
```

### Entornos

```typescript
// environments/environment.ts (producción WebLogic)
import { Configuration, BrowserCacheLocation, LogLevel } from '@azure/msal-browser';

export const environment = {
  production: true,
  apiUrl: '/sigcon/api',                 // Contexto WebLogic + ruta API

  msalConfig: {
    auth: {
      clientId: '[AZURE_CLIENT_ID]',
      authority: 'https://login.microsoftonline.com/[AZURE_TENANT_ID]',
      redirectUri: 'https://[servidor-sed]/sigcon',
    },
    cache: {
      cacheLocation: BrowserCacheLocation.SessionStorage,
      storeAuthStateInCookie: false,
    },
  } as Configuration,

  apiScopes: ['api://[AZURE_CLIENT_ID]/access_as_user'],
};

// environments/environment.local-dev.ts
export const environment = {
  production: false,
  apiUrl: '/api',                        // Proxy → Spring Boot :8080

  // En local-dev, MSAL se deshabilita y se usa HTTP Basic (dev-session)
  msalConfig: null,
  apiScopes: [],
  useDevSession: true,
};
```

### Proxy de Desarrollo (local-dev)

```json
// proxy.conf.json — solo para ng serve en local
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

```json
// angular.json — referenciar proxy solo en configuración local
"serve": {
  "configurations": {
    "local-dev": {
      "proxyConfig": "proxy.conf.json"
    }
  }
}
```

---

## 6. Base de Datos — Oracle

### Convenciones de Nomenclatura

| Objeto | Patrón | Ejemplo |
|--------|--------|---------|
| Esquema | `SED_[MODULO]` | `SED_SIGCON` |
| Tablas | `[PREFIJO]_[ENTIDAD]` | `SGCN_CONTRATOS` |
| Sequences | `[TABLA]_SEQ` | `SGCN_CONTRATOS_SEQ` |
| Triggers | `TRG_[TABLA]_AUDIT` | `TRG_CONTRATOS_AUDIT` |
| Vistas | `V_[NOMBRE]` | `V_CONTRATOS_RESUMEN` |
| Índices | `IDX_[TABLA]_[CAMPO]` | `IDX_CONTRATOS_FASE` |

### Template DDL

```sql
-- 1. Sequences
CREATE SEQUENCE SGCN_ENTIDAD_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

-- 2. Tabla principal
CREATE TABLE SGCN_ENTIDAD (
    ID            NUMBER        DEFAULT SGCN_ENTIDAD_SEQ.NEXTVAL PRIMARY KEY,
    -- campos de negocio...
    ACTIVO        NUMBER(1)     DEFAULT 1 NOT NULL,
    CREATED_AT    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY    VARCHAR2(200),          -- email Office 365 (UPN)
    UPDATED_AT    TIMESTAMP
);

-- 3. Índices funcionales
CREATE INDEX IDX_ENTIDAD_CAMPO ON SGCN_ENTIDAD(CAMPO_BUSQUEDA);

-- 4. Trigger de auditoría
CREATE OR REPLACE TRIGGER TRG_ENTIDAD_AUDIT
BEFORE UPDATE ON SGCN_ENTIDAD
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := SYSTIMESTAMP;
END;
/

-- 5. Vistas desnormalizadas (reportes)
CREATE OR REPLACE VIEW V_ENTIDAD_RESUMEN AS
SELECT e.*, r.nombre AS referencia_nombre
FROM SGCN_ENTIDAD e
LEFT JOIN SGCN_REFERENCIA r ON r.id = e.referencia_id;
```

### Archivos de Base de Datos

```
db/
├── 00_setup.sql            ← DDL: usuario, tablas, sequences, triggers, vistas
└── 01_datos_prueba.sql     ← DML: datos de prueba para desarrollo local
```

> **Nota**: Los scripts de inicialización los ejecuta DBA en el servidor Oracle distrital para producción. En desarrollo local el DBA local (o el desarrollador) ejecuta los scripts manualmente.

---

## 7. Seguridad y Autenticación — Office 365 / Azure AD

### Arquitectura de Autenticación

```
┌─────────────────────────────────────────────────────────────────────┐
│                   ESTRATEGIA DE AUTENTICACIÓN                       │
├──────────────────────────────┬──────────────────────────────────────┤
│       local-dev              │       producción (WebLogic)           │
├──────────────────────────────┼──────────────────────────────────────┤
│ Backend:                     │ Backend:                              │
│ DevSecurityConfig            │ SecurityConfig                        │
│ @Profile("local-dev")        │ @Profile("!local-dev")               │
│ └─ HttpBasic                 │ └─ OAuth2ResourceServer               │
│    └─ InMemoryUsers          │    └─ JWT Azure AD                   │
│                              │       (JWKS de Microsoft)             │
│ Frontend:                    │ Frontend:                              │
│ DevSessionService            │ MsalService (MSAL Angular 3.x)        │
│ └─ Base64(user:pass)         │ └─ PKCE Flow → Azure AD               │
│    localStorage              │    access_token JWT                   │
│                              │                                       │
│ Interceptor HTTP → Authorization: Basic / Bearer (ambos entornos)  │
└─────────────────────────────────────────────────────────────────────┘
```

### SecurityConfig — Producción (Azure AD)

```java
@Configuration
@EnableWebSecurity
@Profile("!local-dev")
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigSource()))
            .authorizeHttpRequests(auth -> auth
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/swagger-ui.html", "/api-docs/**",
                             "/swagger-ui/**", "/webjars/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**").hasRole("ADMIN_BASICO")
                .antMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN_BASICO")
                .antMatchers("/api/*/aprobar/**").hasRole("APROBADOR")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(azureRolesConverter()))
            )
            .build();
    }

    /**
     * Extrae roles desde el claim "roles" del JWT de Azure AD.
     * Los roles se definen en el App Registration de Azure AD → App roles.
     */
    @Bean
    public JwtAuthenticationConverter azureRolesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");        // claim de Azure AD
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        // El UPN (email O365) queda en jwt.getClaimAsString("preferred_username")
        jwtConverter.setPrincipalClaimName("preferred_username");
        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://*.educacionbogota.edu.co"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### DevSecurityConfig — Desarrollo Local

```java
@Configuration
@EnableWebSecurity
@Profile("local-dev")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
            User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123")
                .roles("ADMIN_BASICO")
                .build(),
            User.withDefaultPasswordEncoder()
                .username("aprobador")
                .password("aprobador123")
                .roles("ADMIN_BASICO", "APROBADOR")
                .build()
        );
    }
}
```

### Configuración Azure AD — App Registration

```
Tenant: [AZURE_TENANT_ID]  (proporcionado por TI SED)
Client ID: [AZURE_CLIENT_ID]  (por proyecto/SI)

App Roles definidos en Azure Portal:
  ├─ ADMIN_BASICO   → asignado a grupos de usuarios SED
  └─ APROBADOR      → asignado a supervisores/aprobadores

Redirect URIs:
  ├─ https://[servidor]/sigcon                (producción)
  └─ http://localhost:4200                    (desarrollo)

Token claim mapping:
  ├─ preferred_username → email corporativo (@educacionbogota.edu.co)
  └─ roles              → App Roles asignados
```

### Roles RBAC Estándar

| Rol Azure AD | Permisos |
|-------------|----------|
| `ADMIN_BASICO` | Consultar, crear registros, descargar reportes |
| `APROBADOR` | Todo lo anterior + aprobar transiciones, validar documentos |

Ampliar con roles adicionales según dominio del proyecto (ej: `SUPERVISOR`, `AUDITOR`).

### Auditoría con Usuario O365

El `CREATED_BY` registrado en BD será el `preferred_username` del JWT (email corporativo):

```java
// config/AuditConfig.java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName)          // preferred_username de Azure AD
            .filter(name -> !name.equals("anonymousUser"));
    }
}
```

### Guards Angular

```typescript
// Usar MsalGuard nativo de MSAL Angular (reemplaza authGuard propio)
import { MsalGuard } from '@azure/msal-angular';

// role.guard.ts — sigue siendo propio para control RBAC granular
export const roleGuard = (role: string): CanActivateFn => () => {
  const auth = inject(AuthService);
  return auth.hasRole(role) || inject(Router).createUrlTree(['/sin-permiso']);
};

// auth.service.ts — facade que lee roles del token MSAL
@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private msalService: MsalService) {}

  get currentUser(): AccountInfo | null {
    return this.msalService.instance.getActiveAccount();
  }

  hasRole(role: string): boolean {
    const claims = this.currentUser?.idTokenClaims as any;
    return (claims?.roles ?? []).includes(role);
  }

  getUserEmail(): string {
    return this.currentUser?.username ?? '';
  }
}
```

### Credenciales Demo (local-dev)

```
Usuario:    admin       / admin123       → ROLE_ADMIN_BASICO
Usuario:    aprobador   / aprobador123   → ROLE_ADMIN_BASICO, ROLE_APROBADOR
```

---

## 8. Infraestructura — WebLogic 12

> **Docker, nginx y Keycloak están omitidos de esta arquitectura.**  
> El despliegue es directo sobre la infraestructura distrital existente.

### Modelo de Despliegue

```
┌─────────────────────────────────────────────────────────┐
│              INFRAESTRUCTURA DISTRITAL SED               │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │         Oracle WebLogic 12.2.1.4.0              │    │
│  │                                                 │    │
│  │  ┌───────────────────┐  ┌───────────────────┐   │    │
│  │  │ sigcon-backend.war│  │  [otro-si].war    │   │    │
│  │  │  Spring Boot API  │  │                   │   │    │
│  │  │  JDK 8            │  │                   │   │    │
│  │  └───────────────────┘  └───────────────────┘   │    │
│  └─────────────────────────────────────────────────┘    │
│                         │                               │
│  ┌──────────────────────▼──────────────────────────┐    │
│  │         Oracle Database 19c+                    │    │
│  │         Servidor Distrital                      │    │
│  └─────────────────────────────────────────────────┘    │
│                                                         │
│  Estáticos Angular → servidos desde WebLogic o IIS      │
│  (coordinar con infraestructura SED)                    │
└─────────────────────────────────────────────────────────┘
```

### Proceso de Despliegue WAR

```
Desarrollador
    │
    │  mvn clean package -P weblogic
    │  → genera: target/sigcon-backend.war
    │
    ▼
Control de versiones / repositorio de artefactos
    │
    ▼
DBA / DevOps SED
    │  1. Copia WAR a servidor WebLogic
    │  2. Despliega desde consola WebLogic admin
    │     (http://[servidor]:7001/console)
    │  3. Activa despliegue
    ▼
WebLogic 12.2.1.4.0 — contexto: /sigcon
```

### Variables de Configuración (properties WebLogic)

En el servidor WebLogic configurar las siguientes JNDI properties o variables de sistema:

```properties
# Fuente de datos Oracle (configurar JNDI en WebLogic o via application.yml)
DB_URL=jdbc:oracle:thin:@//[host-oracle]:1521/[service-name]
DB_USERNAME=SED_SIGCON
DB_PASSWORD=[contraseña-segura]

# Perfil Spring activo
SPRING_PROFILE=weblogic

# Azure AD
AZURE_TENANT_ID=[tenant-id-sed]
AZURE_CLIENT_ID=[client-id-app-registration]
```

### Estructura de Archivos del Proyecto (sin Docker)

```
ProyectoContratosSED/
├── docs/
│   ├── CONSTITUTION.md
│   ├── ARCHITECTURE.md
│   ├── TECNOLOGIAS.md
│   ├── ARRANQUE.md
│   ├── specs/
│   └── plans/
├── Prototipo/
├── sigcon-backend/
│   ├── pom.xml                       ← packaging: war, java.version: 8
│   ├── src/main/
│   │   ├── java/...                  ← Código fuente
│   │   ├── resources/
│   │   │   └── application.yml       ← Perfiles local-dev / weblogic
│   │   └── webapp/WEB-INF/
│   │       └── weblogic.xml          ← Descriptor WebLogic
│   └── src/test/...
├── sigcon-angular/
│   ├── package.json                  ← Angular 20, PrimeNG 21, MSAL
│   ├── angular.json
│   ├── src/
│   │   └── environments/
│   │       ├── environment.ts        ← Azure AD production config
│   │       └── environment.local-dev.ts
│   └── proxy.conf.json               ← Solo desarrollo local
└── db/
    ├── 00_setup.sql
    └── 01_datos_prueba.sql
```

---

## 9. CI/CD

### Flujo de Integración Continua

```
Push a main/develop
        │
        ▼
┌────────────────────────┐
│  CI — ci.yml           │
│  ──────────────────    │
│  1. Checkout           │
│  2. Java 8 (Oracle JDK)│
│     mvn test + package │
│     → sigcon-backend.war │
│  3. Node 20 LTS        │
│     npm ci + ng lint   │
│     ng build --prod    │
│  4. Artefactos:        │
│     ├─ *.war           │
│     └─ dist/ (statics) │
└────────────────────────┘
```

### ci.yml Template

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Oracle JDK 8
        uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'oracle'            # Oracle JDK 8
      - name: Build WAR
        run: mvn -B clean package -DskipTests
        working-directory: ./${{ env.BACKEND_DIR }}
      - name: Run tests
        run: mvn -B test
        working-directory: ./${{ env.BACKEND_DIR }}
      - name: Upload WAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: backend-war
          path: ./${{ env.BACKEND_DIR }}/target/*.war

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: ./${{ env.FRONTEND_DIR }}/package-lock.json
      - name: Install and build
        run: |
          npm ci
          npx ng lint
          npx ng build --configuration production
        working-directory: ./${{ env.FRONTEND_DIR }}
      - name: Upload static files
        uses: actions/upload-artifact@v4
        with:
          name: frontend-dist
          path: ./${{ env.FRONTEND_DIR }}/dist/
```

---

## 10. Patrones de Diseño

### Resumen de Patrones Aplicados

| Patrón | Capa | Implementación |
|--------|------|----------------|
| **Layered Architecture** | Backend | `web/` → `application/` → `domain/` |
| **Repository Pattern** | Domain | Interfaces Spring Data JPA |
| **DTO Pattern** | Application | Separar entidades JPA de contratos API |
| **Service Layer** | Application | `@Service` + `@Transactional` |
| **State Machine** | Application | `Service.avanzarFase()` con validaciones |
| **Strategy Pattern** | Config | `DevSecurityConfig` vs `SecurityConfig` |
| **Builder** | Domain | `@Builder` Lombok en entidades |
| **Facade** | Frontend | `AuthService` unifica MSAL + DevSession |
| **Guard Pattern** | Frontend | `MsalGuard`, `roleGuard` |
| **Interceptor Pattern** | Frontend | `MsalInterceptor` (token Azure AD) |
| **Smart/Dumb Components** | Frontend | Features (SMART) vs Shared (DUMB) |
| **Lazy Loading** | Frontend | `loadComponent()` / `loadChildren()` |
| **Observer** | Frontend | RxJS Observables + Angular Signals |

### State Machine para Workflows

```java
public enum FaseContrato {
    PLANEACION, SELECCION, CONTRATACION, EJECUCION, LIQUIDACION
}

private static final Map<FaseContrato, List<FaseContrato>> TRANSICIONES = Map.of(
    PLANEACION,    List.of(SELECCION),
    SELECCION,     List.of(CONTRATACION),
    CONTRATACION,  List.of(EJECUCION),
    EJECUCION,     List.of(LIQUIDACION)
);

public void avanzarFase(Long id) {
    Contrato c = repo.findById(id).orElseThrow();
    FaseContrato siguiente = TRANSICIONES.get(c.getFaseActual())
        .stream().findFirst()
        .orElseThrow(() -> new EstadoInvalidoException("Sin siguiente fase"));
    c.setFaseActual(siguiente);
    repo.save(c);
}
```

---

## 11. Sistema de Diseño — SIGCON

### Autoridad Visual

`Prototipo/DESIGN.md` gobierna la identidad visual de SIGCON: paleta, tipografia, radios, densidad, espaciado y comportamiento visual de componentes. Esta seccion solo traduce esa referencia a decisiones tecnicas de Angular 20 + PrimeNG 21 + Tailwind CSS.

Reglas canonicas para SIGCON:

- Tipografia principal: `Public Sans`.
- Radio base para botones, inputs, cards y paneles: `4px`.
- Interfaz administrativa compacta, con formularios de alta densidad.
- Cards y paneles con borde neutral de `1px`; no usar sombras pesadas como estilo base.
- Tablas con zebra stripes y densidad operativa.
- PrimeNG 21 es la libreria primaria de componentes; los tokens SED se aplican por CSS variables.

```scss
// design-tokens.scss
:root {
  // Colores institucionales desde Prototipo/DESIGN.md
  --color-primary:    #0b3d91;
  --color-secondary:  #ffb300;
  --color-tertiary:   #92032e;
  --color-surface:    #f8f9ff;
  --color-on-surface: #0b1c30;

  // Tipografía
  --font-family: 'Public Sans', 'Inter', sans-serif;
  --font-size-base: 14px;

  // Forma y densidad
  --radius-card: 4px;
  --radius-control: 4px;
  --radius-chip: 8px;
  --spacing-unit: 4px;
  --panel-border: 1px solid #e2e8f0;
}
```

### Tema PrimeNG 21 — Tokens SED

```typescript
// Extensión del preset Aura con colores SED (app.config.ts)
providePrimeNG({
  theme: {
    preset: Aura,
    options: { prefix: 'p', darkModeSelector: '.dark-mode' },
  },
  translation: {            // Español colombiano
    accept: 'Aceptar',
    reject: 'Cancelar',
    // ... resto de traducciones
  },
})
```

```scss
// styles.scss — sobreescritura de variables PrimeNG con paleta SED
:root {
  --p-primary-color: #0b3d91;
  --p-primary-contrast-color: #ffffff;
  --p-primary-hover-color: #002869;
  --p-surface-card: #ffffff;
  --p-surface-ground: #f8f9ff;
}
```

### Uso de PrimeNG 21 por caso

| Caso de uso | Componente PrimeNG | Import |
|-------------|-------------------|--------|
| Tablas con filtros y paginación | `p-table` | `TableModule` |
| Formularios de captura | `p-floatLabel`, `p-inputText`, `p-dropdown` | `InputTextModule`, `DropdownModule` |
| Modales / confirmaciones | `p-dialog`, `p-confirmDialog` | `DialogModule` |
| Mensajes y alertas | `p-toast`, `p-message` | `ToastModule`, `MessagesModule` |
| Badges de estado | `p-tag` | `TagModule` |
| Botones de acción | `p-button` | `ButtonModule` |
| Menú lateral | `p-panelMenu`, `p-sidebar` | `PanelMenuModule` |
| Carga de archivos | `p-fileUpload` | `FileUploadModule` |
| Selección de fechas | `p-calendar` | `CalendarModule` |
| KPIs / métricas | Componente propio + `p-card` | `CardModule` |

### Componentes Shared Reutilizables

| Componente | Selector | Basado en |
|-----------|----------|-----------|
| `KpiCardComponent` | `app-kpi-card` | `p-card` + estilos SED |
| `StatusChipComponent` | `app-status-chip` | `p-tag` con severidad semántica |
| `SidebarComponent` | `app-sidebar` | `p-panelMenu` + tokens SED |
| `TopbarComponent` | `app-topbar` | Propio + avatar usuario O365 |
| `AppShellComponent` | `app-shell` | Layout root: sidebar + topbar + outlet |

### Principios de UI

- **Densidad operativa**: formularios, tablas y paneles priorizan lectura rapida y gestion repetitiva.
- **Bordes sutiles**: cards y paneles usan borde neutral; sombras solo para overlays, dropdowns y tooltips.
- **Zebra stripes**: `p-datatable-striped` en tablas operativas.
- **Status chips**: `p-tag` con severities (`success`, `warn`, `danger`, `info`) y mapeo semantico definido por la spec activa.
- **Sin efectos decorativos dominantes**: no usar glassmorphism como estilo base de pantallas administrativas.

---

## 12. Convenciones y Estándares

### Nomenclatura

| Ámbito | Convención | Ejemplo |
|--------|-----------|---------|
| Paquetes Java | `co.gov.bogota.sed.[modulo]` | `co.gov.bogota.sed.sigcon` |
| Clases Controller | `[Entidad]Controller` | `ContratoController` |
| Clases Service | `[Entidad]Service` | `ContratoService` |
| Clases Entity | `[Entidad]` (singular) | `Contrato` |
| Clases DTO | `[Entidad]Dto`, `[Entidad]Request` | `ContratoDto`, `ContratoRequest` |
| Tablas Oracle | `[PREFIJO]_[ENTIDAD]` (mayúscula) | `SGCN_CONTRATOS` |
| Archivos Angular | `[entidad].[tipo].ts` | `contrato.service.ts` |
| Componentes Angular | `[nombre].component.ts` | `contratos-list.component.ts` |
| Rutas API | `/api/[entidades]` (plural, kebab-case) | `/api/contratos-activos` |
| WAR desplegado | `[sistema]-backend.war` | `sigcon-backend.war` |
| Contexto WebLogic | `/[sistema]` | `/sigcon` |

### Estructura de Proyecto

```
ProyectoContratosSED/
├── docs/
│   ├── CONSTITUTION.md            ← Constitucion SDD
│   ├── ARCHITECTURE.md            ← Este documento (adaptado)
│   ├── TECNOLOGIAS.md             ← Versiones exactas del stack
│   ├── ARRANQUE.md                ← Guia de inicio rapido para devs
│   ├── specs/                     ← PRD y specs tecnicas
│   └── plans/                     ← Planes y outlines de implementacion
├── Prototipo/                ← Design system y pantallas de referencia
├── sigcon-backend/           ← Spring Boot API (WAR, JDK 8)
├── sigcon-angular/           ← Angular 20 SPA + PrimeNG 21
└── db/                       ← Scripts SQL Oracle
```

### API REST Convenciones

```
GET    /api/{entidades}                  ← Listar (paginado)
GET    /api/{entidades}/{id}             ← Detalle
POST   /api/{entidades}                 ← Crear
PUT    /api/{entidades}/{id}             ← Reemplazar
PATCH  /api/{entidades}/{id}/{accion}   ← Acción de negocio
DELETE /api/{entidades}/{id}            ← Eliminar (lógico, activo=0)

# Respuestas de error estandarizadas
{ "error": "ENTIDAD_NO_ENCONTRADA", "mensaje": "...", "timestamp": "..." }
```

### Auditoría

Toda tabla principal debe incluir:

```sql
CREATED_AT    TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
CREATED_BY    VARCHAR2(200),           -- email O365 (preferred_username JWT)
UPDATED_AT    TIMESTAMP,              -- trigger automático ON UPDATE
ACTIVO        NUMBER(1)     DEFAULT 1 NOT NULL   -- borrado lógico
```

En JPA: `@EnableJpaAuditing` + `@EntityListeners(AuditingEntityListener.class)`.

---

## 13. Checklist para Nuevos Proyectos

### Setup Inicial
- [ ] Crear repositorio con estructura estándar de carpetas
- [ ] Copiar y adaptar `docs/ARCHITECTURE.md`, `docs/ARRANQUE.md`, `docs/TECNOLOGIAS.md`
- [ ] Definir prefijo de módulo para tablas Oracle (ej: `CTRL_`, `RRHH_`, `ACAD_`)
- [ ] Definir nombre del WAR y contexto WebLogic (`sigcon-backend.war`, `/sigcon`)
- [ ] Registrar la aplicación en Azure AD (App Registration en tenant SED)
- [ ] Obtener `AZURE_TENANT_ID` y `AZURE_CLIENT_ID` de TI SED
- [ ] Definir App Roles en Azure AD (`ADMIN_BASICO`, `APROBADOR`, etc.)

### Backend
- [ ] Inicializar proyecto Spring Boot 2.7.x con **Oracle JDK 8** (Maven)
- [ ] Configurar `<packaging>war</packaging>` y `<java.version>8</java.version>`
- [ ] Marcar `spring-boot-starter-tomcat` como `<scope>provided</scope>`
- [ ] Extender `SpringBootServletInitializer` en la clase principal
- [ ] Crear `src/main/webapp/WEB-INF/weblogic.xml` con `prefer-web-inf-classes: true`
- [ ] Configurar paquete base: `co.gov.bogota.sed.[modulo]`
- [ ] Crear estructura de capas: `domain/`, `application/`, `web/`, `config/`
- [ ] Configurar `SecurityConfig` con **Azure AD JWT** (perfil `weblogic`)
- [ ] Configurar `DevSecurityConfig` con HTTP Basic (perfil `local-dev`)
- [ ] Configurar `AuditConfig` (`AuditorAware` → `preferred_username` del JWT)
- [ ] Agregar `GlobalExceptionHandler`
- [ ] Configurar **Swagger (SpringDoc 1.7.0)** — **siempre activo**, no solo local
- [ ] Anotar todos los controllers con `@Tag` y `@Operation`
- [ ] Configurar `application.yml` con perfiles `local-dev` y `weblogic`
- [ ] Verificar compilación con JDK 8: `mvn clean package`

### Frontend
- [ ] Inicializar proyecto **Angular 20** (standalone, strict mode)
- [ ] Instalar **PrimeNG 21** (`npm install primeng @primeng/themes`)
- [ ] Instalar **MSAL Angular** (`npm install @azure/msal-angular @azure/msal-browser`)
- [ ] Configurar `providePrimeNG()` con tema Aura y tokens SED
- [ ] Configurar MSAL con `tenantId`, `clientId` y `apiScopes` de Azure AD
- [ ] Configurar Tailwind CSS con design tokens SED (layout/utilidades)
- [ ] Crear estructura: `core/`, `shared/`, `features/`
- [ ] Implementar `AuthService` (Facade MSAL + DevSession)
- [ ] Usar `MsalGuard` para proteger rutas principales
- [ ] Implementar `roleGuard` para control granular RBAC
- [ ] El `MsalInterceptor` maneja automáticamente el `Authorization: Bearer`
- [ ] Configurar `environments/` (Azure AD en prod, devSession en local-dev)
- [ ] Crear `AppShellComponent` (sidebar PrimeNG + topbar + router-outlet)
- [ ] Configurar lazy loading en `app.routes.ts`
- [ ] Crear `proxy.conf.json` → `/api` a `:8080` (solo desarrollo local)
- [ ] Usar componentes PrimeNG 21 en lugar de construir desde cero

### Base de Datos
- [ ] Crear usuario Oracle con esquema `SED_[MODULO]` (coordinar con DBA)
- [ ] Definir DDL en `db/00_setup.sql` (tablas, sequences, triggers, vistas)
- [ ] Incluir campos de auditoría en todas las tablas (`CREATED_BY` = email O365)
- [ ] Crear vistas desnormalizadas para reportes (`V_*`)
- [ ] Crear datos de prueba en `db/01_datos_prueba.sql` para desarrollo local
- [ ] Validar que `hibernate.dialect = Oracle12cDialect` (compatible 19c)

### Infraestructura WebLogic
- [ ] Coordinar con TI SED acceso a consola WebLogic 12.2.1.4.0
- [ ] Definir datasource JNDI en WebLogic para conexión Oracle
- [ ] Verificar que JDK 8 está disponible en el servidor
- [ ] Configurar variables de sistema/JNDI: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`
- [ ] Desplegar WAR desde consola WebLogic admin

### CI/CD
- [ ] `.github/workflows/ci.yml` — Java 8 (Oracle) + Node 20 LTS
- [ ] Pipeline genera artefacto `*.war` y `dist/` como artefactos descargables
- [ ] `.gitignore` con: `.env`, `target/`, `dist/`, `node_modules/`, Oracle wallets

### Documentación
- [ ] `docs/ARRANQUE.md` con pasos de inicio local (Oracle local → Backend → Frontend)
- [ ] `docs/TECNOLOGIAS.md` con versiones exactas de todas las dependencias
- [ ] `SDD_SPEC_v1.md` con arquitectura, pantallas, modelos de datos
- [ ] `PRD_[NOMBRE].txt` con requerimientos del producto
- [ ] `README.md` mínimo con descripción, arquitectura y enlace a docs/ARRANQUE.md

---

## Referencias

| Recurso | Ubicación |
|---------|-----------|
| Proyecto | `ProyectoContratosSED/` |
| Constitucion SDD | `docs/CONSTITUTION.md` |
| Especificaciones tecnicas | `docs/specs/` |
| Planes de implementacion | `docs/plans/` |
| Guía de arranque | `docs/ARRANQUE.md` |
| Versiones del stack | `docs/TECNOLOGIAS.md` |
| Requerimientos | `docs/specs/2026-04-30-sigcon-prd.md` |
| Design System | `Prototipo/DESIGN.md` |
| Scripts Oracle | `db/00_setup.sql` |

---

*Documento adaptado para `ProyectoContratosSED` — SIGCON / Ecosistema SED Bogotá*  
*Revisión 3 aplicada para coherencia SDD — 01/05/2026*  
*Actualizar cuando cambien decisiones arquitectónicas fundamentales.*
