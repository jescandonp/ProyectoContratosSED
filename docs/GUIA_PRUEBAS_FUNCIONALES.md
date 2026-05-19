# SIGCON - Guia De Pruebas Funcionales

Estado: guia operativa para pruebas manuales I1-I8  
Fecha: 2026-05-19  
Rama objetivo: `main` (incluye hasta I8)  
Marco: SDD Spec-Anchored por incrementos

## 1. Objetivo

Validar funcionalmente SIGCON desde la base tecnica hasta el flujo completo de contratos, informes, revision, aprobacion, PDF y notificaciones.

Esta guia cubre:

- Incremento 1: autenticacion local-dev, usuarios, contratos, catalogo y acceso por rol.
- Incremento 2: creacion y gestion de informes, actividades, soportes, documentos y revision.
- Incremento 3: aprobacion final, PDF institucional y notificaciones.
- Incrementos 4-7: edicion administrativa, PDF institucional, SGSSI, usuario IVA, documentos requeridos, email de aprobacion y busqueda administrativa.
- Incremento 8: campo `fechaElaboracion` en informes y PDF formato institucional SED 11-IF-023 V1.

## 2. Prerrequisitos

### 2.1 Base De Datos Oracle

El esquema local debe existir y tener los scripts ejecutados:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED

sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/00_setup.sql
sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/01_datos_prueba.sql
```

Si el usuario ya existe y los scripts ya fueron ejecutados, no repetir `00_setup.sql` sobre el mismo esquema porque no es idempotente.

Para una base existente anterior a I7, ejecutar la migracion incremental:

```powershell
sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/04_apply_i7_schema.sql
```

Esta migracion resuelve el error `Schema-validation: missing table [sgcn_docs_requeridos]`.

Para una base existente anterior a I8, ejecutar adicionalmente:

```powershell
sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/05_add_fecha_elaboracion.sql
```

Esta migracion agrega `SGCN_INFORMES.FECHA_ELABORACION`. Si no se ejecuta, el backend arranca con error `Schema-validation: missing column [fecha_elaboracion] in table [sgcn_informes]`.

### 2.2 Backend

En una terminal:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend

$env:DB_USERNAME="SED_SIGCON"
$env:DB_PASSWORD="Sigcon2026Local1"
$env:SPRING_PROFILE="local-dev"

mvn spring-boot:run
```

Validar:

```text
http://localhost:8080/actuator/health
```

Resultado esperado:

```json
{"status":"UP"}
```

### 2.3 Frontend

En otra terminal:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
npm start
```

Abrir:

```text
http://localhost:4200
```

### 2.4 Credenciales Local-Dev

La pantalla de login local-dev muestra botones por rol. Las credenciales equivalentes son:

| Rol | Email | Password |
|---|---|---|
| ADMIN | `admin@educacionbogota.edu.co` | `admin123` |
| CONTRATISTA | `juan.escandon@educacionbogota.edu.co` | `contratista123` |
| REVISOR | `revisor1@educacionbogota.edu.co` | `revisor123` |
| SUPERVISOR | `supervisor1@educacionbogota.edu.co` | `supervisor123` |

## 3. Formato De Registro De Evidencia

Usar este formato para cada caso:

```text
ID prueba:
Incremento:
Rol:
Accion:
Datos usados:
Resultado esperado:
Resultado obtenido:
Endpoint observado:
Estado final:
Captura o log:
OK / Error:
Observaciones:
```

## 4. Orden Recomendado

Ejecutar primero una ruta feliz completa:

```text
ADMIN -> CONTRATISTA -> REVISOR -> SUPERVISOR -> PDF -> Notificaciones
```

Despues ejecutar pruebas negativas y de permisos.

Este orden reduce ruido: primero confirma que la base esta funcionando y luego valida bordes.

## 5. Prevalidacion Tecnica

| ID | Accion | Esperado |
|---|---|---|
| T-00-01 | Abrir `http://localhost:8080/actuator/health` | Backend responde `UP` |
| T-00-02 | Abrir `http://localhost:4200` | Frontend carga login local-dev |
| T-00-03 | Abrir DevTools -> Network | Requests `/api/...` pasan por frontend proxy hacia backend |
| T-00-04 | Login como ADMIN | Redireccion a shell SIGCON |
| T-00-05 | Refrescar navegador con `Ctrl + F5` | No aparecen errores de assets criticos |

## 6. Incremento 1 - Administracion Base

### 6.1 Usuarios

Entrar como `ADMIN`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I1-U-01 | Crear usuario contratista | Email nuevo, nombre, cargo, rol CONTRATISTA | Usuario creado y visible en tabla |
| I1-U-02 | Crear usuario revisor | Email nuevo, rol REVISOR | Usuario creado |
| I1-U-03 | Crear usuario supervisor | Email nuevo, rol SUPERVISOR | Usuario creado |
| I1-U-04 | Crear usuario admin | Email nuevo, rol ADMIN | Usuario creado |
| I1-U-05 | Crear usuario con email repetido | Email ya existente | Mensaje `El email ya esta registrado.` |
| I1-U-06 | Editar nombre/cargo/rol | Usuario existente | Cambios visibles al recargar |
| I1-U-07 | Desactivar usuario | Usuario activo | Estado pasa a inactivo/no visible segun filtro |
| I1-U-08 | Reactivar usuario | Usuario inactivo | Estado vuelve activo |

Validaciones adicionales:

- Si aparece mensaje de conexion, validar backend en `localhost:8080`.
- Si aparece mensaje de permisos, confirmar que la sesion actual sea ADMIN.
- Si aparece validacion, revisar campos obligatorios y formato de email.

### 6.2 Contratos

Entrar como `ADMIN`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I1-C-01 | Crear contrato OPS | Numero nuevo, objeto, valor, fechas, contratista, revisor, supervisor | Contrato creado |
| I1-C-02 | Ver contrato en listado | Contrato creado | Aparece en tabla |
| I1-C-03 | Abrir detalle de contrato | Contrato creado | Muestra datos, obligaciones e informes asociados |
| I1-C-04 | Editar contrato | Cambiar objeto/cargo/asignaciones si aplica | Cambios persistidos |
| I1-C-05 | Crear contrato con numero duplicado | Numero existente | Error de contrato duplicado |
| I1-C-06 | Cambiar estado de contrato | EN_EJECUCION, LIQUIDADO o CERRADO | Estado actualizado |

