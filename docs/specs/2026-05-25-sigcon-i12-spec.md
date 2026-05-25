# Spec Tecnica — SIGCON I12
## Mejoras de Perfiles, Informes y Contratos

> Fecha: 2026-05-25
> Metodologia: SDD Spec-Anchored
> Estado: APROBADO para ejecucion
> Alcance: doble perfil CONTRATISTA+ADMIN, campo % ejecucion editable, control de carga de informes, tipos de contrato OPS/PRO

---

## 1. Objetivo

Cuatro mejoras independientes agrupadas en un solo incremento:

1. **R1 — Doble perfil:** Permitir que un usuario CONTRATISTA sea tambien ADMIN, con selector de rol en la UI.
2. **R2 — % Ejecucion editable:** Habilitar edicion del campo `porcentajeEjecucion` en estados EN_REVISION y EN_VISTO_BUENO, sin devolver el flujo al contratista. Ademas, el perfil ADMIN/VB puede ver la vista previa del PDF igual que el REVISOR.
3. **R3 — Control de carga:** El ADMIN puede desactivar la creacion de nuevos informes y notificar a todos los usuarios del sistema via notificacion interna + correo electronico.
4. **R4 — Tipo de contrato PRO:** Ampliar `TipoContrato` a OPS y PRO, con texto de encabezado PDF diferenciado por tipo.

---

## 2. Stack y Archivos Clave

| Capa | Tecnologia |
|------|-----------|
| Backend | Java 8, Spring Boot 2.7.18, Oracle 19c |
| Seguridad | Spring Security, `SecurityConfig`, `DevSecurityConfig` |
| Frontend | Angular 20, PrimeNG 20 |
| PDF | Flying Saucer + OpenPDF, `InformePdfTemplateService` |

Archivos principales impactados:

| Archivo | Cambio |
|---------|--------|
| `domain/entity/Usuario.java` | Agregar campo `esAdmin` |
| `domain/enums/RolUsuario.java` | Sin cambio (el flag reemplaza un nuevo enum) |
| `domain/enums/TipoContrato.java` | Agregar `PRO` |
| `config/SecurityConfig.java` | Extender authorities y acceso a PDF preview |
| `config/DevSecurityConfig.java` | Reflejar mismo cambio en dev |
| `application/service/UsuarioService.java` | Metodos create/update exponen `esAdmin` |
| `application/service/InformeService.java` | Bloqueo por parametro CARGA_INFORMES_ACTIVA |
| `application/service/NotificacionService.java` | Nuevo metodo `notificarBloqueoMasivo` |
| `application/service/InformePdfTemplateService.java` | Texto encabezado segun TipoContrato |
| `web/controller/InformeController.java` | Nuevo endpoint PATCH porcentaje-ejecucion |
| `web/controller/AdminContratoController.java` | Nuevo endpoint PUT control carga informes |
| `sigcon-angular/.../admin-usuarios.component.*` | Checkbox esAdmin |
| `sigcon-angular/.../auth/auth-context.service.ts` | Nuevo servicio selector de rol |
| `sigcon-angular/.../sidebar/sidebar.component.*` | Adaptar menu a activeRole |
| `sigcon-angular/.../topbar/topbar.component.*` | Dropdown selector de rol |
| `sigcon-angular/.../informe-detalle.component.*` | Campo % editable en estados permitidos |
| `sigcon-angular/.../admin-parametros.component.*` | Toggle control carga informes (nuevo o existente) |
| `db/` | Script migracion: columna ES_ADMIN, fila parametro |

---

## 3. Reglas Funcionales

### 3.1 R1 — Doble Perfil CONTRATISTA + ADMIN

**Modelo de datos:**
- `SGCN_USUARIOS` agrega columna `ES_ADMIN NUMBER(1) DEFAULT 0 NOT NULL`.
- Solo aplica cuando `ROL = 'CONTRATISTA'`. Para otros roles el campo existe pero se ignora.

**Spring Security:**
- `UserDetailsService` (o equivalente en `SecurityConfig`) agrega authority `ROLE_ADMIN` cuando `usuario.esAdmin = true` ademas del authority base del rol.
- Los endpoints `/admin/**` requieren `ROLE_ADMIN` sin cambio.
- Los endpoints de contratista requieren `ROLE_CONTRATISTA` sin cambio.

