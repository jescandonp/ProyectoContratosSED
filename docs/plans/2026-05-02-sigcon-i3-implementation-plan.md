# SIGCON I3 Implementation Plan
## Completitud: PDF, Firmas y Notificaciones

> **Metodologia:** SDD Spec-Anchored. Este documento es fuente ejecutable.  
> **Spec rectora:** `docs/specs/2026-05-01-sigcon-i3-spec.md`  
> **Outline de origen:** `docs/plans/2026-05-01-sigcon-i3-implementation-outline.md`  
> **Execution log activo:** `docs/plans/2026-05-02-sigcon-i3-execution-log.md`  
> **Rama:** `feat/sigcon-i3` (base: `feat/sigcon-i2` HEAD `0658cef`)  
> **Fecha:** 2026-05-02

---

## Criterios De Entrada (verificados al crear este plan)

| Criterio | Estado |
|----------|--------|
| I1 implementado y verificado (64 backend + 53 frontend tests) | ✅ |
| I2 implementado y verificado (64 backend + 53 frontend tests) | ✅ |
| Maquina de estados `InformeEstadoService` funcional | ✅ |
| `EN_REVISION -> APROBADO` centralizado en `InformeEstadoService.aprobar()` | ✅ |
| `DocumentStorageService.storeFile(subdir, file)` disponible | ✅ |
| `firmaImagen` upload funcional para CONTRATISTA y SUPERVISOR | ✅ |
| `Informe.pdfRuta` nullable (seam I3) | ✅ |
| `Informe.fechaAprobacion` se setea en `aprobar()` (seam I3) | ✅ |

---

## Decisiones Tecnicas Resueltas (antes de ejecutar)

### PDF — Libreria

**Decision:** `OpenPDF 1.3.x` (com.github.librepdf:openpdf) + `Flying Saucer` (org.xhtmlrenderer:flying-saucer-pdf-openpdf).

**Razon:**
- OpenPDF es fork libre (LGPL/MPL) de iText 4; Java 8 y WebLogic 12 compatibles sin licencia comercial.
- Flying Saucer permite escribir el template como XHTML/CSS y renderizarlo a PDF, facilitando replicar el layout del DOCX de referencia.
- iText 7 Community tiene licencia AGPL no aprobada para entornos institucionales cerrados.
- No se requiere JDK > 8.

**Dependencias a agregar (pom.xml):**
```xml
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>1.3.35</version>
</dependency>
<dependency>
  <groupId>org.xhtmlrenderer</groupId>
  <artifactId>flying-saucer-pdf-openpdf</artifactId>
  <version>9.1.22</version>
</dependency>
```

### PDF Hash

**Decision:** SHA-256 (algoritmo estandar GDTI Colombia).

### Email — Mecanismo

**Decision:** Microsoft Graph API via cliente HTTP (WebClient o RestTemplate).
- En `local-dev`, `sigcon.mail.enabled=false` → log simulado.
- En `weblogic`, credenciales via environment variables.

### AuditorProvider — Deuda I2

**Decision:** I3 reemplaza el `"SYSTEM"` fijo en `JpaAuditingConfig.auditorProvider` por el email del principal autenticado via `SecurityContextHolder`. Esta tarea va en Task 6 (integracion estado).

### Almacenamiento PDF

**Decision:** Usar el mismo `DocumentStorageService` con subdirectorio `pdfs/{idContrato}/{idInforme}/`. Ruta configurable en `local-dev` via `sigcon.storage.signatures-path`. En WebLogic, debe apuntar a sistema de archivos compartido.

### SGCN_NOTIFICACIONES — Auditoria Email

**Decision:** Solo logs (sin tabla de auditoria email en MVP). Errores de envio van a log con nivel ERROR incluyendo tipo de evento, idInforme y destinatario.

---

## Ajustes Respecto Al Outline Tras Cierre I2

