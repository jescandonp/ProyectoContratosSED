# Spec Tecnica - SIGCON Incremento 3
## Completitud: PDF, Firmas y Notificaciones

> **Metodologia:** Spec-Driven Development (SDD) - Spec-Anchored  
> **Version:** 1.0 - **Fecha:** 2026-05-01  
> **Constitucion:** `docs/superpowers/CONSTITUTION.md`  
> **Arquitectura:** `ARCHITECTURE.md`  
> **PRD de referencia:** `docs/superpowers/specs/2026-04-30-sigcon-prd.md`  
> **Specs base:** `docs/superpowers/specs/2026-04-30-sigcon-i1-spec.md`, `docs/superpowers/specs/2026-05-01-sigcon-i2-spec.md`  
> **Formato institucional:** `Notas_ProyectoContratos/06_Informe_actividades_06_Abril_2026_Juan_Escandon.docx`  
> **Design System:** `Prototipo/DESIGN.md`  
> **Estado:** Listo para revision

---

## 1. Alcance Del Incremento

### Modulos incluidos

| Modulo | Descripcion | Rol principal |
|--------|-------------|---------------|
| M7 - Generacion de PDF | Generar PDF institucional al aprobar informe | SUPERVISOR / Sistema |
| M7 - Firmas | Incrustar firma del contratista y supervisor | CONTRATISTA / SUPERVISOR |
| M8 - Notificaciones in-app | Crear y consultar notificaciones internas | Todos |
| M8 - Notificaciones email | Enviar correos por eventos del flujo | Sistema |

### Fuera de este incremento

- Modificacion de estados de informe.
- Nuevos tipos contractuales.
- Integracion SECOP2.
- Firma digital criptografica avanzada.
- Radicacion oficial externa.
- Motor de pagos.

### Entregable de cierre

Al aprobar un informe, el sistema genera un PDF institucional inmutable, lo deja disponible para descarga por roles autorizados y genera notificaciones in-app/email para los actores definidos por el PRD.

---

## 2. Decisiones De Coherencia Con I2

- La maquina de estados se mantiene igual que I2.
- `EN_REVISION -> APROBADO` sigue siendo la transicion final del supervisor.
- I3 agrega efectos secundarios obligatorios a esa transicion: generar PDF y notificar.
- Si la generacion de PDF falla, la transicion a `APROBADO` no debe confirmarse.
- El PDF aprobado es inmutable: no se regenera ni se reemplaza desde la UI.
- Notificaciones fallidas no revierten la aprobacion si el PDF ya fue generado; quedan registradas para seguimiento tecnico.

---

## 3. Base De Datos - DDL Incremento 3

### 3.1 Ajuste A SGCN_INFORMES

I2 ya prepara `PDF_RUTA` como campo nullable. I3 agrega metadatos de inmutabilidad:

```sql
ALTER TABLE SGCN_INFORMES ADD (
    PDF_GENERADO_AT       TIMESTAMP,
    PDF_HASH              VARCHAR2(128)
);

CREATE INDEX IDX_INFORMES_PDF_GENERADO ON SGCN_INFORMES(PDF_GENERADO_AT);
```

Regla:

- Para informes aprobados bajo I3, `PDF_RUTA`, `PDF_GENERADO_AT` y `PDF_HASH` deben quedar diligenciados.

### 3.2 Tabla SGCN_NOTIFICACIONES

```sql
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
        'INFORME_ENVIADO',
        'REVISION_APROBADA',
        'REVISION_DEVUELTA',
        'INFORME_APROBADO',
        'INFORME_DEVUELTO'
    ))
);

CREATE INDEX IDX_NOTIFICACIONES_USUARIO ON SGCN_NOTIFICACIONES(ID_USUARIO);
CREATE INDEX IDX_NOTIFICACIONES_LEIDA   ON SGCN_NOTIFICACIONES(LEIDA);

CREATE OR REPLACE TRIGGER TRG_NOTIFICACIONES_AUDIT
BEFORE UPDATE ON SGCN_NOTIFICACIONES FOR EACH ROW
BEGIN :NEW.UPDATED_AT := SYSTIMESTAMP; END;
/
```

### 3.3 Auditoria De Email

La auditoria detallada de envios de email no es funcionalidad visible del MVP. Para I3, el backend debe registrar errores de envio en logs con contexto suficiente: tipo de evento, idInforme, destinatario y causa.

---

## 4. Backend - Spring Boot

### 4.1 Servicios nuevos

