# ARRANQUE SIGCON

> Estado: Incremento 3 completado.
> Metodologia: Spec-Driven Development (SDD), nivel Spec-Anchored.
> Ultima actualizacion: 2026-05-02 — cierre I3.

## Orden De Documentos

1. Constitucion SDD: `docs/CONSTITUTION.md`
2. Arquitectura SIGCON/SED: `docs/ARCHITECTURE.md`
3. Versiones del stack: `docs/TECNOLOGIAS.md`
4. PRD: `docs/specs/2026-04-30-sigcon-prd.md`
5. Spec tecnica I1: `docs/specs/2026-04-30-sigcon-i1-spec.md`
6. Spec tecnica I2: `docs/specs/2026-05-01-sigcon-i2-spec.md`
7. Spec tecnica I3: `docs/specs/2026-05-01-sigcon-i3-spec.md`
8. Plan I3: `docs/plans/2026-05-02-sigcon-i3-implementation-plan.md`
9. Log de ejecucion I3: `docs/plans/2026-05-02-sigcon-i3-execution-log.md`

## Alcance I3 (implementado)

Incluye:
- Generacion de PDF institucional al aprobar informes.
- PDF inmutable: si ya existe `pdfRuta`, no se regenera.
- Metadatos PDF expuestos en API: `pdfRuta`, `pdfGeneradoAt`, `pdfHash`.
- Descarga PDF desde `/api/informes/{id}/pdf`.
- Vista Angular `/informes/:id/pdf` para descarga de informe aprobado.
- Notificaciones in-app por eventos de informe.
- Centro de notificaciones Angular en `/notificaciones`.
- Campana de notificaciones en topbar con contador de no leidas.
- Email simulado en `local-dev` y configurable para WebLogic.
- Advertencia de firma faltante en perfil para CONTRATISTA/SUPERVISOR.

