# Spec de Ajuste — SIGCON I9
## Acceso del perfil Admin al Visto Bueno Administrativo

> Metodologia: Spec-Driven Development (SDD) — ajuste sobre I9  
> Fecha: 2026-05-22  
> Estado: PROPUESTO  
> Fuente base: `docs/specs/2026-05-19-sigcon-i9-spec.md`, validacion funcional del informe `100`

---

## 1. Contexto

Durante la validacion funcional del flujo de Visto Bueno Administrativo se confirmo:

- La base de datos contiene un informe activo en `EN_VISTO_BUENO`.
- El endpoint `GET /api/informes/cola/visto-bueno` retorna el informe `100` cuando se invoca con credenciales de `administrativo@educacionbogota.edu.co`.
- La pantalla no mostraba el informe porque la sesion activa era `ADMIN`, no `ADMINISTRATIVO`.
- En la administracion de usuarios, el formulario actual permite crear `ADMIN`, `CONTRATISTA`, `REVISOR` y `SUPERVISOR`, pero no expone `ADMINISTRATIVO`.

El hallazgo no es una falla de maquina de estados. Es una decision de producto pendiente: si el usuario administrador funcional debe operar tambien el Visto Bueno, o si debe existir un perfil separado `ADMINISTRATIVO`.

---

## 2. Decision Recomendada

Mantener `ADMIN` y `ADMINISTRATIVO` como roles distintos en el modelo de dominio, pero habilitar lo siguiente:

1. El rol `ADMIN` puede acceder a la cola y acciones de Visto Bueno Administrativo.
2. El formulario de usuarios permite crear y editar usuarios con rol `ADMINISTRATIVO`.
3. La UI del sidebar muestra "Visto Bueno" para `ADMIN` y `ADMINISTRATIVO`.
4. Las acciones de Visto Bueno siguen registrandose como actor funcional `ADMINISTRATIVO` en observaciones/eventos, aunque el usuario autenticado sea `ADMIN`.

Esta opcion evita una migracion destructiva o ambigua de roles, preserva el rol `ADMIN` como administrador global, y permite que el administrador funcional atienda el flujo cuando la operacion real no tenga un usuario administrativo separado.

---

## 3. Alternativas Evaluadas

| Alternativa | Descripcion | Ventajas | Riesgos |
|---|---|---|---|
| A. Fusionar roles | Eliminar `ADMINISTRATIVO` y usar solo `ADMIN` para administracion y VB | Menos perfiles visibles | Rompe la separacion de funciones de I9, requiere migraciones, cambia tests y puede ampliar privilegios operativos sin trazabilidad |
| B. Mantener roles separados | Solo agregar `ADMINISTRATIVO` al formulario de usuarios | Respeta I9 estrictamente | El usuario espera que "Admin" pueda operar VB; seguiria existiendo confusion funcional |
| C. ADMIN con capacidad VB | Mantener ambos roles, pero permitir que `ADMIN` tambien opere VB | Menor impacto, compatible con la operacion observada, no elimina trazabilidad | Requiere ajustar reglas de seguridad y pruebas negativas I9 |

Decision propuesta: **Alternativa C**.

---

## 4. Impacto Funcional

### 4.1 Usuarios

El formulario de creacion/edicion de usuarios debe incluir:

| Etiqueta UI | Valor tecnico |
|---|---|
| Contratista | `CONTRATISTA` |
| Revisor | `REVISOR` |
| Supervisor | `SUPERVISOR` |
| Admin | `ADMIN` |
| Administrativo | `ADMINISTRATIVO` |

### 4.2 Navegacion

El item **Visto Bueno** debe estar visible para:

- `ADMIN`
- `ADMINISTRATIVO`

El item **Administracion** sigue visible solo para `ADMIN`.

### 4.3 Backend

Los endpoints de Visto Bueno deben aceptar:

| Metodo | Ruta | Roles permitidos |
|---|---|---|
| `GET` | `/api/informes/cola/visto-bueno` | `ADMIN`, `ADMINISTRATIVO` |
| `POST` | `/api/informes/{id}/dar-visto-bueno` | `ADMIN`, `ADMINISTRATIVO` |
| `POST` | `/api/informes/{id}/escalar` | `ADMIN`, `ADMINISTRATIVO` |
| `POST` | `/api/informes/{id}/devolver` desde `EN_VISTO_BUENO` | `ADMIN`, `ADMINISTRATIVO` |

