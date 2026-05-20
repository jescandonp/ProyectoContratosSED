# ARRANQUE SIGCON

> Estado: Incremento 8 completado — listo para despliegue PROD (ambiente pruebas SED).
> Metodologia: Spec-Driven Development (SDD), nivel Spec-Anchored.
> Ultima actualizacion: 2026-05-20 — 4 errores WebLogic resueltos (E-WL-01 a E-WL-04).

## Orden De Documentos

1. Constitucion SDD: `docs/CONSTITUTION.md`
2. Arquitectura SIGCON/SED: `docs/ARCHITECTURE.md`
3. Versiones del stack: `docs/TECNOLOGIAS.md`
4. PRD: `docs/specs/2026-04-30-sigcon-prd.md`
5. Spec tecnica I1: `docs/specs/2026-04-30-sigcon-i1-spec.md`
6. Spec tecnica I2: `docs/specs/2026-05-01-sigcon-i2-spec.md`
7. Spec tecnica I3: `docs/specs/2026-05-01-sigcon-i3-spec.md`
8. Spec tecnica I4: `docs/specs/2026-05-04-sigcon-i4-spec.md`
9. Spec tecnica I5: `docs/specs/2026-05-04-sigcon-i5-spec.md`
10. Spec tecnica I6: `docs/specs/2026-05-09-sigcon-i6-spec.md`
11. Spec tecnica I7: `docs/specs/2026-05-11-sigcon-i7-spec.md`
12. Plan I7: `docs/plans/2026-05-11-sigcon-i7-plan.md`
13. Log de ejecucion I7: `docs/plans/2026-05-11-sigcon-i7-execution-log.md`
14. Spec tecnica I8: `docs/specs/2026-05-18-sigcon-i8-spec.md`
15. Log de ejecucion I8: `docs/plans/2026-05-18-sigcon-i8-execution-log.md`

## Estado GitHub

Repositorio remoto: `https://github.com/jescandonp/ProyectoContratosSED.git`

| Rama | Commit | Estado |
|---|---:|---|
| `main` | `9c93fdd` | Sincronizada — incluye hasta I8 + 4 fixes WebLogic (E-WL-01..04) |
| `feat/sigcon-i1` | `be26bbe` | Sincronizada |
| `feat/sigcon-i2` | `0658cef` | Sincronizada |
| `feat/sigcon-i3` | `9be9c73` | Sincronizada |
| `feat/sigcon-i4` | `7b61d09` | Sincronizada |
| `feat/sigcon-i5` | `0cc76a9` | Sincronizada |
| `feat/sigcon-i6` | `df9762c` | Sincronizada |
| `feat/sigcon-i7` | Ver `git log origin/feat/sigcon-i7 -1` | Sincronizada |
| `feat/sigcon-i8` | mergeado a `main` directamente | No existe como rama separada |

> **I8 fue integrado directamente en `main`** — no tiene rama `feat/sigcon-i8` en remoto.
> Todos los commits `feat(i8)` / `fix(i8)` / `docs(i8)` estan en `origin/main`.

Para clonar y continuar desde I8 (estado actual):

```powershell
git clone https://github.com/jescandonp/ProyectoContratosSED.git
Set-Location ProyectoContratosSED
# main ya incluye I8 completo
```

El lider tecnico puede cambiar apuntamientos locales despues del clone usando los valores reales del ambiente SED para Oracle, WebLogic, Azure AD y rutas compartidas de firmas.

## Alcance I8 (implementado)