### 6.3 Catalogo De Documentos

Entrar como `ADMIN`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I1-D-01 | Crear documento obligatorio | Nombre, descripcion, obligatorio=true | Documento visible |
| I1-D-02 | Crear documento opcional | obligatorio=false | Documento visible |
| I1-D-03 | Editar documento | Cambiar descripcion/obligatorio | Cambios persistidos |
| I1-D-04 | Desactivar documento si aplica | Documento activo | No se ofrece como requerido en flujos nuevos |

## 7. Incremento 1 - Acceso Por Rol

| ID | Rol | Accion | Esperado |
|---|---|---|---|
| I1-R-01 | CONTRATISTA | Abrir contratos | Solo ve contratos asignados como contratista |
| I1-R-02 | CONTRATISTA | Intentar abrir administracion | Redireccion o acceso denegado |
| I1-R-03 | REVISOR | Abrir contratos | Solo ve contratos asignados como revisor |
| I1-R-04 | REVISOR | Intentar crear contrato | Acceso denegado |
| I1-R-05 | SUPERVISOR | Abrir contratos | Solo ve contratos asignados como supervisor |
| I1-R-06 | SUPERVISOR | Intentar crear usuario | Acceso denegado |
| I1-R-07 | ADMIN | Abrir administracion | Acceso permitido |

## 8. Incremento 2 - Informes

Entrar como `CONTRATISTA`.

### 8.1 Creacion De Informe

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I2-I-01 | Abrir contrato asignado | Contrato en ejecucion | Detalle visible |
| I2-I-02 | Crear informe | Fechas inicio/fin | Informe en estado `BORRADOR` |
| I2-I-03 | Agregar actividad | Obligacion, descripcion, porcentaje | Actividad visible |
| I2-I-04 | Agregar soporte URL | Nombre + URL | Soporte visible |
| I2-I-05 | Agregar soporte archivo/referencia | Nombre + referencia | Soporte visible |
| I2-I-06 | Agregar documento adicional | Documento catalogo + referencia | Documento visible |
| I2-I-07 | Enviar informe sin actividades | Informe vacio | Error de actividad requerida |
| I2-I-08 | Enviar informe con actividades | Informe completo | Estado pasa a `ENVIADO` |

### 8.2 Restricciones De Edicion

| ID | Accion | Esperado |
|---|---|
| I2-E-01 | Editar informe `BORRADOR` | Permitido |
| I2-E-02 | Editar informe `DEVUELTO` | Permitido |
| I2-E-03 | Editar informe `ENVIADO` | Bloqueado |
| I2-E-04 | Editar informe `EN_REVISION` | Bloqueado |
| I2-E-05 | Editar informe `APROBADO` | Bloqueado |

## 9. Incremento 2 - Revision

Entrar como `REVISOR`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I2-REV-01 | Abrir cola de revision | Informe `ENVIADO` | Informe aparece |
| I2-REV-02 | Abrir detalle | Informe asignado | Detalle visible |
| I2-REV-03 | Devolver sin observacion | Campo vacio | Error de observacion requerida |
| I2-REV-04 | Devolver con observacion | Texto de correccion | Estado pasa a `DEVUELTO` |
| I2-REV-05 | Contratista corrige informe devuelto | Ajustar actividad/soporte | Edicion permitida |
| I2-REV-06 | Contratista reenvia | Informe corregido | Estado pasa a `ENVIADO` |
| I2-REV-07 | Revisor aprueba revision | Observacion opcional | Estado pasa a `EN_REVISION` |

## 10. Incremento 3 - Aprobacion Final

Entrar como `SUPERVISOR`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I3-APR-01 | Abrir cola de aprobacion | Informe `EN_REVISION` | Informe aparece |
| I3-APR-02 | Abrir detalle | Informe asignado | Detalle visible |
| I3-APR-03 | Devolver sin observacion | Campo vacio | Error de observacion requerida |
| I3-APR-04 | Devolver con observacion | Texto de correccion | Estado pasa a `DEVUELTO` |
| I3-APR-05 | Repetir correccion y aprobacion revision | Flujo contratista/revisor | Estado vuelve a `EN_REVISION` |
| I3-APR-06 | Aprobar final | Supervisor asignado | Estado pasa a `APROBADO` |
| I3-APR-07 | Validar fecha aprobacion | Informe aprobado | Fecha presente |
| I3-APR-08 | Validar terminalidad | Intentar editar/aprobar/devolver de nuevo | Bloqueado |

## 11. Incremento 3 - PDF

| ID | Rol | Accion | Esperado |
|---|---|---|---|
| I3-PDF-01 | SUPERVISOR | Descargar PDF aprobado | Descarga permitida |
| I3-PDF-02 | CONTRATISTA asignado | Descargar PDF aprobado | Descarga permitida |
| I3-PDF-03 | ADMIN | Descargar PDF aprobado | Descarga permitida |
| I3-PDF-04 | REVISOR | Descargar PDF aprobado | Acceso denegado |
| I3-PDF-05 | CONTRATISTA no asignado | Descargar PDF de contrato ajeno | Acceso denegado |
| I3-PDF-06 | CONTRATISTA asignado | Descargar PDF antes de aprobar | PDF no disponible |

Validar en el PDF:

- Numero de informe.
- Numero de contrato.
- Datos de contratista.
- Periodo.
- Actividades.
- Documentos/soportes.
- Fecha de aprobacion no debe aparecer como `N/A`.
- Hash/ruta de PDF si la UI lo expone.

## 12. Incremento 3 - Notificaciones

Validar despues de cada transicion:

| ID | Evento | Destinatario esperado | Esperado |
|---|---|---|---|
| I3-N-01 | Contratista envia informe | REVISOR | Notificacion in-app |
| I3-N-02 | Revisor devuelve | CONTRATISTA | Notificacion in-app |
| I3-N-03 | Revisor aprueba revision | SUPERVISOR | Notificacion in-app |
| I3-N-04 | Supervisor devuelve | CONTRATISTA | Notificacion in-app |
| I3-N-05 | Supervisor aprueba final | CONTRATISTA | Notificacion in-app |

Validaciones de UI:

| ID | Accion | Esperado |
|---|---|---|
| I3-N-06 | Ver contador en topbar | Refleja no leidas |
| I3-N-07 | Abrir menu de notificaciones | Muestra recientes |
| I3-N-08 | Abrir centro de notificaciones | Lista paginada |
| I3-N-09 | Marcar una como leida | Baja contador |
| I3-N-10 | Marcar todas como leidas | Contador queda en cero |
| I3-N-11 | Click en notificacion con informe | Navega al detalle |

Nota local-dev:

- Email Office 365 esta simulado en logs (`sigcon.mail.enabled=false`).
- No se espera envio real de correo en local-dev.

## 13. Pruebas Negativas Criticas

| ID | Caso | Esperado |
|---|---|---|
| NEG-01 | Contratista crea informe en contrato ajeno | Acceso denegado |
| NEG-02 | Contratista ve contrato ajeno | Acceso denegado |
| NEG-03 | Revisor aprueba informe no asignado | Acceso denegado |
| NEG-04 | Supervisor aprueba informe no asignado | Acceso denegado |
| NEG-05 | Revisor intenta aprobacion final | Acceso denegado |
| NEG-06 | Supervisor intenta aprobar revision | Acceso denegado |
| NEG-07 | Admin intenta ejecutar acciones operativas de contratista si no estan permitidas | Acceso denegado o no disponible |
| NEG-08 | Informe aprobado intenta volver a estados previos | Bloqueado |
| NEG-09 | PDF falla durante aprobacion | No debe quedar `APROBADO` |
| NEG-10 | Usuario sin rol admin intenta crear usuarios | Acceso denegado |

## 14. Diagnostico Rapido De Errores

| Sintoma | Causa probable | Validacion |
|---|---|---|
| `No se pudo conectar con el backend` | Backend apagado o proxy no disponible | Abrir `http://localhost:8080/actuator/health` |
| `ORA-01017` al arrancar backend | Usuario/password Oracle incorrectos | Probar `sqlplus SED_SIGCON/<password>@localhost:1521/XEPDB1` |
| `EMAIL_DUPLICADO` | Usuario activo con mismo email | Usar otro email o revisar `SGCN_USUARIOS` |
| `VALIDACION_FALLIDA` | Campo requerido o formato invalido | Revisar email, nombre y rol |
| `403` | Rol no autorizado | Confirmar login local-dev por rol correcto |
| PDF no disponible | Informe no aprobado o PDF no generado | Revisar estado `APROBADO` y `pdfRuta` |

## 15. Cierre De Pruebas

Antes de declarar el ciclo manual como aprobado:

- Ruta feliz completa ejecutada.
- Pruebas negativas criticas ejecutadas.
- Evidencia guardada por rol.
- No hay errores 500 no explicados.
- Backend sigue respondiendo `UP`.
- Frontend no muestra errores bloqueantes en consola.
- Cualquier desviacion queda registrada con pasos de reproduccion.

---

## 12. Incremento 4 — Hallazgos Funcionales

> Rama: `feat/sigcon-i4`  
> Fecha: 2026-05-04

### 12.1 H1 — Revisor Opcional (Flujo Sin Revisor)

Entrar como `ADMIN`. Crear un contrato sin seleccionar revisor.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I4-H1-01 | Crear contrato sin revisor | Campo "Revisor (opcional)" vacío | Contrato creado, campo revisor = Sin revisor |
| I4-H1-02 | Verificar detalle del contrato | Contrato recién creado | Revisor muestra "No asignado" |
| I4-H1-03 | Contratista crea informe | Contrato sin revisor | Informe en BORRADOR |
| I4-H1-04 | Contratista envía informe | Informe en BORRADOR | Estado pasa a ENVIADO |
| I4-H1-05 | Supervisor aprueba desde ENVIADO | Informe en ENVIADO, sin revisor | Estado pasa a APROBADO (sin pasar por EN_REVISION) |
| I4-H1-06 | Supervisor devuelve desde ENVIADO | Informe en ENVIADO, sin revisor | Estado pasa a DEVUELTO con observación |
| I4-H1-07 | Contrato CON revisor — flujo sin cambios | Contrato existente con revisor | Flujo I1-I3 idéntico (regresión) |
| I4-H1-08 | Revisor intenta actuar en contrato sin revisor | `POST /api/informes/{id}/aprobar-revision` | 403 ACCESO_DENEGADO |

### 12.2 H2 — Contrato Editable

Entrar como `ADMIN`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I4-H2-01 | Editar número de contrato | Nuevo número único | Contrato actualizado, número cambiado |
| I4-H2-02 | Editar número duplicado | Número de otro contrato activo | Error 409 con mensaje inline |
| I4-H2-03 | Agregar revisor a contrato sin revisor | Seleccionar revisor en el form | Contrato actualizado con revisor asignado |
| I4-H2-04 | Quitar revisor de contrato con revisor | Seleccionar "Sin revisor" | Contrato actualizado sin revisor |
| I4-H2-05 | Cambiar supervisor | Seleccionar otro supervisor | Nuevo supervisor puede actuar en informes activos |
| I4-H2-06 | Editar fechas del contrato | Fecha fin < fecha inicio | Error 422 FECHA_FIN_INVALIDA |
| I4-H2-07 | Rol CONTRATISTA intenta editar contrato | `PUT /api/admin/contratos/{id}` | 403 Forbidden |
| I4-H2-08 | Cancelar edición | Click en "Cancelar" | Valores originales restaurados, sin llamada HTTP |

### 12.3 H3 — Informe Editable (Periodo)