`GET /api/admin/parametros` y `PUT /api/admin/parametros/vb-activo` permanecen solo para `ADMIN`.

### 4.4 Servicio de Aplicacion

`InformeService.listarColaVistoBueno()` actualmente exige `usuario.getRol() == ADMINISTRATIVO`. Debe aceptar tambien `ADMIN`.

Las acciones de estado (`darVistosBueno`, `escalar`, `devolverDesdeVistoBueno`) no dependen de `CurrentUserService`; el control de rol ocurre en el controller/security. No requieren cambio de dominio salvo que se agregue auditoria explicita por usuario ejecutor.

### 4.5 Seguridad

El cambio amplia permisos operativos del rol `ADMIN`. Debe quedar cubierto por tests:

- `ADMIN` puede listar cola VB.
- `ADMIN` puede dar visto bueno.
- `ADMIN` puede escalar.
- `ADMIN` puede devolver desde VB.
- `ADMINISTRATIVO` no obtiene acceso a administracion global.
- `CONTRATISTA`, `REVISOR` y `SUPERVISOR` siguen sin acceso a la cola VB.

---

## 5. Impacto de Datos

No se requiere migracion de datos obligatoria.

Se conserva:

- `RolUsuario.ADMIN`
- `RolUsuario.ADMINISTRATIVO`
- constraint Oracle `CHK_USUARIOS_ROL` permitiendo ambos valores

Se recomienda ajustar datos semilla/local-dev para mantener ambos usuarios:

- `admin@educacionbogota.edu.co` con rol `ADMIN`
- `administrativo@educacionbogota.edu.co` con rol `ADMINISTRATIVO`

---

## 6. Impacto de Frontend

Archivos esperados:

| Archivo | Ajuste |
|---|---|
| `core/models/usuario.model.ts` | Ya incluye `ADMINISTRATIVO`; verificar uso en formularios |
| `features/admin/usuarios` o componente equivalente | Agregar opcion `ADMINISTRATIVO` en selector de rol |
| `shared/components/sidebar/sidebar.component.ts` | Mostrar "Visto Bueno" para `ADMIN` o `ADMINISTRATIVO` |
| `app.routes.ts` | Permitir `/visto-bueno` y `/visto-bueno/:id` para `ADMIN` o `ADMINISTRATIVO` |
| `core/auth/dev-session.service.ts` | Corregir `ADMINISTRATIVO.id` local-dev de `5` a `6` |

---

## 7. Impacto de Documentacion

Actualizar:

- `docs/GUIA_PRUEBAS_FUNCIONALES.md`: I9-ACL-04 deja de esperar acceso denegado para `ADMIN`; ahora `ADMIN` puede abrir Visto Bueno.
- `docs/ARRANQUE.md`: explicar diferencia entre `ADMIN` y `ADMINISTRATIVO`, y que ambos pueden operar VB en local-dev.
- `docs/specs/2026-05-19-sigcon-i9-spec.md`: agregar nota de ajuste o referencia a esta spec.

---

## 8. Criterios de Aceptacion

| ID | Criterio | Verificacion |
|---|---|---|
| AC-AJ-01 | `ADMIN` ve el item "Visto Bueno" en sidebar | Prueba UI local-dev |
| AC-AJ-02 | `ADMIN` abre `/visto-bueno` y ve informes `EN_VISTO_BUENO` | Smoke test con informe `100` |
| AC-AJ-03 | `ADMINISTRATIVO` sigue pudiendo abrir `/visto-bueno` | Smoke test |
| AC-AJ-04 | Formulario de usuarios permite seleccionar `ADMINISTRATIVO` | Prueba funcional admin |
| AC-AJ-05 | Backend autoriza cola VB para `ADMIN` y `ADMINISTRATIVO` | `mvn test -Dtest=InformeControllerVbTest,SigconBackendSecurityTest` |
| AC-AJ-06 | Otros roles siguen bloqueados en cola VB | Tests seguridad |
| AC-AJ-07 | `ADMINISTRATIVO` no puede acceder a `/api/admin/**` | Tests seguridad |
| AC-AJ-08 | Build Angular sin errores | `npm test` o `ng build` segun plan |

---

## 9. Pregunta de Aprobacion

Decision a aprobar antes de implementar:

> En SIGCON, `ADMIN` mantiene sus privilegios globales y ademas puede operar Visto Bueno Administrativo. `ADMINISTRATIVO` permanece como perfil operativo separado para usuarios que solo atienden Visto Bueno y no administran el sistema.