1. `DocumentStorageService.storeFile(subdir, file)` ya existe — Task 4 PDF puede usarlo directamente sin extension de la interfaz.
2. `JpaAuditingConfig.auditorProvider` devuelve `"SYSTEM"` — se corrige en Task 6 (integracion) como prerequisito de I3.
3. `SigconBackendSecurityTest` necesita `@MockBean` para `NotificacionRepository` (nuevo en Task 3). Task 7 debe parchearlo.
4. El test `informesAreExposedInI2ButFutureNotificationsAreNot` en `SigconBackendSecurityTest` espera `isNotFound()` en `/api/notificaciones`. En I3 ese endpoint existe → el test debe actualizarse en Task 7.
5. El template DOCX es referencia visual; no se convierte — se escribe HTML equivalente en `InformePdfTemplateService`.

---

## Tareas

### Task 1 — Plan + Execution Log Baseline

**Objetivo:** Promover el outline I3 a plan ejecutable y crear el execution log para handoff multi-modelo.

**Archivos a crear:**
- `docs/plans/2026-05-02-sigcon-i3-implementation-plan.md` (este archivo)
- `docs/plans/2026-05-02-sigcon-i3-execution-log.md`

**Validacion:**
```powershell
Test-Path docs/plans/2026-05-02-sigcon-i3-implementation-plan.md
Test-Path docs/plans/2026-05-02-sigcon-i3-execution-log.md
git log --oneline -1
```

**Criterio de cierre:** Ambos archivos commiteados en `feat/sigcon-i3`.

---

### Task 2 — Oracle DDL I3

**Objetivo:** Extender schema Oracle con metadatos de PDF e inmutabilidad, y crear tabla de notificaciones.

**Archivos a modificar:**
- `db/00_setup.sql` — agregar seccion `===== INCREMENTO 3 =====`
- `db/01_datos_prueba.sql` — agregar notificaciones de prueba

**DDL a agregar en `00_setup.sql`:**