Entrar como `CONTRATISTA`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I4-H3-01 | Editar periodo en BORRADOR | Nuevas fechas válidas | Periodo actualizado, vista actualizada sin recargar |
| I4-H3-02 | Editar periodo en DEVUELTO | Nuevas fechas válidas | Periodo actualizado |
| I4-H3-03 | Fecha fin < fecha inicio | fechaFin anterior a fechaInicio | Error 422 con mensaje inline |
| I4-H3-04 | Informe en ENVIADO | Abrir detalle | Campos de periodo en solo lectura (sin botón "Guardar periodo") |
| I4-H3-05 | Informe en EN_REVISION | Abrir detalle | Campos de periodo en solo lectura |
| I4-H3-06 | Informe en APROBADO | Abrir detalle | Campos de periodo en solo lectura |
| I4-H3-07 | Contratista B intenta editar informe de contratista A | `PATCH /api/informes/{id}` | 403 ACCESO_DENEGADO |

### 12.4 Diagnóstico de Errores Comunes I4

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| 403 al editar contrato como ADMIN | Endpoint incorrecto (usando `/api/contratos/{id}` en vez de `/api/admin/contratos/{id}`) | Verificar que el frontend usa `actualizarContratoAdmin` |
| Supervisor no puede aprobar desde ENVIADO | Contrato tiene revisor asignado | Verificar `contrato.revisor` en BD |
| Campo revisor sigue siendo obligatorio en el form | Cache del navegador o build desactualizado | Limpiar caché y reconstruir |
| 409 al crear contrato sin revisor | Backend no actualizado a I4 | Verificar que `feat/sigcon-i4` está desplegado |

---

## 13. Incremento 5 — Edición de Actividades en Informe BORRADOR

> Rama: `feat/sigcon-i5`  
> Fecha: 2026-05-04

### 13.1 Editar descripción de actividad

1. Autenticarse como CONTRATISTA.
2. Navegar a un informe en estado **BORRADOR**.
3. Verificar que las tarjetas de actividad muestran campos editables (textarea de descripción, input de porcentaje).
4. Modificar la descripción de una actividad.
5. Hacer clic en **"Guardar actividad"**.
6. Verificar que la descripción actualizada se muestra en la tarjeta.

### 13.2 Editar porcentaje de avance

1. En la misma pantalla, modificar el porcentaje de una actividad (0–100).
2. Guardar. Verificar que el porcentaje actualizado se muestra.

### 13.3 Agregar soporte URL

1. En la tarjeta de una actividad, ingresar URL en el campo "URL soporte".
2. Hacer clic en **"Guardar actividad"**.
3. Verificar que el soporte aparece en la lista de soportes de la actividad.

### 13.4 Eliminar soporte existente

1. Hacer clic en **×** junto a un soporte existente.
2. Verificar que el soporte desaparece de la lista.

### 13.5 Agregar documento adicional

1. En la sección "Documentos adicionales", seleccionar tipo de documento del catálogo.
2. Ingresar referencia.
3. Hacer clic en **"Agregar"**.
4. Verificar que el documento aparece en la lista.

### 13.6 Eliminar documento adicional

1. Hacer clic en **×** junto a un documento adicional existente.
2. Verificar que desaparece de la lista.

### 13.7 Solo lectura en otros estados

| Estado | Comportamiento esperado |
|--------|------------------------|
| ENVIADO | Sin campos editables, sin botones de guardar/eliminar |
| EN_REVISION | Sin campos editables |
| DEVUELTO | Sin campos editables (usa `CorregirInformeComponent`) |
| APROBADO | Sin campos editables |

### 13.8 Validaciones

| Acción | Resultado esperado |
|--------|-------------------|
| Guardar actividad con descripción vacía | Error inline: "La descripcion no puede estar vacia." |
| Agregar documento sin seleccionar tipo | Error inline: "Seleccione el tipo de documento e ingrese la referencia." |

### Diagnóstico de errores comunes I5

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| 403 al guardar actividad | Contratista no es propietario del informe | Verificar usuario autenticado |
| 409 al guardar actividad | Informe no está en BORRADOR | Recargar la página |
| Campos no editables en BORRADOR | Build desactualizado | Limpiar caché y reconstruir |
| Soporte no aparece tras guardar | Error silencioso en la recarga | Verificar consola del navegador |

---

## 14. Incremento 6 — Datos Complementarios, Aportes SGSSI y PDF Institucional

### Prerrequisitos

- Usuario CONTRATISTA con un informe en estado BORRADOR
- Usuario ADMIN con acceso a la administración de contratos
- Navegador con consola abierta para verificar errores silenciosos

---

### 14.1 Dependencia del contrato (AdminContratoForm)

**Contexto:** El campo "Dependencia SED" es un input con autocompletado nativo (`<datalist>`) que lista 44 unidades organizacionales de la SED.

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Ir a Administración → Contratos → Nuevo contrato | Formulario visible con sección "Datos Complementarios" |
| 2 | Hacer clic en el campo Dependencia | Lista desplegable con unidades SED aparece |
| 3 | Escribir "local" | Lista filtra solo las Direcciones Locales |
| 4 | Seleccionar "Dirección Local Usaquén" | Campo se llena con el valor seleccionado |
| 5 | Escribir un valor libre (p. ej. "Unidad X") | Campo acepta texto libre no incluido en la lista |
| 6 | Llenar todos los campos obligatorios y guardar | Contrato creado; al editar, dependencia aparece pre-cargada |
| 7 | Editar el contrato y cambiar la dependencia | Nuevo valor se guarda correctamente |

---

### 14.2 Forma de pago y modificaciones del contrato

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | En el formulario de contrato, completar "Forma de Pago" | Campo de texto acepta entrada libre |
| 2 | Completar "Modificaciones" (textarea) | Acepta texto largo (p. ej. descripción de adición) |
| 3 | Guardar y reabrir el contrato | Ambos campos persisten con los valores ingresados |
| 4 | Dejar ambos campos vacíos y guardar | Contrato se guarda sin error (campos opcionales) |

---

### 14.3 Entidades SGSSI en el perfil del contratista