Excluye:
- Firma digital criptografica avanzada (PKCS#11, CAdES, XAdES).
- Radicacion oficial externa.
- Motor de pagos.
- Integracion SECOP2.
- Panel de administracion de notificaciones.

## Alcance I2 (implementado)

Incluye:
- Creacion y gestion de informes de actividades (CONTRATISTA).
- Adjuntar soportes URL y archivo por actividad.
- Documentos adicionales del catalogo OPS.
- Vista previa del informe (read-only, sin PDF real).
- Correccion de informes devueltos con historial de observaciones.
- Cola de revision para REVISOR: aprobar revision o devolver con observacion.
- Cola de aprobacion para SUPERVISOR: aprobar o devolver con observacion.
- Maquina de estados completa: `BORRADOR → ENVIADO → EN_REVISION → APROBADO` (y devoluciones).
- Historial de informes en detalle de contrato (CONTRATISTA/REVISOR/SUPERVISOR/ADMIN).
- Boton "Nuevo Informe" habilitado en detalle de contrato para contratos `EN_EJECUCION`.
- Card "Informes" activa en dashboard ADMIN (enlaza a lista de contratos).
- Sidebar con entradas "Revision" (REVISOR) y "Aprobacion" (SUPERVISOR).

## Alcance I1 (implementado)

Incluye:
- Auth `local-dev` (HTTP Basic) y perfil de usuario con imagen de firma.
- Administracion de contratos, obligaciones, usuarios y catalogo de documentos (ADMIN).
- Vista de contratos y detalle para CONTRATISTA/SUPERVISOR/REVISOR.

## Stack

| Capa | Decision |
|------|----------|
| Backend | Java 8, Spring Boot 2.7.18, Maven, WAR |
| Servidor | Oracle WebLogic 12.2.1.4.0 |
| Base de datos | Oracle 19c+, esquema `SED_SIGCON`, prefijo `SGCN_` |
| Frontend | Angular 20, PrimeNG 20, Tailwind CSS 3.4 |
| Auth local-dev | HTTP Basic, perfil `local-dev` |
| Auth weblogic | Azure AD JWT / Office 365, perfil `weblogic` |
| Artefacto WAR | `sigcon-backend.war` |
| Contexto WebLogic | `/sigcon` |

## Prerrequisitos Locales

| Herramienta | Version minima | Verificacion |
|---|---|---|
| Oracle JDK 8 | 8u361+ | `java -version` |
| Maven | 3.9.x | `mvn -version` |
| Node.js | 20 LTS | `node -v` |
| npm | 9+ | `npm -v` |
| Oracle DB | 19c compatible | verificar con DBA |

> **Nota:** La maquina de desarrollo puede tener JDK 21 para ejecutar Maven; el `pom.xml` compila con source/target `1.8`. Validar con JDK 8 real antes de desplegar en WebLogic.

## Configuracion Local-Dev: Base De Datos

### 1. Crear esquema Oracle

Ejecutar como DBA (una sola vez, NO es idempotente):

```sql
CREATE USER SED_SIGCON IDENTIFIED BY <password>;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE TRIGGER, CREATE INDEX TO SED_SIGCON;
GRANT UNLIMITED TABLESPACE TO SED_SIGCON;
```

### 2. Ejecutar DDL I1 + I2 + I3

```powershell
# Conectar con sqlplus y ejecutar en orden:
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/00_setup.sql
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/01_datos_prueba.sql
```

`db/00_setup.sql` contiene DDL de I1, I2 e I3 bajo cabeceras `-- ===== INCREMENTO 1 =====`, `-- ===== INCREMENTO 2 =====` y `-- ===== INCREMENTO 3 =====`.
`db/01_datos_prueba.sql` solo debe ejecutarse en ambientes de desarrollo local.

## Configuracion I3: PDF Y Email

### Local-dev

| Propiedad | Valor local-dev |
|---|---|
| `sigcon.mail.enabled` | `false` |
| Email | Envios simulados en logs; no requiere credenciales Azure |
| PDF | Generado en `${java.io.tmpdir}/sigcon-test/pdfs/` |

En local-dev no se debe configurar Microsoft Graph. Los eventos de email se registran en logs para facilitar pruebas sin dependencias externas.

### WebLogic

Configurar variables de entorno:

| Variable / propiedad | Descripcion |
|---|---|
| `MAIL_FROM` | Cuenta remitente institucional |
| `AZURE_TENANT_ID` | Tenant ID de Azure AD / Office 365 SED |
| `MAIL_CLIENT_ID` | Client ID de la aplicacion Graph |
| `MAIL_CLIENT_SECRET` | Client secret de la aplicacion Graph |
| `sigcon.mail.enabled` | `true` |
| `sigcon.storage.signatures-path` | Ruta compartida para firmas |

## Configuracion Local-Dev: Backend

### Variables de entorno requeridas

| Variable | Valor local-dev |
|---|---|
| `DB_USERNAME` | `SED_SIGCON` |
| `DB_PASSWORD` | (password del esquema Oracle local) |
| `SPRING_PROFILE` | `local-dev` (opcional, es el default) |

```powershell
$env:DB_USERNAME = "SED_SIGCON"
$env:DB_PASSWORD = "<password>"
```

### Levantar backend

```powershell
Set-Location sigcon-backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-dev
```

Backend disponible en `http://localhost:8080`.

### Ejecutar pruebas backend

```powershell
Set-Location sigcon-backend
mvn test
```

Resultado esperado: todos los tests pasan.

### Empaquetar WAR

```powershell
Set-Location sigcon-backend
mvn clean package
# Genera: sigcon-backend/target/sigcon-backend.war
```

## Configuracion Local-Dev: Frontend

### Instalar dependencias

```powershell
Set-Location sigcon-angular
# Si npm global falla, usar la ruta completa:
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
```

### Levantar frontend (requiere backend corriendo)

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" start
# O si npm global funciona:
npm start
```

Frontend disponible en `http://localhost:4200`. El proxy reenvía `/api`, `/api-docs`, `/swagger-ui.html` y `/actuator` al backend en puerto 8080.

### Ejecutar pruebas frontend

```powershell
Set-Location sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

### Build produccion

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

## URLs Local-Dev

| Recurso | URL |
|---|---|
| Frontend SPA | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs | http://localhost:8080/api-docs |
| Health | http://localhost:8080/actuator/health |

## Credenciales Local-Dev

Usuarios disponibles en `db/01_datos_prueba.sql` y en `DevSecurityConfig` / `DevSessionService` del frontend:

| Email (usuario HTTP Basic) | Password | Rol |
|---|---|---|
| `admin@educacionbogota.edu.co` | `admin123` | `ADMIN` |
| `juan.escandon@educacionbogota.edu.co` | `contratista123` | `CONTRATISTA` |
| `revisor1@educacionbogota.edu.co` | `revisor123` | `REVISOR` |
| `supervisor1@educacionbogota.edu.co` | `supervisor123` | `SUPERVISOR` |

> **IMPORTANTE:** La pantalla de login en local-dev muestra botones de acceso directo por rol. No se requiere ingresar credenciales manualmente.

## Configuracion WebLogic (produccion)

### Variables de entorno requeridas

| Variable | Descripcion |
|---|---|
| `DB_URL` | JDBC URL de Oracle produccion (e.g. `jdbc:oracle:thin:@host:1521/SIDPROD`) |
| `DB_USERNAME` | Usuario Oracle produccion |
| `DB_PASSWORD` | Password Oracle produccion |
| `AZURE_TENANT_ID` | Tenant ID de Azure AD / Office 365 SED |
| `SPRING_PROFILE` | `weblogic` |

### Configuracion de correo I3

Para notificaciones email:

```yaml
# application.yml — perfil weblogic
sigcon:
  mail:
    enabled: true
    from: ${MAIL_FROM}
    tenant-id: ${AZURE_TENANT_ID}
    client-id: ${MAIL_CLIENT_ID}
    client-secret: ${MAIL_CLIENT_SECRET}
```

En `local-dev` poner `sigcon.mail.enabled: false`; el sistema registra los emails simulados en logs.

### Despliegue WAR en WebLogic

1. Generar WAR: `mvn clean package`
2. Copiar `sigcon-backend/target/sigcon-backend.war` al directorio de despliegue WebLogic.
3. Configurar variables de entorno antes de arrancar el servidor.
4. El contexto sera `/sigcon` (configurado en `WEB-INF/weblogic.xml`).

## Regla De Actualizacion

Actualizar este documento cuando:
- Cambie una version en `docs/TECNOLOGIAS.md`.
- Se cierre I1, I2 o I3.
- Cambien comandos reales de arranque local o WebLogic.
- Se agregue configuracion validada con entorno SED.