```text
application/service/
├── PdfInformeService
├── InformePdfTemplateService
├── NotificacionService
├── EmailNotificacionService
└── EventoInformeService
```

Responsabilidades:

- `PdfInformeService`: coordina generacion, almacenamiento, hash y actualizacion del informe.
- `InformePdfTemplateService`: construye el contenido visual del PDF desde el informe aprobado.
- `NotificacionService`: crea, lista y marca notificaciones in-app.
- `EmailNotificacionService`: envia correos usando configuracion Office 365.
- `EventoInformeService`: centraliza efectos secundarios de transiciones de I2.

### 4.2 Generacion De PDF

Contenido obligatorio:

- Encabezado institucional SED.
- Datos del contrato: numero, objeto, contratista, supervisor, vigencia.
- Periodo del informe.
- Tabla de obligaciones con actividades y porcentaje.
- Listado de soportes referenciados.
- Listado de documentos adicionales.
- Firma del contratista.
- Firma del supervisor.
- Nombre, cargo y fecha de cada firmante.
- Metadata: numero de informe, fecha de generacion, estado `APROBADO`.

Reglas:

- El template se basa en `Notas_ProyectoContratos/06_Informe_actividades_06_Abril_2026_Juan_Escandon.docx`.
- La salida final es PDF, no DOCX.
- El archivo se guarda mediante el almacenamiento configurable existente.
- El hash se calcula sobre bytes finales del PDF.
- Una vez `PDF_RUTA` esta definida, no se permite regenerar desde endpoints funcionales.

### 4.3 Firmas

- La firma del contratista usa `SGCN_USUARIOS.FIRMA_IMAGEN` del contratista.
- La firma del supervisor usa `SGCN_USUARIOS.FIRMA_IMAGEN` del supervisor.
- Si falta firma del contratista o supervisor, la aprobacion final falla con `FIRMA_REQUERIDA`.
- Las imagenes aceptadas siguen la regla I1: JPG/PNG.

### 4.4 Notificaciones In-App

Eventos:

| Evento | Destinatario | Titulo |
|--------|--------------|--------|
| `INFORME_ENVIADO` | Revisor asignado | Informe enviado para revision |
| `REVISION_APROBADA` | Supervisor asignado | Informe listo para aprobacion |
| `REVISION_DEVUELTA` | Contratista | Informe devuelto por revision |
| `INFORME_APROBADO` | Contratista | Informe aprobado |
| `INFORME_DEVUELTO` | Contratista | Informe devuelto por supervisor |

Campos de descripcion:

- nombre del contratista
- numero de contrato
- periodo del informe
- observacion cuando aplique

### 4.5 Email Office 365

Configuracion:

```yaml
sigcon:
  mail:
    enabled: true
    from: ${MAIL_FROM}
    graph-api-base-url: https://graph.microsoft.com/v1.0
    tenant-id: ${AZURE_TENANT_ID}
    client-id: ${MAIL_CLIENT_ID}
    client-secret: ${MAIL_CLIENT_SECRET}
```

Reglas:

- En `local-dev`, `sigcon.mail.enabled` puede ser `false`; el sistema registra el email simulado en logs.
- En `weblogic`, el envio usa Microsoft Graph / Office 365 con credenciales de aplicacion.
- El cuerpo del correo es HTML simple, institucional y sin adjuntos.
- El PDF no se adjunta en email; se referencia la accion dentro del sistema.

### 4.6 Controllers

```text
web/controller/
├── InformePdfController       @RequestMapping("/api/informes/{id}/pdf")
└── NotificacionController     @RequestMapping("/api/notificaciones")
```

Endpoints:

```text
GET   /api/informes/{id}/pdf              [CONTRATISTA, SUPERVISOR, ADMIN]
GET   /api/notificaciones                 [autenticado]
GET   /api/notificaciones/no-leidas/count [autenticado]
PATCH /api/notificaciones/{id}/leida      [destinatario]
PATCH /api/notificaciones/leidas          [autenticado]
```

Autorizacion PDF:

- Contratista descarga solo PDF de sus informes aprobados.
- Supervisor descarga PDF de sus contratos supervisados.
- Admin descarga cualquier PDF aprobado.
- Revisor no descarga PDF salvo que tambien tenga rol autorizado en una regla futura.

### 4.7 Codigos De Error Nuevos

```text
PDF_NO_DISPONIBLE
PDF_GENERACION_FALLIDA
FIRMA_REQUERIDA
NOTIFICACION_NO_ENCONTRADA
EMAIL_NO_ENVIADO
```

---

## 5. Frontend - Angular 20

### 5.1 Estructura