**Contexto:** El perfil de un CONTRATISTA expone campos para registrar las entidades de salud, pensión y ARL.

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Iniciar sesión como CONTRATISTA → ir a Perfil | Sección "Aportes SGSSI — Entidades" visible (3 campos) |
| 2 | Completar "Entidad Salud" con "Nueva EPS" | |
| 3 | Completar "Entidad Pensión" con "Protección" | |
| 4 | Completar "Entidad ARL" con "Sura" | |
| 5 | Guardar perfil | Mensaje de confirmación; al recargar, los 3 valores persisten |
| 6 | Iniciar sesión como ADMIN | Sección SGSSI NO debe aparecer en el perfil de ADMIN |

---

### 14.4 Datos del desembolso en nuevo informe (InformeForm)

**Contexto:** Al crear un informe, el encabezado incluye Nro. Desembolso, Valor Desembolso, % Ejecución y Correspondencia Pendiente. Las actividades ya NO tienen campo "Avance %".

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Crear nuevo informe para un contrato | Formulario muestra 4 campos de desembolso en el encabezado |
| 2 | Ingresar Nro. Desembolso = 2 | Campo numérico acepta el valor |
| 3 | Ingresar Valor Desembolso = 1500000 | |
| 4 | Ingresar % Ejecución = 45 | |
| 5 | Marcar "Correspondencia Pendiente" | Checkbox marcado |
| 6 | Verificar tarjeta de actividad | NO existe campo "Avance %" en ninguna tarjeta |
| 7 | Completar todas las actividades y enviar | Informe creado; detalles persisten al abrir el informe |
| 8 | Dejar todos los campos de desembolso vacíos | Informe se crea sin error (campos opcionales) |

---

### 14.5 Aportes SGSSI en nuevo informe (InformeForm)

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | En el formulario de nuevo informe, localizar sección "Aportes SGSSI" | Botón "+ Agregar aporte" visible |
| 2 | Hacer clic en "+ Agregar aporte" | Nueva fila con select (SALUD/PENSION/ARL), entidad, fecha de pago y valor |
| 3 | Seleccionar "Pensión", ingresar entidad "Protección", fecha 2026-03-05, valor 180000 | Fila con datos válidos |
| 4 | Agregar otra fila para ARL | Total: 2 filas de aportes |
| 5 | Hacer clic en "×" de una fila | Fila eliminada; quedan las restantes |
| 6 | Crear el informe | Aportes se guardan vía PUT `/api/informes/{id}/aportes-sgssi` tras crear el informe |
| 7 | Crear informe sin agregar ningún aporte | Informe se crea sin error |

---

### 14.6 Aportes SGSSI en detalle del informe (InformeDetalle — BORRADOR)

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Abrir un informe en BORRADOR | Sección "Aportes SGSSI" visible con botón "+ Agregar aporte" |
| 2 | Verificar que NO existe badge "Avance %" en ninguna actividad | Ningún indicador porcentual en las tarjetas |
| 3 | Hacer clic en "+ Agregar aporte" | Nueva fila editable aparece al final de la tabla |
| 4 | Completar: Item=SALUD, Entidad="SaludTotal", Fecha=2026-03-31, Valor=250000 | |
| 5 | Hacer clic en "Guardar aportes SGSSI" | PUT al backend; sección se recarga con los datos guardados |
| 6 | Agregar una fila incompleta (sin entidad) junto a una completa | Solo la fila completa se envía al backend; la incompleta se descarta silenciosamente |
| 7 | Eliminar una fila existente y guardar | PUT con la lista sin la fila eliminada; backend reemplaza todos los aportes |
| 8 | Simular error del backend (desconectar red) y guardar | Mensaje de error: "No se pudieron guardar los aportes SGSSI. Intente de nuevo." |

---

### 14.7 Aportes SGSSI en solo lectura (InformeDetalle — ENVIADO / APROBADO)

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Abrir un informe en estado ENVIADO o APROBADO | Sección SGSSI muestra lista de aportes sin campos editables |
| 2 | Verificar formato del valor | Valor mostrado con separadores de miles (pipe `number`) |
| 3 | Verificar etiqueta del item | "Salud", "Pensión" o "A.R.L." según el tipo |
| 4 | Verificar que no hay botón "Guardar aportes SGSSI" | Botón ausente en estados distintos de BORRADOR |

---

### 14.8 PDF Institucional (I6 — superado por I8)

> **Nota:** El PDF fue rediseñado completamente en I8 (plantilla 11-IF-023 V1). Las pruebas del PDF deben realizarse usando la Seccion 16.3 de esta guia. La descripcion anterior de 8 secciones (I6) ya no aplica al estado actual del sistema.

**Contexto (referencia historica I6):** El PDF generado sigue el diseño institucional SED con colores primarios (`#002869`) y encabezado de la entidad.

#### Flujo de generación

| Paso | Acción | Resultado esperado |
|------|--------|--------------------|
| 1 | Con un informe en BORRADOR completo, hacer clic en "Preview" | Previsualización HTML del informe |
| 2 | Hacer clic en "Ver PDF" | PDF generado y almacenado; descarga o visualización en navegador |
| 3 | Verificar sección 1 — Encabezado | Logo SED (si aplica), nombre de la entidad, número y fecha del informe |
| 4 | Verificar sección 2 — Datos del contrato | Número de contrato, objeto, contratista, valor, vigencia, dependencia, forma de pago |
| 5 | Verificar sección 3 — Datos del desembolso | Nro. desembolso, valor (en letras con `NumeroPesosConverter`), % ejecución, correspondencia pendiente |
| 6 | Verificar sección 4 — Periodo del informe | Fecha inicio y fecha fin del período reportado |
| 7 | Verificar sección 5 — Actividades | Tabla SIN columna "% avance"; columnas: Nro., Obligación, Descripción, Soportes |
| 8 | Verificar sección 6 — Aportes SGSSI | Tabla con período calculado (mes anterior al inicio del informe), entidades y valores |
| 9 | Verificar sección 7 — Documentos adicionales | Lista de documentos con tipo y referencia |
| 10 | Verificar sección 8 — Firmas | Firma contratista + firma supervisor; firma revisor solo si el revisor tiene imagen cargada |