```sql
-- ===== INCREMENTO 3 =====
-- 1. Extender SGCN_INFORMES con metadatos PDF
ALTER TABLE SGCN_INFORMES ADD (
    PDF_GENERADO_AT  TIMESTAMP,
    PDF_HASH         VARCHAR2(128)
);
CREATE INDEX IDX_INFORMES_PDF_GENERADO ON SGCN_INFORMES(PDF_GENERADO_AT);

-- 2. Tabla SGCN_NOTIFICACIONES (exacta segun spec §3.2)
CREATE SEQUENCE SGCN_NOTIFICACIONES_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE SGCN_NOTIFICACIONES (
    ID              NUMBER          DEFAULT SGCN_NOTIFICACIONES_SEQ.NEXTVAL PRIMARY KEY,
    ID_USUARIO      NUMBER          NOT NULL,
    TITULO          VARCHAR2(200)   NOT NULL,
    DESCRIPCION     VARCHAR2(1000)  NOT NULL,
    TIPO_EVENTO     VARCHAR2(50)    NOT NULL,
    ID_INFORME      NUMBER,
    LEIDA           NUMBER(1)       DEFAULT 0 NOT NULL,
    FECHA           TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY      VARCHAR2(200),
    UPDATED_AT      TIMESTAMP,
    CONSTRAINT FK_NOTIFICACIONES_USUARIO FOREIGN KEY (ID_USUARIO) REFERENCES SGCN_USUARIOS(ID),
    CONSTRAINT FK_NOTIFICACIONES_INFORME FOREIGN KEY (ID_INFORME) REFERENCES SGCN_INFORMES(ID),
    CONSTRAINT CHK_NOTIFICACIONES_LEIDA CHECK (LEIDA IN (0,1)),
    CONSTRAINT CHK_NOTIFICACIONES_EVENTO CHECK (TIPO_EVENTO IN (
        'INFORME_ENVIADO','REVISION_APROBADA','REVISION_DEVUELTA',
        'INFORME_APROBADO','INFORME_DEVUELTO'
    ))
);
CREATE INDEX IDX_NOTIFICACIONES_USUARIO ON SGCN_NOTIFICACIONES(ID_USUARIO);
CREATE INDEX IDX_NOTIFICACIONES_LEIDA   ON SGCN_NOTIFICACIONES(LEIDA);

CREATE OR REPLACE TRIGGER TRG_NOTIFICACIONES_AUDIT
BEFORE UPDATE ON SGCN_NOTIFICACIONES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

**Seed en `01_datos_prueba.sql`:** agregar 2 notificaciones de prueba contra el informe existente (ID 1) para el contratista (ID 2) y el revisor (ID 3).

**Validacion:**
```powershell
Select-String -Path db/00_setup.sql -Pattern "SGCN_NOTIFICACIONES|PDF_GENERADO_AT|PDF_HASH|TRG_NOTIFICACIONES_AUDIT"
Select-String -Path db/01_datos_prueba.sql -Pattern "SGCN_NOTIFICACIONES"
# Ninguna referencia a tablas fuera de scope I3
Select-String -Path db/00_setup.sql -Pattern "SGCN_PAGOS|SGCN_SECOP" | Should -BeNullOrEmpty
```

**Criterio de cierre:** DDL correcto, triggers presentes, 0 tablas fuera de scope, commit.

---

### Task 3 — Backend Domain I3

**Objetivo:** Crear entidad `Notificacion`, enum `TipoEvento`, repositorio, y actualizar `Informe` con campos `pdfGeneradoAt` y `pdfHash`.

**Archivos a crear:**
- `domain/enums/TipoEvento.java`
- `domain/entity/Notificacion.java`
- `domain/repository/NotificacionRepository.java`

**Archivos a modificar:**
- `domain/entity/Informe.java` — agregar `pdfGeneradoAt` y `pdfHash` (nullable)

**`TipoEvento` valores:** `INFORME_ENVIADO`, `REVISION_APROBADA`, `REVISION_DEVUELTA`, `INFORME_APROBADO`, `INFORME_DEVUELTO`.

**`Notificacion` campos:**
```java
@Id @GeneratedValue(strategy=SEQUENCE, generator="SGCN_NOTIFICACIONES_SEQ")
Long id;
@ManyToOne @JoinColumn(name="ID_USUARIO") Usuario usuario;
String titulo;           // VARCHAR2(200)
String descripcion;      // VARCHAR2(1000)
@Enumerated(STRING) TipoEvento tipoEvento;
@ManyToOne @JoinColumn(name="ID_INFORME") Informe informe; // nullable
boolean leida;           // DEFAULT false
@Column(insertable=false, updatable=false) LocalDateTime fecha; // SYSTIMESTAMP DEFAULT
// @CreatedDate / @LastModifiedDate / @CreatedBy para createdAt, updatedAt, createdBy
```

**`NotificacionRepository` firmas:**
```java
List<Notificacion> findByUsuarioAndLeidaFalseOrderByFechaDesc(Usuario usuario);
Page<Notificacion>  findByUsuarioOrderByFechaDesc(Usuario usuario, Pageable pageable);
long countByUsuarioAndLeidaFalse(Usuario usuario);
Optional<Notificacion> findByIdAndUsuario(Long id, Usuario usuario);
```

**Test a crear:** `test/.../domain/NotificacionDomainMappingTest.java` — 4 tests: mapeo a tabla, secuencia, FK usuario obligatoria, tipoEvento enum.

**Validacion:**
```powershell
cd sigcon-backend
mvn test -Dtest=NotificacionDomainMappingTest
mvn test
```

Resultado esperado: todos los tests existentes siguen pasando + nuevos.

---

### Task 4 — Backend PDF Service

**Objetivo:** Implementar generacion de PDF institucional con firmas incrustadas.

**Dependencias (agregar en `pom.xml`):**
```xml
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>1.3.35</version>
</dependency>
<dependency>
  <groupId>org.xhtmlrenderer</groupId>
  <artifactId>flying-saucer-pdf-openpdf</artifactId>
  <version>9.1.22</version>