Incluye:
- **PDF formato institucional SED 11-IF-023 V1** — rediseno completo de `InformePdfTemplateService`: encabezado con logo Alcaldia Mayor + datos del informe repetido en cada pagina, pie de pagina institucional, 5 secciones numeradas (Datos del Contrato, Ejecucion de Actividades, Aportes SGSSI, Estado Radicacion Correspondencia, Declaracion Especial), layout de firmas 2+1 (contratista + supervisor / revisor opcional).
- **Campo `fechaElaboracion`** — nueva columna `SGCN_INFORMES.FECHA_ELABORACION` (migration idempotente `db/05_add_fecha_elaboracion.sql`); editable por el contratista en BORRADOR/DEVUELTO; default a fecha actual si no se envia.
- **Logo institucional en classpath** — `logo-alcaldia.png` empaquetado en `WEB-INF/classes/`; fallback graceful con WARN si ausente.
- **Fix Java 8** — `StreamUtils.copyToByteArray()` reemplaza `readAllBytes()` (Java 9+) para compatibilidad con WebLogic.
- **Datasource JNDI** — perfil `weblogic` usa `spring.datasource.jndi-name` en lugar de JDBC directo; compatible con pools Oracle administrados por WebLogic Admin Console.
- Build Angular sin errores TypeScript. Tests backend: todos GREEN.
- Spec tecnica, log de ejecucion y documentacion de despliegue versionados.

## Alcance I7 (implementado)

Incluye:
- **Usuario responsable de IVA** — campo `responsableIva` en `SGCN_USUARIOS`; formulario admin con checkbox; mensajes de confirmación al crear/editar.
- **Documentos requeridos por informe** — nueva tabla `SGCN_DOCS_REQUERIDOS`; endpoints carga/descarga/preview/eliminar bajo `/api/informes/{id}/documentos-requeridos`; solo PDF y `.eml`.
- **FACTURA dinámica** — si `responsableIva=true`, el informe exige FACTURA antes de enviarse; no depende de parametrización manual.
- **Preview EML** — modal con asunto, remitente, destinatarios, fecha y cuerpo texto; descarga del original siempre disponible.
- **Sección "Documentos Requeridos" en UI** — editable en BORRADOR/DEVUELTO; solo lectura en ENVIADO/EN_REVISION/APROBADO.
- **Email de aprobación** — `EmailNotificacionService.notificarAprobacion()` envía al contratista y al admin configurable (`SIGCON_ADMIN_EMAIL`); fallo no revierte aprobación.
- **Búsqueda administrativa global** — `GET /api/admin/busqueda?q=&fechaInicio=&fechaFin=`; resultados agrupados por contratistas, contratos e informes; ruta Angular `/admin/busqueda`.
- 170 tests backend, 0 fallos. Build Angular exitoso.
- Spec, plan y log de ejecución I7 versionados.

## Alcance I6 (implementado)

Incluye:
- **PDF institucional SED** — rediseno completo `InformePdfTemplateService`: 8 secciones, encabezado `#002869`, valor del desembolso en letras (`NumeroPesosConverter`), firma del revisor opcional.
- **Aportes SGSSI por informe** — nueva tabla `SGCN_APORTES_SGSSI` con borrado logico; endpoints GET + PUT `/api/informes/{id}/aportes-sgssi`; seccion editable en `InformeFormComponent` e `InformeDetalleComponent`.
- **Datos complementarios del contrato** — campos `dependencia` (catalogo 44 unidades SED via `<datalist>`), `formaPago`, `modificaciones` en el formulario admin.
- **Perfil contratista SGSSI** — campos `sgssiSaludEntidad`, `sgssiPensionEntidad`, `sgssiArlEntidad` en perfil.
- **Campos nuevos del informe** — `numeroDesembolso`, `valorDesembolso`, `porcentajeEjecucion`, `correspondenciaPendiente` en encabezado del informe.
- **Eliminacion de `porcentaje` por actividad** (Enmienda E2) — campo eliminado de DTOs y templates; permanece mapeado en entidad Hibernate.
- 135 tests backend, 106 specs frontend, 0 fallos.
- Spec, plan y log de ejecucion I6 versionados.

## Alcance I5 (implementado)

Incluye:
- Edicion inline de actividades desde `InformeDetalleComponent` para informes en estado BORRADOR.
- Edicion de descripcion, soportes y documentos adicionales sin salir del detalle del informe.
- Vista de detalle en solo lectura para estados distintos de BORRADOR.
- Uso de endpoints existentes de I2-I4; no agrega DDL ni endpoints nuevos.
- Spec, plan y log de ejecucion I5 versionados.

## Alcance I4 (implementado)