#### Casos de firma del revisor

| Escenario | Resultado esperado en el PDF |
|-----------|------------------------------|
| Revisor con firma cargada | Celda de firma del revisor visible con imagen |
| Revisor sin firma | Celda de firma del revisor ausente; PDF se genera sin error |
| Sin revisor asignado | Celda de firma del revisor ausente; PDF se genera sin error |

#### Verificación del valor en letras

| Valor desembolso | Texto esperado |
|-----------------|----------------|
| 1.500.000 | "UN MILLÓN QUINIENTOS MIL PESOS M/CTE" |
| 0 | "CERO PESOS M/CTE" |
| null / no ingresado | Campo omitido o vacío |

---

### 14.9 Período SGSSI en el PDF

El período de aportes SGSSI se calcula automáticamente como el mes anterior a `fechaInicio` del informe.

| Fecha inicio del informe | Período SGSSI esperado en el PDF |
|--------------------------|----------------------------------|
| 2026-04-01 | Marzo 2026 |
| 2026-01-15 | Diciembre 2025 |
| 2026-07-01 | Junio 2026 |

---

### Diagnóstico de errores comunes I6

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| Sección SGSSI no aparece en BORRADOR | `aportesSgssi` ausente en `InformeDetalle` | Verificar que el backend incluye el campo en el DTO |
| "Guardar aportes" no hace nada | `guardarTodos` retorna 404 | Verificar que el endpoint PUT `/api/informes/{id}/aportes-sgssi` está registrado |
| Filas incompletas se envían | Filtro de validación no activo | Verificar lógica en `guardarAportesSgssi()` del componente |
| PDF sin sección de aportes | `aporteSgssiRepository.findByInformeIdAndActivoTrue()` no retorna datos | Verificar que el campo `activo = true` se establece al guardar |
| Firma del revisor causa NPE en PDF | Ausencia de null-check antes de `readSignatureBytes` | Verificar bloque try/catch en `PdfInformeService.generarYPersistir()` |
| Dependencia no aparece en el PDF | Campo no incluido en `generarPdf()` template | Verificar que `informe.getContrato().getDependencia()` se referencia en `InformePdfTemplateService` |
| Lista de dependencias vacía en el formulario | Constante `SED_DEPENDENCIAS` no importada | Verificar import desde `sed-dependencias.constants.ts` |

---

## 15. Incremento 7 — Usuario IVA, Documentos Requeridos, Email de Aprobación y Búsqueda Administrativa

> Rama: `feat/sigcon-i7`
> Fecha: 2026-05-11

### 15.1 Usuario Responsable de IVA

Entrar como `ADMIN`.

| ID | Acción | Datos | Esperado |
|---|---|---|---|
| I7-U-01 | Crear usuario contratista sin marcar IVA | Formulario con "Responsable de IVA" desmarcado | Usuario creado con `responsableIva = false`; mensaje "Usuario creado correctamente." |
| I7-U-02 | Crear usuario contratista marcando IVA | Checkbox "Responsable de IVA" marcado | Usuario creado con `responsableIva = true`; mensaje "Usuario creado correctamente." |
| I7-U-03 | Editar usuario y cambiar IVA a true | Usuario existente con IVA=false | Cambio persistido; mensaje "Usuario actualizado correctamente." |
| I7-U-04 | Editar usuario y cambiar IVA a false | Usuario existente con IVA=true | Cambio persistido; mensaje "Usuario actualizado correctamente." |
| I7-U-05 | Crear usuario con email duplicado | Email ya existente | Mensaje de error claro visible en el formulario |
| I7-U-06 | Verificar default visual | Abrir formulario de nuevo usuario | Checkbox "Responsable de IVA" desmarcado por defecto |

### 15.2 Documentos Requeridos — Contratista NO Responsable de IVA

Entrar como `CONTRATISTA` con `responsableIva = false`.

| ID | Acción | Datos | Esperado |
|---|---|---|---|
| I7-DR-01 | Abrir informe en BORRADOR | Informe del contratista | Sección "Documentos Requeridos" visible; sin FACTURA en la lista |
| I7-DR-02 | Cargar archivo PDF | Seleccionar archivo `.pdf` | Archivo cargado; badge "Cargado" aparece; botones Descargar y Eliminar visibles |
| I7-DR-03 | Cargar archivo EML | Seleccionar archivo `.eml` | Archivo cargado; botón "Vista previa" aparece |
| I7-DR-04 | Intentar cargar archivo DOCX | Seleccionar archivo `.docx` | Error: "Solo se permiten archivos PDF y EML." |
| I7-DR-05 | Ver preview EML | Clic en "Vista previa" de un EML cargado | Modal con asunto, remitente, fecha y cuerpo texto |
| I7-DR-06 | Descargar archivo | Clic en "Descargar" | Descarga del archivo original |
| I7-DR-07 | Eliminar archivo | Clic en "Eliminar" en BORRADOR | Badge vuelve a "Pendiente"; botón Eliminar desaparece |
| I7-DR-08 | Enviar informe sin documentos requeridos pendientes | Todos los requeridos cargados | Informe pasa a ENVIADO |

### 15.3 Documentos Requeridos — Contratista Responsable de IVA (FACTURA)

Entrar como `CONTRATISTA` con `responsableIva = true`.

| ID | Acción | Datos | Esperado |
|---|---|---|---|
| I7-FAC-01 | Abrir informe en BORRADOR | Informe del contratista IVA | Sección "Documentos Requeridos" muestra FACTURA con badge "IVA" y badge "Pendiente" |
| I7-FAC-02 | Intentar enviar sin FACTURA | Clic en "Enviar" | Error: "debe cargar la FACTURA antes de enviar el informe" |
| I7-FAC-03 | Cargar FACTURA (PDF) | Seleccionar archivo `.pdf` | Badge cambia a "Cargado"; badge "IVA" permanece |
| I7-FAC-04 | Enviar informe con FACTURA cargada | Todos los requeridos cargados | Informe pasa a ENVIADO |
| I7-FAC-05 | Verificar solo lectura en ENVIADO | Abrir informe ENVIADO | Botones Cargar/Eliminar ausentes; Descargar disponible |
| I7-FAC-06 | Verificar solo lectura en APROBADO | Abrir informe APROBADO | Solo Descargar disponible |