</dependency>
```

**Archivos a crear:**
- `application/service/InformePdfTemplateService.java`
- `application/service/PdfInformeService.java`

**`InformePdfTemplateService`:** construye el HTML/XHTML del informe. Dependencias: `Informe`, datos del contrato, actividades, soportes, documentos, firmas (base64 desde `firmaImagen`). Retorna `byte[]` del PDF via Flying Saucer.

Estructura del HTML del informe (basada en DOCX de referencia):
1. Encabezado institucional SED (logo placeholder + datos entidad)
2. Datos del contrato: numero, objeto, contratista, supervisor, vigencia
3. Periodo del informe
4. Tabla de obligaciones + actividades + porcentaje de avance
5. Soportes referenciados (tipo URL o ARCHIVO)
6. Documentos adicionales
7. Seccion de firmas: imagen contratista + nombre/cargo/fecha; imagen supervisor + nombre/cargo/fecha
8. Footer: numero de informe, fecha de generacion, estado APROBADO

**`PdfInformeService`:** coordina generacion, almacenamiento y metadatos.
```java
// Metodo principal - llamado por InformeEstadoService.aprobar()
public void generarYPersistir(Informe informe)
    throws SigconBusinessException;
    // 1. Validar firmaImagen contratista != null -> FIRMA_REQUERIDA
    // 2. Validar firmaImagen supervisor != null -> FIRMA_REQUERIDA
    // 3. Si informe.pdfRuta != null -> no regenerar (inmutable)
    // 4. Llamar InformePdfTemplateService -> byte[] pdfBytes
    // 5. Calcular SHA-256 del pdf -> String hash
    // 6. Almacenar via DocumentStorageService.storeFile("pdfs/{idContrato}/{id}", file)
    // 7. Setear informe.pdfRuta, pdfGeneradoAt=now, pdfHash
    // 8. Si cualquier paso falla -> PDF_GENERACION_FALLIDA
```

**Errores nuevos** (agregar a `ErrorCode`):
- `FIRMA_REQUERIDA`
- `PDF_GENERACION_FALLIDA`
- `PDF_NO_DISPONIBLE`

**Tests a crear:** `PdfInformeServiceTest.java`
- `firmaContratistaAusenteBloquea()` → `FIRMA_REQUERIDA`
- `firmaSupervisorAusenteBloquea()` → `FIRMA_REQUERIDA`
- `pdfExistenteNoSeRegenera()` → no llama a `InformePdfTemplateService`
- `generacionExitosaSetaRutaYHash()` → verifica que `pdfRuta`, `pdfGeneradoAt` y `pdfHash` se setean
- `errorEnStorageLanzaPdfGeneracionFallida()` → `PDF_GENERACION_FALLIDA`

**Validacion:**
```powershell
cd sigcon-backend
mvn test -Dtest=PdfInformeServiceTest
mvn test
```

---

### Task 5 — Backend Notificaciones + Email

**Objetivo:** Implementar notificaciones in-app y envio/simulacion de email.

**Archivos a crear:**
- `application/service/NotificacionService.java`
- `application/service/EmailNotificacionService.java`
- `application/service/EventoInformeService.java`
- `application/dto/notificacion/NotificacionDto.java`
- `application/dto/notificacion/NotificacionResumenDto.java`
- `application/mapper/NotificacionMapper.java`
- `application/config/MailProperties.java` (o `@ConfigurationProperties("sigcon.mail")`)

**`NotificacionService`:**
```java
Notificacion crear(Usuario usuario, TipoEvento evento, Informe informe, String descripcion);
Page<NotificacionDto> listarPorUsuario(Pageable pageable);
long contarNoLeidas();
NotificacionDto marcarLeida(Long id);
void marcarTodasLeidas();
```

**`EmailNotificacionService`:**
- Si `sigcon.mail.enabled=false` → `log.info("EMAIL SIMULADO evento={} para={}", ...)` y retornar.
- Si `enabled=true` → POST a Microsoft Graph `/v1.0/users/{from}/sendMail` con Bearer token (client credentials flow).
- Errores de envio → `log.error(...)` sin revertir aprobacion.

**`EventoInformeService`:** Centraliza efectos secundarios por evento de transicion.
```java
public void publicar(TipoEvento evento, Informe informe, String observacion);
// Determina destinatario segun evento, crea notificacion y dispara email
```

Tabla de routing (segun spec §4.4):
| Evento | Destinatario |
|--------|-------------|
| `INFORME_ENVIADO` | `informe.contrato.revisor` |
| `REVISION_APROBADA` | `informe.contrato.supervisor` |
| `REVISION_DEVUELTA` | `informe.contrato.contratista` |
| `INFORME_APROBADO` | `informe.contrato.contratista` |
| `INFORME_DEVUELTO` | `informe.contrato.contratista` |

**Configuracion (agregar a `application-local-dev.yml`):**
```yaml
sigcon:
  mail:
    enabled: false
    from: sigcon@educacionbogota.edu.co
    graph-api-base-url: https://graph.microsoft.com/v1.0
    tenant-id: ${AZURE_TENANT_ID:placeholder}
    client-id: ${MAIL_CLIENT_ID:placeholder}
    client-secret: ${MAIL_CLIENT_SECRET:placeholder}