**Selector de rol — Frontend:**
- Nuevo `AuthContextService` con estado `activeRole: 'CONTRATISTA' | 'ADMIN'`.
- Inicia en `'CONTRATISTA'` para usuarios duales al hacer login.
- El topbar muestra un `p-dropdown` visible solo si `usuario.esAdmin = true`.
- Al cambiar `activeRole`, el sidebar recarga el arbol de menu correspondiente.
- Los route guards consultan `AuthContextService.activeRole` en lugar de solo el token para decidir acceso a rutas.
- Los usuarios sin `esAdmin` no ven el selector; su sidebar es identico al actual.

**Gestion de usuarios:**
- El formulario de crear/editar usuario en `AdminUsuariosComponent` expone un checkbox `"Tambien es Admin"`, visible y editable solo cuando `rol === 'CONTRATISTA'`.
- Al guardar, `UsuarioRequest` incluye el campo `esAdmin: boolean`.
- `UsuarioService` persiste el valor.

---

### 3.2 R2 — % Ejecucion Acumulada Editable + Vista Previa VB

**Endpoint nuevo:**
```
PATCH /informes/{id}/porcentaje-ejecucion
Body: { "porcentajeEjecucion": 85.50 }
```
- Roles permitidos: `REVISOR`, `ADMIN` (esAdmin=true).
- Estados permitidos: `EN_REVISION`, `EN_VISTO_BUENO`.
- Si el estado no coincide: `SigconBusinessException(OPERACION_NO_PERMITIDA, 422)`.
- Si el rol no tiene permiso: respuesta `403` via Spring Security.
- El campo ya existe en `SGCN_INFORMES.PORCENTAJE_EJECUCION` — no hay migracion de BD.

**Frontend:**
- En el componente de detalle de informe, `porcentajeEjecucion` renderiza como `p-inputNumber` editable cuando:
  `(estado === 'EN_REVISION' || estado === 'EN_VISTO_BUENO') && (esRevisor || esAdmin)`.
- Al perder foco (`blur`), lanza el PATCH automaticamente.
- En cualquier otro estado o rol, el campo es de solo lectura (comportamiento actual).

**Vista previa PDF para VB/ADMIN:**
- `SecurityConfig`: `GET /informes/{id}/pdf-preview` agrega `ROLE_ADMIN` a los roles permitidos.
- Actualmente solo REVISOR y SUPERVISOR tienen acceso; se suma ADMIN.
- Sin cambios en logica de generacion.

---

### 3.3 R3 — Control de Carga de Informes

**Parametro del sistema:**
- Nueva fila en `SGCN_PARAMETROS`:
  - `CLAVE = 'CARGA_INFORMES_ACTIVA'`
  - `VALOR = 'true'`
  - `DESCRIPCION = 'Habilita la creacion de nuevos informes por contratistas'`
- Se inserta en `08_reset_datos_prueba.sql` y en un script de migracion de produccion.

**Endpoint admin:**
```
PUT /admin/parametros/carga-informes
Requiere: ROLE_ADMIN
Body: { "activo": boolean }
```
- `ParametroService` persiste el valor `'true'` / `'false'` en la clave `CARGA_INFORMES_ACTIVA`.
- Si el valor cambia de `true` a `false`, se dispara `notificarBloqueoMasivo` inmediatamente.
- Si el valor cambia de `false` a `true`, no se envia notificacion.

**Bloqueo en creacion de informes:**
- `InformeService.crearInforme()` consulta `ParametroService.isCargaInformesActiva()` al inicio.
- Si retorna `false`: lanza `SigconBusinessException` con `HttpStatus.LOCKED (423)` y mensaje `"La creacion de nuevos informes esta temporalmente deshabilitada."`.

**Notificacion masiva:**
- `NotificacionService.notificarBloqueoMasivo(String mensaje)`:
  1. Recupera todos los usuarios con `activo = true`.
  2. Por cada usuario: crea una entidad `Notificacion` interna (misma estructura que notificaciones existentes).
  3. Por cada usuario: envia correo via `MailService` al email del usuario.
- Mensaje fijo: `"La carga de nuevos informes ha sido temporalmente deshabilitada por el administrador del sistema."`.