### 15.4 Email de Aprobación

| ID | Acción | Datos | Esperado |
|---|---|---|---|
| I7-EMAIL-01 | Aprobar informe como SUPERVISOR | Informe en EN_REVISION | Estado pasa a APROBADO; en logs del backend aparece "EMAIL SIMULADO [local-dev]" para el contratista |
| I7-EMAIL-02 | Verificar log de admin | `SIGCON_ADMIN_EMAIL` no configurado en local-dev | Log: "EMAIL ADMIN no configurado — omitiendo copia admin" |
| I7-EMAIL-03 | Verificar que la aprobación no se revierte si el email falla | Simular error de email | Estado permanece APROBADO; error registrado en log sin excepción |

> **Nota local-dev:** `sigcon.mail.enabled=false`. Los emails se simulan en logs. No se requiere SMTP real.

### 15.5 Búsqueda Administrativa Global

Entrar como `ADMIN`.

| ID | Acción | Datos | Esperado |
|---|---|---|---|
| I7-BUS-01 | Navegar a Admin → Búsqueda global | Clic en tarjeta "Búsqueda global" en el dashboard | Pantalla de búsqueda visible con formulario |
| I7-BUS-02 | Buscar por nombre de contratista | Texto: nombre parcial del contratista | Sección "Contratistas" muestra resultados con nombre y email |
| I7-BUS-03 | Buscar por número de contrato | Texto: "OPS-2026" | Sección "Contratos" muestra contratos que coinciden |
| I7-BUS-04 | Buscar por estado de informe | Texto: "APROBADO" | Sección "Informes" muestra informes en estado APROBADO |
| I7-BUS-05 | Filtrar por rango de periodo | fechaInicio=2026-05-01, fechaFin=2026-05-31 | Solo informes cuyo periodo cruza el rango aparecen |
| I7-BUS-06 | Buscar sin texto con rango de fechas | q vacío, fechas definidas | Todos los informes que cruzan el rango |
| I7-BUS-07 | Navegar al detalle de contrato | Clic en "Ver detalle" de un contrato | Redirige a `/contratos/{id}` |
| I7-BUS-08 | Navegar al detalle de informe | Clic en "Ver detalle" de un informe | Redirige a `/informes/{id}` |
| I7-BUS-09 | Acceso como CONTRATISTA | Navegar a `/admin/busqueda` | Redireccionado o acceso denegado |
| I7-BUS-10 | Búsqueda sin resultados | Texto inexistente | Las tres secciones muestran "Sin resultados." |

### 15.6 Pruebas Negativas I7

| ID | Caso | Esperado |
|---|---|---|
| I7-NEG-01 | Contratista intenta cargar documento de otro contratista | `POST /api/informes/{id}/documentos-requeridos/...` con informe ajeno | 403 ACCESO_DENEGADO |
| I7-NEG-02 | Cargar documento en informe ENVIADO | Informe en estado ENVIADO | 409 DOCUMENTO_REQUERIDO_NO_EDITABLE |
| I7-NEG-03 | Cargar archivo con extensión no permitida | `.xlsx`, `.docx`, `.png` | 400 DOCUMENTO_REQUERIDO_FORMATO_INVALIDO |
| I7-NEG-04 | Acceder a búsqueda global como REVISOR | `GET /api/admin/busqueda` | 403 Forbidden |
| I7-NEG-05 | Acceder a búsqueda global sin autenticar | Sin credenciales | 401 Unauthorized |

### 15.7 Diagnóstico de Errores Comunes I7

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| FACTURA no aparece en la lista | `responsableIva = false` en el usuario | Verificar campo en `SGCN_USUARIOS.RESPONSABLE_IVA` |
| Error al cargar archivo | Extensión no permitida o archivo vacío | Verificar que el archivo es `.pdf` o `.eml` |
| Preview EML vacío | EML complejo o sin cuerpo texto | Normal — `previewParcial=true`; descargar el original |
| Email no aparece en logs | `sigcon.mail.enabled=false` y log level INFO no visible | Verificar configuración de logging |
| Búsqueda no retorna resultados | Término no coincide con datos de prueba | Usar términos presentes en `db/01_datos_prueba.sql` |
| 403 en búsqueda como ADMIN | Sesión expirada o rol incorrecto | Cerrar sesión y volver a entrar como ADMIN |

---

## 16. Incremento 8 — Fecha de Elaboracion y PDF Formato Institucional SED 11-IF-023 V1

> Rama: `main` (mergeado directamente)
> Fecha: 2026-05-18
> Prerequisito de BD: ejecutar `db/05_add_fecha_elaboracion.sql` si el esquema es anterior a I8.

### 16.1 Campo `fechaElaboracion` en nuevo informe

Entrar como `CONTRATISTA`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I8-FE-01 | Abrir formulario de nuevo informe | Contrato en ejecucion | Campo "Fecha de Elaboracion" visible con valor por defecto = hoy |
| I8-FE-02 | Crear informe sin modificar la fecha | Dejar el default | Informe creado; `fechaElaboracion` guardada con la fecha actual |
| I8-FE-03 | Crear informe con fecha personalizada | Seleccionar una fecha anterior | Informe creado; `fechaElaboracion` refleja la fecha ingresada |
| I8-FE-04 | Dejar fecha en blanco y crear | Borrar el valor del campo | Informe creado; backend asigna la fecha actual como default |
| I8-FE-05 | Abrir informe en BORRADOR | Informe creado | Campo "Fecha de Elaboracion" editable |
| I8-FE-06 | Modificar `fechaElaboracion` en BORRADOR | Nueva fecha valida | Cambio persistido al guardar |
| I8-FE-07 | Abrir informe en ENVIADO | Informe enviado | Campo "Fecha de Elaboracion" en solo lectura |
| I8-FE-08 | Abrir informe en APROBADO | Informe aprobado | Campo "Fecha de Elaboracion" en solo lectura |

