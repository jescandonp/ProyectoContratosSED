# Spec Tecnica — SIGCON Incremento 7
## Usuario IVA, Documentos Requeridos, Email de Aprobacion y Busqueda Administrativa

> **Metodologia:** Spec-Driven Development (SDD) — Spec-Anchored
> **Version:** 1.0 — **Fecha:** 2026-05-11
> **Constitucion:** `docs/CONSTITUTION.md`
> **Arquitectura:** `docs/ARCHITECTURE.md`
> **PRD de referencia:** `docs/specs/2026-04-30-sigcon-prd.md`
> **Specs base:** I1-I6 completados
> **Rama:** `feat/sigcon-i7`
> **Feature name:** `usuario-iva-documentos-email-busqueda`
> **Estado:** Listo para planificacion e implementacion

---

## 1. Alcance del Incremento

### 1.1 Problema que resuelve

Las pruebas funcionales posteriores a I6 dejaron cinco necesidades:

1. Al crear o editar usuarios no siempre se presenta retroalimentacion visual de exito/error.
2. SIGCON no identifica si un contratista es responsable de IVA; por tanto no puede exigir FACTURA por informe cuando aplique.
3. La seccion de Documentos Requeridos no permite gestionar directamente los archivos exigidos por contrato/informe con carga, visualizacion previa y descarga.
4. Al aprobar un informe se requiere notificacion por email al contratista y a un correo administrador configurable.
5. El administrador necesita una busqueda global que combine contratistas, contratos e informes, con filtro por periodo de informe.

### 1.2 Modulos incluidos

| Modulo | Descripcion | Rol principal |
|--------|-------------|---------------|
| Estabilizacion T0 | Mantener `PORCENTAJE` con default `0`, asignarlo internamente al crear actividad y conservar reset local de informes | Sistema |
| Usuarios | Campo `responsableIva` en usuario contratista, default `No`; mensajes de confirmacion/error en crear/editar usuario | ADMIN |
| Documentos Requeridos | Carga, preview y descarga de documentos requeridos del informe; solo PDF y `.eml` | CONTRATISTA / ADMIN / REVISION |
| Factura IVA | Documento requerido dinamico `FACTURA` por cada informe cuando el contratista es responsable de IVA | CONTRATISTA |
| Email aprobacion | Notificar al contratista y a correo administrador configurable cuando el informe pasa a `APROBADO` | Sistema |
| Busqueda global admin | Busqueda por texto y rango de periodo de informe, con resultados agrupados por contratistas, contratos e informes | ADMIN |

### 1.3 Fuera de este incremento

- Facturacion electronica DIAN o validacion tributaria externa.
- Nuevos roles.
- Cambios a la maquina de estados del informe.
- Conversion de DOCX/XLSX u otros formatos para preview.
- Indexacion full-text avanzada o motor externo de busqueda.
- Configuracion de multiples correos administradores desde UI.
- Reintentos asincronos persistentes de email. I7 deja una interfaz extensible; la entrega minima puede registrar error sin bloquear la aprobacion.

### 1.4 Entregable de cierre

I7 queda cerrado cuando:

- Crear/editar usuario muestra confirmacion de exito y errores claros.
- Un usuario contratista tiene `responsableIva = No` por defecto y puede marcarse como `Si` desde administracion.
- Si el contratista es responsable de IVA, cada informe exige un documento requerido `FACTURA` antes de enviarse.
- La seccion Documentos Requeridos permite adjuntar, visualizar y descargar solo PDF y `.eml`.
- El preview `.eml` muestra asunto, remitente, fecha y cuerpo texto basico cuando sea posible; el archivo original siempre se puede descargar.
- Al aprobar un informe se dispara notificacion email al contratista y al correo administrador configurable.
- El administrador cuenta con busqueda global por contratista, contrato o informe, con rango de fechas aplicado al periodo del informe (`fechaInicio` / `fechaFin`).

---

## 2. Coherencia con Incrementos Anteriores

