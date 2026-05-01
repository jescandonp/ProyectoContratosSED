# ARRANQUE SIGCON

> Estado: pre-implementacion I1.
> Metodologia: Spec-Driven Development (SDD), nivel Spec-Anchored.
> Este documento se debe actualizar al cerrar cada incremento.

## Orden De Trabajo

1. Constitucion SDD: `docs/CONSTITUTION.md`
2. Arquitectura SIGCON/SED: `docs/ARCHITECTURE.md`
3. Versiones del stack: `docs/TECNOLOGIAS.md`
4. PRD: `docs/specs/2026-04-30-sigcon-prd.md`
5. Spec tecnica I1: `docs/specs/2026-04-30-sigcon-i1-spec.md`
6. Specs futuras de referencia: `docs/specs/2026-05-01-sigcon-i2-spec.md` y `docs/specs/2026-05-01-sigcon-i3-spec.md`
7. Plan I1: `docs/plans/2026-05-01-sigcon-i1-implementation-plan.md`
8. Implementacion I1

## Estado Actual

El proyecto esta listo documentalmente para iniciar la implementacion de I1, pero todavia no existen los artefactos generados de backend, frontend ni base de datos.

Pendiente por implementar en I1:

- `db/00_setup.sql`
- `db/01_datos_prueba.sql`
- `sigcon-backend/`
- `sigcon-angular/`

## Alcance I1

Incluye:

- Auth `local-dev` y perfil de usuario.
- Imagen de firma en perfil.
- Administracion de contratos, obligaciones, usuarios y catalogo de documentos.
- Vista de contratos y detalle para contratista.

Excluye:

- Creacion y gestion de informes.
- Flujo de revision/aprobacion.
- Generacion de PDF.
- Notificaciones email o in-app.
- Integracion SECOP2.
- Contratos de personas juridicas/proveedores.

## Stack Base

Resumen operativo. Ver detalle completo en `docs/TECNOLOGIAS.md`.

| Capa | Decision |
|------|----------|
| Backend | Java 8, Spring Boot 2.7.18, Maven, WAR |
| Servidor | Oracle WebLogic 12.2.1.4.0 |
| Base de datos | Oracle 19c+, esquema `SED_SIGCON`, prefijo `SGCN_` |
| Frontend | Angular 20, PrimeNG 20, Tailwind CSS 3.4 |
| Auth local | HTTP Basic en perfil `local-dev` |
| Auth servidor | Azure AD JWT / Office 365 en perfil `weblogic` |
| WAR | `sigcon-backend.war` |
| Contexto | `/sigcon` |

## Prerrequisitos Locales

Antes de ejecutar I1:

- Oracle JDK 8 disponible en terminal.
- Maven 3.9.x disponible en terminal.
- Node.js 20 LTS disponible en terminal.
- npm disponible en terminal.
- Oracle local o compatible 19c accesible como `jdbc:oracle:thin:@localhost:1521/XEPDB1`.
- Usuario/esquema local `SED_SIGCON` creado o coordinado con DBA local.

## Flujo De Implementacion I1

La implementacion no debe hacerse libremente desde codigo. Debe seguir el plan aprobado:

```powershell
Get-Content docs\plans\2026-05-01-sigcon-i1-implementation-plan.md
```

Orden esperado:

1. Crear scripts Oracle I1.
2. Crear backend `sigcon-backend` como WAR compatible con WebLogic.
3. Implementar dominio, servicios, seguridad y APIs I1.
4. Crear frontend `sigcon-angular`.
5. Implementar shell, auth local, perfil, contratos y vistas admin/contratista.
6. Ejecutar verificacion local y actualizar este documento.

## Comandos Esperados Despues De I1

Estos comandos aplican cuando existan los artefactos I1.

Backend:

```powershell
Set-Location sigcon-backend
mvn test
mvn clean package
```

Resultado esperado:

- Pruebas backend pasan.
- Se genera `sigcon-backend/target/sigcon-backend.war`.

Frontend:

```powershell
Set-Location sigcon-angular
npm install
npm test
npm run build
```

Resultado esperado:

- Pruebas frontend pasan.
- Build Angular de produccion termina sin errores.

Local dev:

```powershell
Set-Location sigcon-backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-dev
```

En otra terminal:

```powershell
Set-Location sigcon-angular
npm start
```

URLs esperadas despues de I1:

- Backend local: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Frontend local: `http://localhost:4200`

## Credenciales Local-Dev

Segun PRD/spec I1:

| Usuario | Password | Rol |
|---------|----------|-----|
| `admin` | `admin123` | `ADMIN` |
| `contratista` | `contratista123` | `CONTRATISTA` |
| `revisor` | `revisor123` | `REVISOR` |
| `supervisor` | `supervisor123` | `SUPERVISOR` |

## Regla De Actualizacion

Actualizar este archivo cuando:

- Cambie una version en `docs/TECNOLOGIAS.md`.
- Cambie una coordenada en `docs/ARCHITECTURE.md`.
- Se cierre I1, I2 o I3.
- Cambien los comandos reales de arranque local.
- Se agregue configuracion `weblogic` real validada con entorno SED.