**Frontend:**
- Panel de administracion: tarjeta de configuracion con `p-toggleButton` que refleja el estado actual de `CARGA_INFORMES_ACTIVA`.
- Al desactivar: muestra `p-confirmDialog` antes de enviar el PUT.
- Al activar: sin confirmacion adicional.
- Cuando un contratista intenta crear un informe y recibe 423: el frontend muestra un `p-message` de advertencia en lugar del formulario de creacion.

---

### 3.4 R4 — Tipos de Contrato OPS / PRO

**Enum:**
- `TipoContrato.java`: agrega valor `PRO`.
- Los registros existentes en BD tienen `TIPO = 'OPS'` — no requieren migracion de datos.

**Template PDF:**
- `InformePdfTemplateService`: la linea que genera el texto del tipo de contrato en el encabezado usa `switch` sobre `contrato.getTipo()`:

| Tipo | Texto en encabezado |
|------|-------------------|
| `OPS` | `CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES` |
| `PRO` | `CONTRATO DE APOYO A LA GESTIÓN` |

- Sin cambios en otras secciones del template.

**Frontend:**
- Formulario de creacion/edicion de contrato (admin): el selector de tipo expone ambas opciones:
  - `OPS — Contrato de Prestacion de Servicios Profesionales`
  - `PRO — Contrato de Apoyo a la Gestión`

---

## 4. Criterios de Aceptacion

| ID | Criterio | Verificacion |
|----|----------|--------------|
| AC-01 | Usuario CONTRATISTA con `esAdmin=true` ve selector de rol en topbar | Test funcional |
| AC-02 | Usuario sin `esAdmin` no ve selector de rol | Test funcional |
| AC-03 | En modo ADMIN, el sidebar muestra menu de administracion | Test funcional |
| AC-04 | En modo CONTRATISTA, el sidebar muestra menu de contratista | Test funcional |
| AC-05 | Formulario de usuario muestra checkbox "Tambien es Admin" solo para rol CONTRATISTA | Test funcional |
| AC-06 | `PATCH /informes/{id}/porcentaje-ejecucion` actualiza el campo en estado EN_REVISION | Test unitario |
| AC-07 | `PATCH /informes/{id}/porcentaje-ejecucion` actualiza el campo en estado EN_VISTO_BUENO | Test unitario |
| AC-08 | `PATCH /informes/{id}/porcentaje-ejecucion` rechaza si estado es BORRADOR o APROBADO | Test unitario |
| AC-09 | Campo % ejecucion es editable en la UI solo en estados y roles permitidos | Test funcional |
| AC-10 | ADMIN se suma a REVISOR/SUPERVISOR en acceso a `GET /informes/{id}/pdf-preview` (no se elimina acceso existente) | Test de seguridad |
| AC-11 | `PUT /admin/parametros/carga-informes` con `activo=false` persiste el parametro | Test unitario |
| AC-12 | Con `CARGA_INFORMES_ACTIVA=false`, `crearInforme` lanza error 423 | Test unitario |
| AC-13 | Al desactivar carga, se crean notificaciones internas para todos los usuarios activos | Test unitario |
| AC-14 | Al desactivar carga, se envian correos a todos los usuarios activos | Test unitario |
| AC-15 | Frontend muestra mensaje de bloqueo al recibir 423 al crear informe | Test funcional |
| AC-16 | PDF de contrato OPS muestra "CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES" | Test unitario |
| AC-17 | PDF de contrato PRO muestra "CONTRATO DE APOYO A LA GESTION" | Test unitario |
| AC-18 | Selector de tipo contrato en formulario admin muestra OPS y PRO | Test funcional |
| AC-19 | `mvn test` BUILD SUCCESS con 0 failures al cierre del incremento | `mvn test` |

---

## 5. Limites

- No modificar el flujo de estados de informes (BORRADOR → EN_REVISION → EN_VISTO_BUENO → APROBADO).
- No introducir nuevas dependencias Maven ni NPM.
- La dualidad de roles aplica exclusivamente a CONTRATISTA + ADMIN; no se contemplan otras combinaciones.
- El campo `porcentajeEjecucion` no tiene formula automatica en este incremento; sigue siendo entrada manual.
- La notificacion masiva de R3 usa la infraestructura existente de `Notificacion` y `MailService`; no se introduce un nuevo mecanismo de mensajeria.
- No modificar logica de generacion de secciones del PDF distintas al encabezado (R4).
- No modificar contratos existentes en BD al agregar tipo PRO.