- I7 parte desde I6 cerrado. No cambia el PDF institucional, salvo que pueda incluir referencias a documentos requeridos ya existentes si el codigo actual lo hace.
- La maquina de estados se mantiene: `BORRADOR`, `ENVIADO`, `EN_REVISION`, `DEVUELTO`, `APROBADO`.
- La edicion de documentos requeridos se permite solo para informes `BORRADOR` o `DEVUELTO`.
- En estados `ENVIADO`, `EN_REVISION` y `APROBADO`, los documentos requeridos son solo lectura: visualizar y descargar.
- La regla de seguridad vigente permanece: un contratista no puede consultar informes ni documentos de otro contratista.
- La notificacion email de I7 complementa las notificaciones in-app existentes, no las reemplaza.
- Los documentos requeridos son diferentes de soportes de actividades y documentos adicionales libres. I7 limita la nueva carga/preview/descarga a la seccion "Documentos Requeridos".

---

## 3. Modelo de Datos

### 3.1 `SGCN_USUARIOS`

Agregar campo:

```sql
ALTER TABLE SGCN_USUARIOS ADD (
    RESPONSABLE_IVA NUMBER(1) DEFAULT 0 NOT NULL
);
```

Reglas:

- `0` = No responsable de IVA.
- `1` = Responsable de IVA.
- Default obligatorio: `0`.
- Se expone en DTOs de administracion de usuarios y en el detalle de usuario.
- Solo ADMIN puede modificarlo.

### 3.2 Documentos Requeridos del informe

I7 debe reutilizar el modelo existente de catalogo/documentos cuando sea suficiente. Si el modelo actual no tiene entidad para archivo requerido por informe, crear una entidad especifica:

`DocumentoRequeridoInforme`

Campos minimos:

- `id`
- `informe`
- `documentoCatalogo`
- `nombreArchivo`
- `contentType`
- `extension`
- `storagePath` o identificador usado por `DocumentStorageService`
- `tamanoBytes`
- `activo`
- auditoria existente (`createdAt`, `createdBy`, `updatedAt`)

Reglas:

- Un documento requerido puede estar pendiente o cargado.
- Solo se aceptan extensiones `.pdf` y `.eml`.
- Content types permitidos:
  - PDF: `application/pdf`
  - EML: `message/rfc822`, `application/octet-stream` solo si la extension validada es `.eml`
- Para cada informe de contratista responsable de IVA debe existir requerimiento dinamico `FACTURA`.
- `FACTURA` no depende de parametrizacion manual en catalogo. Si no existe registro catalogado, el backend debe exponerla como requerido dinamico del informe.

---

## 4. Backend

### 4.1 Usuario responsable de IVA

Extender:

- `Usuario`
- `UsuarioRequest` / request usado por administracion
- `UsuarioDto`
- `UsuarioService`
- tests de usuario

Validaciones:

- Si el request omite `responsableIva`, persistir `false`.
- Solo ADMIN puede crear/editar usuarios.
- El valor no aplica para roles no contratistas, pero puede persistirse en `false` sin error.

### 4.2 Documentos Requeridos

Endpoints propuestos:

```text
GET  /api/informes/{id}/documentos-requeridos
POST /api/informes/{id}/documentos-requeridos/{documentoId}/archivo
GET  /api/informes/{id}/documentos-requeridos/{documentoId}/archivo
GET  /api/informes/{id}/documentos-requeridos/{documentoId}/preview
DELETE /api/informes/{id}/documentos-requeridos/{documentoId}/archivo
```

Reglas por endpoint:

- `GET lista`: retorna documentos requeridos del catalogo del contrato/informe + `FACTURA` dinamica cuando aplique.
- `POST archivo`: solo CONTRATISTA propietario, solo `BORRADOR` o `DEVUELTO`, solo PDF/EML.
- `GET archivo`: roles con acceso al informe pueden descargar.
- `GET preview`: PDF retorna stream visualizable; EML retorna DTO con metadatos y cuerpo texto basico.
- `DELETE archivo`: solo CONTRATISTA propietario, solo `BORRADOR` o `DEVUELTO`.

Validacion antes de enviar informe:

- En `InformeEstadoService.enviar()` o servicio equivalente, verificar que todos los documentos requeridos esten cargados.
- Si falta `FACTURA` para responsable IVA, responder error de validacion claro.
- No exigir `FACTURA` si `responsableIva = false`.