```

**Tests a crear:** `NotificacionServiceTest.java`, `EventoInformeServiceTest.java`
- `crear_generaNotificacionConDatosCorrectos()`
- `contarNoLeidas_soloDelUsuarioActual()`
- `marcarLeida_soloDestinatario()` → `ACCESO_DENEGADO` si otro usuario
- `publicar_creaNotificacionYDispararaEmail()`
- `emailSimulado_noBloquea()` (local-dev)

**Validacion:**
```powershell
cd sigcon-backend
mvn test -Dtest=NotificacionServiceTest,EventoInformeServiceTest
mvn test
```

---

### Task 6 — Integracion Con InformeEstadoService + AuditorProvider

**Objetivo:** Conectar PDF y notificaciones a las transiciones de estado. Corregir auditorProvider.

**Archivos a modificar:**
- `application/service/InformeEstadoService.java` — inyectar `PdfInformeService` y `EventoInformeService`, enganchar en cada transicion.
- `application/config/JpaAuditingConfig.java` — reemplazar `"SYSTEM"` por email del principal autenticado.

**Orden transaccional en `aprobar()` (spec §6):**
1. Validar permisos y transicion.
2. `pdfInformeService.generarYPersistir(informe)` — puede lanzar `FIRMA_REQUERIDA` o `PDF_GENERACION_FALLIDA`.
3. Solo si PDF exitoso: setear `informe.estado = APROBADO`, `informe.fechaAprobacion`.
4. `informeRepository.save(informe)`.
5. `eventoInformeService.publicar(INFORME_APROBADO, informe, null)` — fallos de notificacion no revierten.

**Resto de transiciones:**
```
enviar()          → eventoInformeService.publicar(INFORME_ENVIADO, ...)
aprobarRevision() → eventoInformeService.publicar(REVISION_APROBADA, ...)
devolverRevision()→ eventoInformeService.publicar(REVISION_DEVUELTA, informe, observacion)
devolver()        → eventoInformeService.publicar(INFORME_DEVUELTO, informe, observacion)
```

**`JpaAuditingConfig` fix:**
```java
@Bean
public AuditorAware<String> auditorProvider() {
    return () -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String s && s.equals("anonymousUser")) {
            return Optional.of("SYSTEM");
        }
        return Optional.of(auth.getName()); // retorna el email
    };
}
```

**Tests a crear:** `InformeEstadoServiceI3Test.java`
- `aprobarConFirmasLlamaAlPdfService()` — verifica que `PdfInformeService.generarYPersistir` es invocado.
- `aprobarSinFirmaContratistaFalla()` → `FIRMA_REQUERIDA`.
- `aprobarConPdfFallidoNoPersiste()` → estado no cambia a `APROBADO`.
- `aprobarExitosoCreaNotificacion()` — verifica que `EventoInformeService.publicar(INFORME_APROBADO, ...)` es invocado.
- `enviarCreaNotificacionRevisor()` — `INFORME_ENVIADO` publicado.

**Validacion:**
```powershell
cd sigcon-backend
mvn test -Dtest=InformeEstadoServiceI3Test
mvn test
# Verificar que auditorProvider no retorna "SYSTEM" en contexto autenticado
```

---

### Task 7 — Backend Controllers + Security + Swagger

**Objetivo:** Exponer endpoints PDF y notificaciones. Actualizar `SigconBackendSecurityTest`.

**Archivos a crear:**
- `web/controller/InformePdfController.java` — `GET /api/informes/{id}/pdf`
- `web/controller/NotificacionController.java` — los 4 endpoints de spec §4.6

**`InformePdfController`:**
```java
@GetMapping("/api/informes/{id}/pdf")
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CONTRATISTA')")
ResponseEntity<Resource> descargarPdf(@PathVariable Long id);
// 1. Obtener informe via InformeService.obtener(id) con verificacion de acceso
// 2. Si informe.pdfRuta == null → PDF_NO_DISPONIBLE (404)
// 3. Leer bytes via DocumentStorageService.loadFile(informe.pdfRuta)
// 4. Retornar con Content-Type: application/pdf, Content-Disposition: attachment; filename=informe-{numero}.pdf
```

**`NotificacionController`:**
```java
GET    /api/notificaciones                 → Page<NotificacionDto>
GET    /api/notificaciones/no-leidas/count → { "count": N }
PATCH  /api/notificaciones/{id}/leida      → NotificacionDto
PATCH  /api/notificaciones/leidas          → 204 No Content
```

**`SecurityConfig` / `DevSecurityConfig`:** agregar `/api/notificaciones/**` como autenticado, `/api/informes/**/pdf` con regla de roles.

**`SigconBackendSecurityTest` actualizaciones:**
- Agregar `@MockBean NotificacionRepository notificacionRepository`.
- Actualizar `informesAreExposedInI2ButFutureNotificationsAreNot()` → `/api/notificaciones` ahora retorna `200` (no `404`). Renombrar y ajustar aserciones.
- Agregar test `pdfRequiereRolAutorizado()` — REVISOR no puede descargar PDF.
- Agregar test `notificacionesSoloParaAutenticados()`.

**Swagger:** agregar `@Tag(name="PDF")` y `@Tag(name="Notificaciones")` en los controllers.

**`ErrorCode` (agregar si no existen):**
```
PDF_NO_DISPONIBLE
PDF_GENERACION_FALLIDA
FIRMA_REQUERIDA
NOTIFICACION_NO_ENCONTRADA
EMAIL_NO_ENVIADO
```

**Validacion:**
```powershell
cd sigcon-backend
mvn test
# Todos los tests incluyendo SecurityTest deben pasar
mvn package -DskipTests
```

---

### Task 8 — Frontend Models + Services I3

**Objetivo:** Agregar tipos TypeScript y servicios Angular para PDF y notificaciones.

**Archivos a crear en `sigcon-angular/src/app/core/`:**
- `models/notificacion.model.ts`
- `services/notificacion.service.ts`
- `services/pdf-informe.service.ts`

**`notificacion.model.ts`:**
```typescript
export interface Notificacion {
  id: number;
  titulo: string;
  descripcion: string;
  tipoEvento: 'INFORME_ENVIADO' | 'REVISION_APROBADA' | 'REVISION_DEVUELTA' | 'INFORME_APROBADO' | 'INFORME_DEVUELTO';
  idInforme: number | null;
  leida: boolean;
  fecha: string; // ISO datetime
}
export interface NotificacionesCount { count: number; }
```

**`notificacion.service.ts`:**
```typescript
listar(page: number, size: number): Observable<Page<Notificacion>>;
contarNoLeidas(): Observable<NotificacionesCount>;
marcarLeida(id: number): Observable<Notificacion>;
marcarTodasLeidas(): Observable<void>;
// Polling cada 30s en local-dev
```

**`pdf-informe.service.ts`:**
```typescript
descargar(idInforme: number): Observable<Blob>;
// Usa responseType: 'blob', Content-Disposition attachment
```

**Agregar a `informe.model.ts`:**
```typescript
pdfRuta?: string;
pdfGeneradoAt?: string;
pdfHash?: string;
```

**Tests:** specs Jasmine para `NotificacionService` (3 tests: listar, contar, marcar leida) y `PdfInformeService` (1 test: descarga retorna Blob).

**Validacion:**
```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

