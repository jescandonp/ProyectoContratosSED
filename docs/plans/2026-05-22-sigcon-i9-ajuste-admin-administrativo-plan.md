# Plan de Implementacion — Ajuste I9 Admin / Administrativo

> Spec: `docs/specs/2026-05-22-sigcon-i9-ajuste-admin-administrativo.md`  
> Fecha: 2026-05-22  
> Estado: PROPUESTO  
> Rama objetivo: pendiente de definir antes de implementar

---

## Objetivo

Permitir que el perfil `ADMIN` opere el flujo de Visto Bueno Administrativo sin eliminar el rol `ADMINISTRATIVO`, y exponer `ADMINISTRATIVO` en la gestion de usuarios.

---

## Alcance

Incluye:

- Backend: autorizar endpoints VB para `ADMIN` y `ADMINISTRATIVO`.
- Backend: permitir que `InformeService.listarColaVistoBueno()` acepte ambos roles.
- Frontend: rutas y sidebar de Visto Bueno visibles para ambos roles.
- Frontend: selector de rol de usuarios incluye `ADMINISTRATIVO`.
- Frontend: corregir usuario local-dev `ADMINISTRATIVO.id` a `6`.
- Tests backend y frontend enfocados.
- Documentacion de pruebas y arranque.

No incluye:

- Eliminar el rol `ADMINISTRATIVO`.
- Migrar usuarios `ADMINISTRATIVO` a `ADMIN`.
- Cambiar la maquina de estados.
- Cambiar la semantica de `VB_ACTIVO`.

---

## Tareas

### T0 — Aprobacion de Decision

- [ ] Revisar y aprobar la decision de la spec de ajuste.
- [ ] Definir rama de trabajo.
- [ ] Confirmar si se requiere commit separado solo documental.

Gate: aprobacion explicita del usuario.

### T1 — Backend Seguridad y Servicio

Archivos esperados:

- `sigcon-backend/src/main/java/.../config/SecurityConfig.java`
- `sigcon-backend/src/main/java/.../config/DevSecurityConfig.java`
- `sigcon-backend/src/main/java/.../web/controller/InformeController.java`
- `sigcon-backend/src/main/java/.../application/service/InformeService.java`

Cambios:

- [ ] Cambiar reglas de `GET /api/informes/cola/visto-bueno` a `hasAnyRole('ADMIN','ADMINISTRATIVO')`.
- [ ] Cambiar reglas de `POST /dar-visto-bueno` a `hasAnyRole('ADMIN','ADMINISTRATIVO')`.
- [ ] Cambiar reglas de `POST /escalar` a `hasAnyRole('ADMIN','ADMINISTRATIVO')`.
- [ ] Confirmar `POST /devolver` acepta `ADMIN` solo cuando corresponde al flujo VB o ajustar controller sin abrir devolucion final indebidamente.
- [ ] Cambiar `InformeService.listarColaVistoBueno()` para aceptar `ADMIN` y `ADMINISTRATIVO`.

Tests:

- [ ] Agregar caso `admin_puedeAcceder_colaVistoBueno`.
- [ ] Agregar caso `admin_puedeDarVistoBueno`.
- [ ] Mantener caso `administrativo_puedeAcceder_colaVistoBueno`.
- [ ] Mantener negativos para `CONTRATISTA`, `REVISOR`, `SUPERVISOR`.

Gate:

```powershell
mvn test "-Dtest=InformeServiceTest,InformeControllerVbTest,SigconBackendSecurityTest"
```

### T2 — Frontend Rutas y Navegacion

Archivos esperados:

- `sigcon-angular/src/app/app.routes.ts`
- `sigcon-angular/src/app/shared/components/sidebar/sidebar.component.ts`
- `sigcon-angular/src/app/core/auth/dev-session.service.ts`

Cambios:

- [ ] Permitir rutas `/visto-bueno` y `/visto-bueno/:id` para roles `ADMIN` y `ADMINISTRATIVO`.
- [ ] Mostrar item "Visto Bueno" en sidebar para `ADMIN` y `ADMINISTRATIVO`.
- [ ] Corregir `ADMINISTRATIVO.id` local-dev de `5` a `6`.
- [ ] Verificar que `ADMIN` conserva acceso a administracion global.
- [ ] Verificar que `ADMINISTRATIVO` no recibe acceso a administracion global.