### 16.2 Campo `fechaElaboracion` al corregir informe devuelto

Entrar como `CONTRATISTA` con informe en estado `DEVUELTO`.

| ID | Accion | Datos | Esperado |
|---|---|---|---|
| I8-FE-09 | Abrir formulario de correccion | Informe DEVUELTO | Campo "Fecha de Elaboracion" pre-cargado con el valor guardado |
| I8-FE-10 | Modificar la fecha y enviar correccion | Nueva fecha valida | Informe pasa a ENVIADO con la nueva fecha |

### 16.3 PDF Formato Institucional SED 11-IF-023 V1

Prerequisito: tener un informe en estado `APROBADO` con actividades, aportes SGSSI y firmas de contratista y supervisor cargadas.

#### Estructura del PDF

| ID | Elemento a verificar | Esperado |
|---|---|---|
| I8-PDF-01 | Descargar PDF de informe APROBADO | Descarga exitosa; archivo valido |
| I8-PDF-02 | Encabezado en CADA pagina | Logo Alcaldia Mayor a la izquierda, numero y nombre del informe en el centro, periodo y codigo "11-IF-023 V1" a la derecha |
| I8-PDF-03 | Pie de pagina en CADA pagina | "Avenida El Dorado N° 66-63 · PBX: 3241000 · www.educacionbogota.edu.co · Linea 195" |
| I8-PDF-04 | Seccion 1 — Datos del Contrato | Contratista, objeto, valor (cifra), forma de pago, plazo, modificaciones, fechas, dependencia, supervisor |
| I8-PDF-05 | Seccion 2 — Ejecucion de Actividades | Tabla 3 columnas: Obligacion Contractual, Actividades realizadas, Evidencia Verificable |
| I8-PDF-06 | Seccion 2 — Evidencia con URL | Nombre del soporte subrayado si es URL |
| I8-PDF-07 | Seccion 3 — Aportes SGSSI | Tabla 5 columnas: ITEM, PERIODO PAGO (mes anterior al inicio del informe), FECHA DE PAGO, VALOR APORTADO, ENTIDAD |
| I8-PDF-08 | Seccion 3 — Periodo calculado | Informe inicio 2026-04-01 → periodo SGSSI "Marzo 2026" |
| I8-PDF-09 | Seccion 4 — Estado Radicacion | "SI" o "NO" segun `correspondenciaPendiente`; siempre dice "01 folios" |
| I8-PDF-10 | Seccion 5 — Declaracion Especial | Parrafo 1 (declaracion contratista), parrafo 2 (supervision autoriza desembolso No. X por valor Y, % Z), linea final con `fechaElaboracion` |
| I8-PDF-11 | Layout de firmas — Fila 1 | Firma contratista izquierda + firma supervisor derecha |
| I8-PDF-12 | Layout de firmas — Fila 2 (revisor con firma) | Celda centrada "Reviso — Apoyo a la Supervision" con imagen de firma |
| I8-PDF-13 | Texto introductorio de firmas | "Para constancia se firma por quienes en ella intervinieron al N dias del mes de MES de AAAA" |
| I8-PDF-14 | PDF sin logo | Retirar `logo-alcaldia.png` del classpath (prueba negativa) | PDF se genera sin error; encabezado sin imagen (columna izquierda vacia); log muestra WARN |

#### Casos de firma del revisor

| Escenario | Esperado en el PDF |
|---|---|
| Revisor con firma cargada | Fila 2 visible con imagen de firma del revisor |
| Revisor sin imagen de firma | Fila 2 ausente; PDF se genera sin error |
| Contrato sin revisor | Fila 2 ausente; PDF se genera sin error |

#### Verificacion de `fechaElaboracion` en PDF

| Escenario | Esperado en Seccion 5 |
|---|---|
| Informe con `fechaElaboracion` = 2026-04-15 | "15/04/2026" en la linea final de la seccion |
| Informe historico sin `fechaElaboracion` | Campo omitido o vacio (no NPE) |

### 16.4 Pruebas Negativas I8

| ID | Caso | Esperado |
|---|---|---|
| I8-NEG-01 | Arrancar backend sin ejecutar `db/05_add_fecha_elaboracion.sql` | Error de arranque: `Schema-validation: missing column [fecha_elaboracion]` |
| I8-NEG-02 | Descargar PDF de informe NO aprobado | 404 o error "PDF no disponible" |
| I8-NEG-03 | Contratista no asignado descarga el PDF | 403 ACCESO_DENEGADO |
| I8-NEG-04 | REVISOR intenta descargar el PDF | 403 ACCESO_DENEGADO |

### 16.5 Diagnostico de Errores Comunes I8

| Sintoma | Causa probable | Solucion |
|---------|----------------|----------|
| `Schema-validation: missing column [fecha_elaboracion]` | Migracion I8 no ejecutada | Ejecutar `db/05_add_fecha_elaboracion.sql` |
| `Cannot resolve method readAllBytes` al compilar | JDK anterior a Java 9 (correcto: Java 8 no tiene readAllBytes) | Verificar que el codigo usa `StreamUtils.copyToByteArray()` — ya corregido en main |
| PDF sin encabezado institucional | `logo-alcaldia.png` no empaquetado en el WAR | Verificar que el archivo existe en `src/main/resources/` y ejecutar `mvn clean package` |
| Encabezado sin logo en el PDF | Archivo presente pero no carga en classpath | Revisar logs de arranque: debe aparecer "WARN logo-alcaldia.png no encontrado" si falta; si el log no aparece, el logo carga correctamente |
| Seccion 3 sin periodo SGSSI | `fechaInicio` del informe es null | Verificar que el informe tiene periodo definido |
| NPE al generar PDF de informe historico | `fechaElaboracion` null sin null-check | Verificado y manejado en `InformePdfTemplateService`; si ocurre, reportar como bug |
| PDF no se regenera al aprobar de nuevo | PDF inmutable por diseno — `pdfRuta` ya existe | Comportamiento correcto; para forzar regeneracion, borrar la ruta en BD (solo en pruebas) |