---

### Task 9 — Frontend Campana + Centro De Notificaciones

**Objetivo:** Implementar badge de notificaciones en topbar y pantalla de centro de notificaciones.

**Archivos a crear:**
- `features/notificaciones/notificaciones-menu/notificaciones-menu.component.ts` — campana con badge
- `features/notificaciones/centro-notificaciones/centro-notificaciones.component.ts` — lista paginada

**`notificaciones-menu.component`:**
- Icono campana PrimeNG (`pi pi-bell`).
- Badge con count de no leidas (polling 30s via `NotificacionService.contarNoLeidas()`).
- Click abre overlay panel con ultimas 5 notificaciones y enlace "Ver todas".
- Marcar leida al hacer click en una notificacion del menu.
- `count = 0` → badge oculto.

**`centro-notificaciones.component`:**
- Lista paginada de todas las notificaciones del usuario.
- Columnas: icono de tipo, titulo, descripcion, fecha relativa, estado (leida/no leida).
- Click en fila → marcarLeida, si `idInforme != null` → navegar a `/informes/{idInforme}`.
- Boton "Marcar todas como leidas".
- Notificaciones no leidas con fondo distinto (PrimeNG `p-tag` o clase CSS).

**Integrar en `topbar.component.ts`:** agregar `<app-notificaciones-menu />` junto al icono de perfil.