### 4.3 Preview `.eml`

DTO minimo:

```java
class EmlPreviewDto {
    String asunto;
    String remitente;
    String destinatarios;
    String fecha;
    String cuerpoTexto;
    boolean previewParcial;
}
```

Reglas:

- Parsear cabeceras y cuerpo texto cuando sea posible con APIs JavaMail disponibles o libreria compatible Java 8 si ya existe dependencia razonable.
- Si el `.eml` es complejo, retornar metadatos disponibles y `previewParcial = true`.
- Nunca modificar el archivo original; siempre conservar descarga.

### 4.4 Email al aprobar informe

Configurar:

```yaml
sigcon:
  notifications:
    admin-email: ${SIGCON_ADMIN_NOTIFICATION_EMAIL:}
    email-enabled: ${SIGCON_EMAIL_ENABLED:false}
```

Servicio:

- `EmailNotificationService`
- Implementacion local/dev: si `email-enabled=false` o falta SMTP/admin email, registrar log estructurado sin bloquear.
- Implementacion real: usar `JavaMailSender` o infraestructura Spring Mail compatible Java 8.

Disparo:

- En transicion final a `APROBADO`, despues de persistir estado y PDF.
- Destinatarios: email del contratista + `sigcon.notifications.admin-email`.
- Contenido minimo: numero de informe, contrato, contratista, periodo, fecha de aprobacion, estado `APROBADO`.
- Si falla el email, la aprobacion no se revierte; se registra error para soporte.

### 4.5 Busqueda global administrativa

Endpoint:

```text
GET /api/admin/busqueda?q=&fechaInicio=&fechaFin=
```

Reglas:

- Solo ADMIN.
- `q` busca por:
  - contratista: nombre, documento, email
  - contrato: numero, objeto, estado, contratista
  - informe: numero, estado, contrato, contratista
- `fechaInicio`/`fechaFin` aplican sobre periodo del informe:
  - informe cruza rango si `informe.fechaInicio <= fechaFin` y `informe.fechaFin >= fechaInicio`.
- Respuesta agrupada:

```java
class BusquedaAdminResponse {
    List<ContratistaResultadoDto> contratistas;
    List<ContratoResultadoDto> contratos;
    List<InformeResultadoDto> informes;
}
```

---

## 5. Frontend

### 5.1 Mensajes en administracion de usuarios

En `AdminUsuariosComponent`:

- Al crear usuario exitosamente: mostrar "Usuario creado correctamente."
- Al actualizar usuario exitosamente: mostrar "Usuario actualizado correctamente."
- Si falla backend: mostrar mensaje de error con detalle util cuando exista.
- Mantener el patron visual existente de alerts/toasts del proyecto.

### 5.2 Responsable IVA en usuario

En formulario de usuario:

- Campo booleano "Responsable de IVA".
- Default visual y request: `No`.
- Solo visible/editable para ADMIN.

### 5.3 Documentos Requeridos en informe

En detalle/formulario de informe:

- Seccion "Documentos Requeridos".
- Estados `BORRADOR`/`DEVUELTO`: cargar, reemplazar, visualizar y descargar.
- Estados `ENVIADO`/`EN_REVISION`/`APROBADO`: visualizar y descargar.
- Mostrar estado por documento: pendiente / cargado / requerido por IVA.
- Para responsable IVA, `FACTURA` aparece como requerido.
- Validar extension antes de enviar al backend; backend conserva la validacion definitiva.

Preview:

- PDF: abrir en visor embebido o nueva vista segura.
- EML: mostrar asunto, remitente, fecha y cuerpo texto basico; ofrecer descarga del `.eml`.

### 5.4 Busqueda global admin

Nueva ruta sugerida:

```text
/admin/busqueda
```

UI:

- Input de texto libre.
- Rango de fechas para periodo del informe.
- Boton buscar.
- Resultados agrupados en tres secciones: Contratistas, Contratos, Informes.
- Acciones de navegacion al detalle correspondiente.

---

## 6. Seguridad

