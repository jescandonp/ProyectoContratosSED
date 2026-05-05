# SIGCON - Guia De Pruebas Funcionales

Estado: guia operativa para pruebas manuales I1-I3  
Fecha: 2026-05-04  
Rama objetivo: `feat/sigcon-i3`  
Marco: SDD Spec-Anchored por incrementos

## 1. Objetivo

Validar funcionalmente SIGCON desde la base tecnica hasta el flujo completo de contratos, informes, revision, aprobacion, PDF y notificaciones.

Esta guia cubre:

- Incremento 1: autenticacion local-dev, usuarios, contratos, catalogo y acceso por rol.
- Incremento 2: creacion y gestion de informes, actividades, soportes, documentos y revision.
- Incremento 3: aprobacion final, PDF institucional y notificaciones.

## 2. Prerrequisitos

### 2.1 Base De Datos Oracle

El esquema local debe existir y tener los scripts ejecutados:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED

sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/00_setup.sql
sqlplus SED_SIGCON/Sigcon2026Local1@localhost:1521/XEPDB1 @db/01_datos_prueba.sql
```

Si el usuario ya existe y los scripts ya fueron ejecutados, no repetir `00_setup.sql` sobre el mismo esquema porque no es idempotente.

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
| Guardar actividad con porcentaje > 100 | Error inline: "El porcentaje debe estar entre 0 y 100." |
| Agregar documento sin seleccionar tipo | Error inline: "Seleccione el tipo de documento e ingrese la referencia." |

### Diagnóstico de errores comunes I5

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| 403 al guardar actividad | Contratista no es propietario del informe | Verificar usuario autenticado |
| 409 al guardar actividad | Informe no está en BORRADOR | Recargar la página |
| Campos no editables en BORRADOR | Build desactualizado | Limpiar caché y reconstruir |
| Soporte no aparece tras guardar | Error silencioso en la recarga | Verificar consola del navegador |