Incluye:
- Edicion administrativa de contratos.
- Revisor opcional en contratos.
- Flujo directo de supervisor cuando no hay revisor asignado.
- Edicion del periodo del informe desde el detalle cuando el informe es editable.

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

### 2. Ejecutar DDL y datos base

```powershell
# Conectar con sqlplus y ejecutar en orden:
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/00_setup.sql
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/01_datos_prueba.sql
```

`db/00_setup.sql` contiene DDL acumulado hasta I7. I4 e I5 no agregaron DDL. I6 e I7 agregan bloques al final.
`db/01_datos_prueba.sql` solo debe ejecutarse en ambientes de desarrollo local.

### 3. Actualizar esquemas existentes a I7

Si el esquema ya existia antes de I7, no repetir `db/00_setup.sql` completo. Ejecutar solo la migracion incremental I7:

```powershell
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/04_apply_i7_schema.sql
```

Este script crea de forma idempotente:

- `SGCN_USUARIOS.RESPONSABLE_IVA`
- `SGCN_DOCS_REQUERIDOS_SEQ`
- `SGCN_DOCS_REQUERIDOS`
- indices y trigger de auditoria de documentos requeridos

Si al levantar backend aparece `Schema-validation: missing table [sgcn_docs_requeridos]`, falta ejecutar esta migracion en la BD objetivo.

### 4. Actualizar esquemas existentes a I8

Si el esquema ya existia antes de I8, no repetir `db/00_setup.sql` completo. Ejecutar solo la migracion incremental I8:

```powershell
sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1 @db/05_add_fecha_elaboracion.sql
```

Este script agrega de forma idempotente:

- `SGCN_INFORMES.FECHA_ELABORACION`

Si al levantar backend aparece `Schema-validation: missing column [fecha_elaboracion] in table [sgcn_informes]`, falta ejecutar esta migracion en la BD objetivo.

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
| `SIGCON_MAIL_ENABLED` | `true` por defecto en perfil `weblogic`; usar `false` solo para despliegues controlados sin envio real |
| `GRAPH_API_BASE_URL` | Opcional; default `https://graph.microsoft.com/v1.0` |
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

## Configuracion WebLogic (produccion — ambiente pruebas SED)

### Datasource JNDI en WebLogic Admin Console

El perfil `weblogic` usa **JNDI** en lugar de JDBC directo. El DBA / administrador WebLogic debe:

1. Ingresar a **WebLogic Admin Console → Services → Data Sources → New → Generic Data Source**.
2. Configurar con los siguientes valores:

   | Campo | Valor |
   |---|---|
   | JNDI Name | `jdbc/sigconDS` (o el valor de `DB_JNDI_NAME`) |
   | Database Type | Oracle |
   | Driver | Oracle's Driver (Thin) for Instance connections |
   | URL | `jdbc:oracle:thin:@<host>:<puerto>/<SID>` |
   | Database User | `SED_SIGCON` |
   | Password | (password del esquema Oracle produccion) |

3. Hacer **Target** del datasource al servidor/cluster donde se desplegara `sigcon-backend.war`.

### Variables de entorno requeridas

| Variable | Descripcion |
|---|---|
| `DB_JNDI_NAME` | Nombre JNDI del DataSource en WebLogic (default: `jdbc/sigconDS`) |
| `AZURE_TENANT_ID` | Tenant ID de Azure AD / Office 365 SED |
| `SPRING_PROFILE` | `weblogic` |

> **Nota:** `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` ya NO son necesarios en el perfil `weblogic`. La conexion se obtiene via JNDI desde el pool de WebLogic.

### Configuracion de correo I3

Para notificaciones email:

```yaml
# application.yml — perfil weblogic
sigcon:
  mail:
    enabled: ${SIGCON_MAIL_ENABLED:true}
    from: ${MAIL_FROM}
    graph-api-base-url: ${GRAPH_API_BASE_URL:https://graph.microsoft.com/v1.0}
    tenant-id: ${AZURE_TENANT_ID}
    client-id: ${MAIL_CLIENT_ID}
    client-secret: ${MAIL_CLIENT_SECRET}
```