- Solo ADMIN accede a busqueda global.
- Solo ADMIN crea/edita `responsableIva`.
- Contratista solo puede cargar/eliminar documentos requeridos de sus propios informes en `BORRADOR` o `DEVUELTO`.
- Revisor, supervisor y admin pueden visualizar/descargar documentos requeridos si ya tienen acceso al informe.
- Validar ownership en backend, no solo en frontend.
- Validar extension y content type en backend.

---

## 7. Tests Requeridos

Backend:

- Usuario default `responsableIva = false`.
- Admin puede crear/editar usuario responsable IVA.
- Contratista responsable IVA requiere `FACTURA` antes de enviar informe.
- Contratista no responsable IVA no requiere `FACTURA`.
- Upload PDF requerido exitoso.
- Upload `.eml` requerido exitoso y preview basico.
- Upload extension no permitida falla.
- No propietario no puede cargar/descargar documentos de otro contratista.
- Email de aprobacion se invoca con contratista + admin configurable.
- Fallo de email no revierte aprobacion.
- Busqueda admin retorna grupos por contratista, contrato e informe.
- Rango de fechas filtra por periodo de informe.
- Usuario no admin no accede a busqueda.

Frontend:

- Admin usuarios muestra confirmacion al crear.
- Admin usuarios muestra confirmacion al editar.
- Formulario usuario default "Responsable de IVA: No".
- Documentos requeridos muestra `FACTURA` cuando el contratista es responsable IVA.
- Documentos requeridos permite seleccionar solo PDF/EML.
- EML preview muestra metadatos basicos.
- Busqueda admin envia `q`, `fechaInicio`, `fechaFin` y renderiza grupos.

---

## 8. Criterios de Aceptacion

### Usuarios

- [ ] Crear usuario muestra confirmacion de exito.
- [ ] Editar usuario muestra confirmacion de exito.
- [ ] Errores de crear/editar usuario se muestran al usuario.
- [ ] `responsableIva` tiene default `No`.
- [ ] ADMIN puede marcar contratista como responsable IVA.

### Documentos requeridos y factura

- [ ] La seccion Documentos Requeridos lista documentos pendientes y cargados.
- [ ] Solo PDF y `.eml` son aceptados.
- [ ] PDF se puede visualizar y descargar.
- [ ] `.eml` se puede previsualizar con metadatos/cuerpo basico y descargar.
- [ ] En `BORRADOR`/`DEVUELTO` se puede adjuntar/reemplazar/eliminar.
- [ ] En `ENVIADO`/`EN_REVISION`/`APROBADO` solo se puede visualizar/descargar.
- [ ] Si contratista es responsable IVA, `FACTURA` aparece como requerida por cada informe.
- [ ] Un informe de responsable IVA no se puede enviar sin `FACTURA`.

### Email

- [ ] Al aprobar informe se intenta enviar email al contratista.
- [ ] Al aprobar informe se intenta enviar email al correo administrador configurable.
- [ ] Si email no esta configurado en local-dev, el evento se registra sin bloquear aprobacion.

### Busqueda

- [ ] ADMIN puede buscar por texto libre contratistas, contratos e informes.
- [ ] Rango de fechas filtra por periodo de informe.
- [ ] Resultados aparecen agrupados y navegan al detalle.
- [ ] Usuarios no ADMIN no pueden acceder.

---

## 9. Rama y Entorno

- **Rama base:** `feat/sigcon-i6`
- **Rama nueva:** `feat/sigcon-i7`
- **Backend:** Java 8, Spring Boot 2.7.18, Oracle 19c, WAR WebLogic
- **Frontend:** Angular 20, TypeScript strict
- **DB local/dev:** `db/00_setup.sql`

---

## 10. Metricas de Cierre

| Area | Meta |
|------|------|
| Backend | Tests nuevos y existentes relevantes en 0 fallos |
| Frontend | Specs nuevos y existentes relevantes en 0 fallos |
| Seguridad | Cobertura de ownership documentos y busqueda ADMIN |
| Documentacion | Spec, plan, execution log y guia funcional actualizados |
| Git | Commits trazables por tarea/fix |