**Ruta nueva en `app.routes.ts`:**
```typescript
{ path: 'notificaciones', loadComponent: () => import(...CentroNotificacionesComponent) }
```

**Prototipo de referencia:** `centro_de_notificaciones_sigcon` (docs/Prototipo/DESIGN.md).

**Tests:** 4 specs — badge se muestra con count > 0, badge oculto con count = 0, click marca leida, navegacion a informe.

**Validacion:**
```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

---

### Task 10 — Frontend Visor PDF + Advertencia Firma En Perfil

**Objetivo:** Agregar visor/descarga PDF y mostrar advertencia de firma faltante en perfil.

**Archivos a crear:**
- `features/informes/visor-pdf/visor-pdf.component.ts` — vista de descarga PDF

**Archivos a modificar:**
- `features/perfil/perfil.component.ts` — agregar advertencia si `firmaImagen` es nulo para CONTRATISTA o SUPERVISOR

**`visor-pdf.component`:**
- Ruta: `/informes/:id/pdf`
- Al cargar: llamar `PdfInformeService.descargar(idInforme)`.
- Si exito: boton "Descargar PDF" que dispara la descarga via `URL.createObjectURL(blob)`.
- Si error (PDF_NO_DISPONIBLE): mostrar mensaje institucional "El PDF no esta disponible. El informe debe estar en estado APROBADO." Sin boton de reintento.
- Usar `InformeService.obtener(id)` para mostrar metadatos del informe (numero, contrato, fecha aprobacion).

**Activar en `informe-detalle.component`:** Si `informe.estado === 'APROBADO'` y `informe.pdfRuta`, mostrar boton "Ver / Descargar PDF" → navegar a `/informes/{id}/pdf`.

**`perfil.component` advertencia firma:**
```typescript
// Si usuario.rol === 'CONTRATISTA' o 'SUPERVISOR' y !usuario.firmaImagen
// Mostrar PrimeNG Message (p-message) severity="warn"
// Texto: "Para que los informes puedan ser aprobados, debe cargar su firma digital."
```

**Prototipo de referencia:** `visor_de_reporte_aprobado_pdf_sigcon`, `mi_perfil_y_firma_sigcon`.

**Ruta nueva en `app.routes.ts`:**
```typescript
{ path: 'informes/:id/pdf', loadComponent: () => import(...VisorPdfComponent) }
```

**Tests:** 4 specs — visor muestra boton descarga, visor muestra mensaje si no disponible, perfil muestra advertencia si sin firma, perfil no muestra advertencia si firma presente.

**Validacion:**
```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

---

### Task 11 — Verificacion E2E + Documentacion

**Objetivo:** Verificacion integral I1 + I2 + I3. Actualizar `ARRANQUE.md`.

**Checklist de verificacion:**

Backend:
```powershell
cd sigcon-backend
mvn test
# Resultado esperado: ≥ 80 tests, 0 fallas, 0 errores
mvn package -DskipTests
ls target/*.war
```

Frontend:
```powershell
cd sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
# Resultado esperado: ≥ 70 specs, 0 fallas
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
ls dist/
```