En `local-dev` poner `sigcon.mail.enabled: false`; el sistema registra los emails simulados en logs.

### Despliegue WAR en WebLogic

1. Generar WAR (requiere JDK 8+, Maven 3.9.x):
   ```powershell
   Set-Location sigcon-backend
   mvn clean package -DskipTests
   # Se generan DOS archivos en target/:
   #   sigcon-backend.war          (~57 MB) → Spring Boot fat WAR  ⚠️  NO usar en WebLogic
   #   sigcon-backend.war.original (~47 MB) → WAR estandar Maven   ✅  ESTE es el correcto
   ```
   > **CRITICO:** Desplegar `sigcon-backend.war.original`, no `sigcon-backend.war`.
   > El fat WAR incluye `WEB-INF/lib-provided/` que WebLogic interpreta y que puede
   > causar conflictos de classpath adicionales.

2. Verificar que el WAR incluye el logo institucional (debe listar `logo-alcaldia.png`):
   ```powershell
   jar tf sigcon-backend/target/sigcon-backend.war | Select-String "logo-alcaldia"
   # Resultado esperado: WEB-INF/classes/logo-alcaldia.png
   ```
   El archivo esta en `src/main/resources/logo-alcaldia.png` y Maven lo empaqueta
   automaticamente en `WEB-INF/classes/`. No es necesario copiar la imagen manualmente.

3. Configurar el DataSource JNDI en **WebLogic Admin Console** (ver seccion anterior).

4. Configurar las variables de entorno en el servidor WebLogic antes del despliegue:
   ```
   SPRING_PROFILE=weblogic
   DB_JNDI_NAME=jdbc/sigconDS
   AZURE_TENANT_ID=<tenant-id-SED>
   MAIL_FROM=<correo-institucional>
   MAIL_CLIENT_ID=<client-id-Graph>
   MAIL_CLIENT_SECRET=<client-secret-Graph>
   SIGCON_CORS_ALLOWED_ORIGINS=<URL-frontend-SED>
   SIGCON_ADMIN_EMAIL=<correo-admin-sigcon>
   ```

5. Desplegar `sigcon-backend.war.original` desde WebLogic Admin Console:
   - **Deployments → Install → Upload → seleccionar `sigcon-backend.war.original`**.
   - Contexto resultante: `/sigcon` (configurado en `WEB-INF/weblogic.xml`).
   - ⚠️ **No confundir** con `sigcon-backend.war` (~57 MB): ese es el fat WAR ejecutable
     de Spring Boot y **no** debe desplegarse en WebLogic.

6. Ejecutar migraciones de BD en Oracle produccion si el esquema no existe:
   ```sql
   -- Conectar como DBA y ejecutar en orden:
   @db/00_setup.sql
   -- Si el esquema ya existia hasta I7, solo ejecutar incrementales:
   @db/05_add_fecha_elaboracion.sql
   ```

7. Verificar health del backend:
   ```
   GET http://<servidor>/sigcon/actuator/health
   -- Respuesta esperada: {"status":"UP"}
   ```

## Errores Conocidos De Despliegue WebLogic

### E-WL-01 — `cvc-enumeration-valid: string value '4.0'` al desplegar el WAR

**Síntoma (WebLogic Admin Console):**
```
VALIDATION PROBLEMS WERE FOUND
WEB-INF/lib/tomcat-embed-el-9.0.83.jar!/META-INF/web-fragment.xml
problem: cvc-enumeration-valid: string value '4.0' is not a valid enumeration
value for web-app-versionType in namespace http://xmlns.jcp.org/xml/ns/javaee
```

**Causa:** WebLogic 12.2.1 soporta hasta Servlet 3.1 (Java EE 7). El jar
`tomcat-embed-el` incluye un `web-fragment.xml` que declara `web-app version="4.0"`
(Servlet 4.0 / Java EE 8). Spring Boot lo incluía con scope `compile` para
Bean Validation. WebLogic escanea todos los jars del WAR (incluyendo
`WEB-INF/lib-provided/`) antes de desplegar y falla en la validación.