Gate:

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test -- --watch=false
```

o, si el proyecto no tiene tests frontend estables:

```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

### T3 — Frontend Gestion de Usuarios

Archivos a ubicar antes de editar:

- Componente de usuarios en `sigcon-angular/src/app/features/admin/...`
- Servicio/modelo de usuarios en `sigcon-angular/src/app/core/...`

Cambios:

- [ ] Agregar opcion visual `Administrativo` con valor `ADMINISTRATIVO` en el selector de rol.
- [ ] Verificar creacion de usuario `ADMINISTRATIVO` desde UI.
- [ ] Verificar edicion de rol hacia y desde `ADMINISTRATIVO`.
- [ ] Confirmar que backend ya acepta `ADMINISTRATIVO` en enum y constraint.

Gate:

- Crear usuario de prueba con rol `ADMINISTRATIVO`.
- Confirmar en BD:

```sql
SELECT email, rol, activo
FROM sgcn_usuarios
WHERE rol = 'ADMINISTRATIVO'
ORDER BY id DESC;
```

### T4 — Documentacion

Archivos:

- `docs/GUIA_PRUEBAS_FUNCIONALES.md`
- `docs/ARRANQUE.md`
- Opcional: `docs/specs/2026-05-19-sigcon-i9-spec.md`

Cambios:

- [ ] Actualizar I9-ACL-04: `ADMIN` ahora puede abrir Visto Bueno.
- [ ] Agregar caso de prueba `ADMIN` opera cola VB.
- [ ] Documentar que `ADMINISTRATIVO` es perfil operativo limitado.
- [ ] Documentar que `ADMIN` tambien puede operar VB.
- [ ] Corregir referencias a `vb.activo=true` si aplican; la llave real es `VB_ACTIVO` con valor `S`.

### T5 — Smoke Test Manual

Precondicion: informe `100` o equivalente en `EN_VISTO_BUENO`.

- [ ] Login como `ADMIN`.
- [ ] Confirmar sidebar muestra "Visto Bueno".
- [ ] Abrir `/visto-bueno`.
- [ ] Confirmar aparece el informe `100`.
- [ ] Abrir detalle.
- [ ] Ejecutar una accion controlada solo si hay datos de prueba preparados.
- [ ] Login como `ADMINISTRATIVO`.
- [ ] Confirmar mismo acceso operativo a cola.
- [ ] Login como `REVISOR` o `SUPERVISOR`.
- [ ] Confirmar acceso denegado a `/visto-bueno`.

### T6 — Cierre

- [ ] Registrar comandos ejecutados y resultados.
- [ ] Actualizar execution log si se abre uno para este ajuste.
- [ ] Commit documental y/o implementacion segun aprobacion.

---

## Riesgos y Controles

| Riesgo | Control |
|---|---|
| `ADMIN` queda con privilegios operativos adicionales no esperados | Documentar explicitamente y cubrir con tests |
| Se abre `/api/admin/**` a `ADMINISTRATIVO` por error | Mantener reglas `hasRole('ADMIN')` y tests negativos |
| `POST /devolver` queda demasiado amplio para `ADMIN` | Validar estado `EN_VISTO_BUENO` y actor antes de permitir ruta funcional |
| Usuario local-dev tiene id desalineado con BD | Corregir `ADMINISTRATIVO.id = 6` |
| Documentacion I9 queda contradictoria | Actualizar guia funcional y referenciar esta spec de ajuste |

---

## Comandos de Verificacion Propuestos

Backend:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-backend
mvn test "-Dtest=InformeServiceTest,InformeControllerVbTest,SigconBackendSecurityTest"
```

Frontend:

```powershell
cd C:\Users\jmep2\Downloads\AgenIALab\ProyectoContratosSED\sigcon-angular
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" run build
```

Smoke API:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/informes/cola/visto-bueno?page=0&size=10" `
  -Method GET `
  -Credential (New-Object System.Management.Automation.PSCredential(
    "admin@educacionbogota.edu.co",
    (ConvertTo-SecureString "admin123" -AsPlainText -Force)
  )) | ConvertTo-Json -Depth 10
```