Verificacion de scope (no debe aparecer nada fuera de I3):
```powershell
# En backend: no hay SECOP, motor de pagos, firma criptografica avanzada
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|PKCS|CAdES|XAdES"
# En frontend: no hay rutas no planeadas
Select-String -Path sigcon-angular\src\app\app.routes.ts -Pattern "path:" | Format-Table
```

Verificacion de seams hacia I3+ (todo debe estar inmutable):
```powershell
# PDF inmutable: pdfRuta no se sobrescribe si ya existe
# AuditorProvider retorna email real, no "SYSTEM"
Select-String -Path sigcon-backend\src\main\java -Recurse -Pattern "\"SYSTEM\"" | Where-Object { $_.Line -notmatch "//|/\*" }
```

**Actualizacion `docs/ARRANQUE.md`:** agregar seccion:
```markdown
## Configuracion I3 — PDF y Email

### Local-dev
sigcon.mail.enabled=false → envios simulados en logs. No requiere credenciales Azure.
PDF generado en ${java.io.tmpdir}/sigcon-test/pdfs/

### WebLogic
Configurar variables de entorno:
- MAIL_FROM, AZURE_TENANT_ID, MAIL_CLIENT_ID, MAIL_CLIENT_SECRET
- sigcon.storage.signatures-path=<ruta compartida>
- sigcon.mail.enabled=true
```

**Criterio de cierre:** 0 tests rojos, WAR generado, build Angular OK, ARRANQUE.md actualizado, execution log con todos los SHAs de commits.

---

## Restricciones De Alcance I3

No se debe implementar:
- Firma digital criptografica avanzada (PKCS#11, CAdES, XAdES).
- Radicacion oficial externa.
- Motor de pagos.
- Integracion SECOP2.
- Nuevos tipos contractuales.
- Nuevas transiciones de estado de informe.
- Panel de administracion de notificaciones.

Validacion de alcance al cerrar cada task con codigo nuevo:
```powershell
Get-ChildItem -Path sigcon-backend\src\main\java -Recurse -File | Select-String -Pattern "SECOP|MotorPagos|radicacion|PKcs11|firma.criptografica"
Get-ChildItem -Path sigcon-angular\src\app -Recurse -File | Select-String -Pattern "secop|motor-pagos"
```

---

## Dependencias Entre Tareas

```
Task 1 → (inicio, independiente)
Task 2 → independiente del codigo (solo SQL)
Task 3 → requiere Task 2 (nombres de tablas y secuencias)
Task 4 → requiere Task 3 (Informe.pdfRuta, pdfGeneradoAt, pdfHash)
Task 5 → requiere Task 3 (entidad Notificacion, TipoEvento)
Task 6 → requiere Task 4 y Task 5 (PdfInformeService + EventoInformeService)
Task 7 → requiere Task 5 y Task 6 (controllers consumen servicios)
Task 8 → puede avanzar en paralelo con Task 3-6 (solo frontend models)
Task 9 → requiere Task 8
Task 10 → requiere Task 8
Task 11 → requiere todas las demas
```

Orden recomendado para modelo unico: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11.
Para trabajo paralelo: backend (2-7) y frontend (8-10) pueden solaparse despues de Task 3.

---

## Notas Para Modelos Que Tomen Tareas

- **Siempre hacer `git pull --ff-only origin feat/sigcon-i3` antes de iniciar**.
- Verificar tabla "Estado De Tareas I3" en el execution log antes de tomar una tarea.
- Task 7 **debe** actualizar `SigconBackendSecurityTest` — no dejar el test roto.
- El `@MockBean NotificacionRepository` falta en `SigconBackendSecurityTest`; Task 7 lo agrega.
- Email simulado: en `local-dev` nunca debe fallar el test por intentar conectar a Graph API real.
- PDF hash: `MessageDigest.getInstance("SHA-256")` — no requiere libreria externa.
- `DocumentStorageService.loadFile(String path)` puede no existir aun — si no existe, Task 7 debe agregar ese metodo a la interfaz e implementarlo en `LocalDocumentStorageService`.

---

*Plan promovido desde outline el 2026-05-02. Ajustes post-I2 incorporados. Rama `feat/sigcon-i3` activa desde commit `0658cef`.*