**Solución (ya aplicada en `main`):** `tomcat-embed-el` excluido del WAR
en `pom.xml` — WebLogic provee su propia implementación de `javax.el`.
Verificar que el WAR generado **no** contiene el jar:
```powershell
jar tf sigcon-backend/target/sigcon-backend.war | Select-String "tomcat-embed-el"
# No debe retornar ninguna línea
```

**Archivos modificados:** `sigcon-backend/pom.xml`, `WEB-INF/weblogic.xml`
**Commit:** `dcd4e53`

---

### E-WL-02 — `Cannot resolve method readAllBytes` al compilar

**Síntoma:** Error de compilación Maven en `InformePdfTemplateService.java`.

**Causa:** `InputStream.readAllBytes()` es Java 9+. El proyecto compila en Java 8.

**Solución (ya aplicada en `main`):** Reemplazado por
`StreamUtils.copyToByteArray()` de Spring Framework.
**Commit:** `83e9a80`

---

### E-WL-03 — `prefer-application-packages cannot be specified when prefer-web-inf-classes is turned on`

**Síntoma (WebLogic Admin Console):**
```
Neither <prefer-application-packages> nor <prefer-application-resources> can be specified
when <prefer-web-inf-classes> is turned on in weblogic.xml
```

**Causa:** `weblogic.xml` tenía simultáneamente `<prefer-web-inf-classes>true</prefer-web-inf-classes>`
y `<prefer-application-packages>`. En WebLogic 12.2.1 son **mutuamente excluyentes**
por diseño — no se pueden usar a la vez.

**Solución (ya aplicada en `main`):** Se eliminó `prefer-application-packages` y
`prefer-application-resources` del intento anterior. Solo queda `prefer-web-inf-classes`.
**Commit:** `67dde25`

---

### E-WL-04 — `NoSuchMethodError: javax.validation.BootstrapConfiguration.getClockProviderClassName()`

**Síntoma (WebLogic Admin Console):**
```
java.lang.NoSuchMethodError: javax.validation.BootstrapConfiguration.getClockProviderClassName()Ljava/lang/String;
```

**Causa:** `prefer-web-inf-classes=true` **no cubre paquetes Java EE** (`javax.*`).
WebLogic los carga siempre desde el servidor, sin excepción. WebLogic 12.2.1 incluye
Bean Validation **1.1**; Spring Boot 2.7 requiere Bean Validation **2.0**
(`getClockProviderClassName()` fue introducido en BV 2.0).
El WAR contiene `jakarta.validation-api-2.0.2.jar` pero WebLogic lo ignora para
`javax.validation.*` y usa su propia versión 1.1.

**Solución (ya aplicada en `main` — commit `9c93fdd`):**
Se reemplazó `prefer-web-inf-classes` por `prefer-application-packages` con lista
explícita de paquetes. Esta directiva **sí** permite sobreescribir paquetes `javax.*`.

Paquetes declarados en `weblogic.xml`:
- `javax.validation.*` y `org.hibernate.validator.*` — Bean Validation 2.0
- `javax.persistence.*` y `org.hibernate.*` — JPA 2.2 / Hibernate ORM 5.6
- `org.springframework.*`, `com.fasterxml.*`, `org.slf4j.*`, `ch.qos.logback.*`
- `org.aopalliance.*`, `org.aspectj.*`, `org.apache.commons.*`
- `antlr.*`, `net.bytebuddy.*`, `com.nimbusds.*`, `net.minidev.*`

Recursos SPI declarados en `<prefer-application-resources>`:
- `META-INF/spring.factories`, `META-INF/spring/*.imports`
- `META-INF/services/javax.persistence.spi.PersistenceProvider`
- `META-INF/services/javax.validation.spi.ValidationProvider`
- `META-INF/services/com.fasterxml.jackson.databind.Module`

**Archivos modificados:** `WEB-INF/weblogic.xml`
**Commit:** `9c93fdd`

---

## Regla De Actualizacion

Actualizar este documento cuando:
- Cambie una version en `docs/TECNOLOGIAS.md`.
- Se cierre un incremento SIGCON.
- Cambien comandos reales de arranque local o WebLogic.
- Se agregue configuracion validada con entorno SED.