```text
src/app/features/notificaciones/
├── centro-notificaciones/
└── notificaciones-menu/

src/app/features/informes/
└── visor-pdf/
```

### 5.2 Rutas

```typescript
{ path: 'notificaciones', loadComponent: ... }
{ path: 'informes/:id/pdf', loadComponent: ... }
```

### 5.3 Pantallas De Referencia

| Pantalla prototipo | Modulo | Ruta Angular |
|--------------------|--------|-------------|
| `centro_de_notificaciones_sigcon` | M8 | `/notificaciones` |
| `visor_de_reporte_aprobado_pdf_sigcon` | M7 | `/informes/:id/pdf` |
| `mi_perfil_y_firma_sigcon` | M7 | `/perfil` |

### 5.4 Comportamiento UX

- Topbar muestra campana con contador de no leidas.
- Click en campana abre menu breve y acceso al centro de notificaciones.
- Centro de notificaciones lista titulo, descripcion, fecha y estado leida/no leida.
- Al abrir una notificacion, se marca como leida.
- Visor PDF permite ver o descargar el PDF aprobado.
- Si el PDF no existe, mostrar mensaje institucional y no intentar regenerar.
- Perfil debe advertir si falta firma para usuarios `CONTRATISTA` o `SUPERVISOR`.

---

## 6. Integracion Con Flujo I2

Actualizar `InformeEstadoService`:

- En `BORRADOR -> ENVIADO`: crear notificacion/email para revisor.
- En `ENVIADO -> EN_REVISION`: crear notificacion/email para supervisor.
- En `ENVIADO -> DEVUELTO`: crear notificacion/email para contratista.
- En `EN_REVISION -> APROBADO`: generar PDF, luego crear notificacion/email para contratista.
- En `EN_REVISION -> DEVUELTO`: crear notificacion/email para contratista.

Orden transaccional recomendado:

1. Validar transicion y permisos.
2. Para aprobacion final, generar y almacenar PDF.
3. Persistir estado final y metadatos.
4. Crear notificacion in-app.
5. Enviar email o registrar simulacion local-dev.

---

## 7. Seguridad

- Notificaciones solo son visibles por destinatario.
- Contador de no leidas solo cuenta notificaciones del usuario autenticado.
- Marcar como leida requiere ser destinatario.
- PDF solo se descarga si el usuario tiene acceso al informe y el informe esta `APROBADO`.
- Archivos PDF se sirven desde backend, no como archivos publicos directos.

---

## 8. Criterios De Aceptacion

### Backend

- [ ] Aprobar informe genera PDF automaticamente.
- [ ] PDF incluye datos de contrato, periodo, obligaciones, soportes, documentos y firmas.
- [ ] Si falta firma de contratista o supervisor, la aprobacion final falla.
- [ ] PDF aprobado queda inmutable y no se regenera.
- [ ] Contratista, supervisor y admin pueden descargar PDF aprobado.
- [ ] Usuario no autorizado no descarga PDF.
- [ ] Cada transicion definida genera notificacion in-app al destinatario correcto.
- [ ] Contador de no leidas retorna solo notificaciones del usuario autenticado.
- [ ] Marcar notificacion como leida funciona solo para destinatario.
- [ ] En `local-dev`, email se simula sin bloquear el flujo.
- [ ] En `weblogic`, email usa configuracion Office 365.

### Frontend

- [ ] Topbar muestra badge con notificaciones no leidas.
- [ ] Centro de notificaciones muestra historial y estados leido/no leido.
- [ ] Click en notificacion marca como leida.
- [ ] Visor PDF muestra PDF aprobado y permite descarga.
- [ ] Perfil muestra advertencia si falta firma requerida.
- [ ] Mensajes de error son claros cuando PDF no esta disponible.

### General

- [ ] I1 e I2 siguen funcionando.
- [ ] Swagger documenta endpoints PDF/notificaciones.
- [ ] No se agregan modulos fuera del MVP.
- [ ] `ARRANQUE.md` documenta configuracion local-dev y weblogic de email.

---

## 9. Impacto Sobre I1/I2

- I1 perfil/firma pasa a ser prerequisito operativo para aprobacion final.
- I2 aprobacion final ahora depende de `PdfInformeService`.
- I2 colas no cambian de estado ni rutas por I3.
- El detalle de contrato puede habilitar descarga PDF solo cuando `estado=APROBADO` y `pdfRuta` existe.

---

*Spec tecnica generada mediante SDD Spec-Anchored - SIGCON - Incremento 3 - 2026-05-01.*
