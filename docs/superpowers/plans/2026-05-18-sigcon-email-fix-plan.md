# Plan — Fix Notificaciones Email SIGCON
## Corrección gaps I3: INFORME_ENVIADO sin revisor + duplicado INFORME_APROBADO

> **Fecha:** 2026-05-18
> **Metodología:** SDD
> **Estado:** PENDIENTE

---

## Contexto

`EventoInformeService.publicar()` ya llama `emailService.enviar()` para los 5 eventos. La infraestructura
de email está completa. Existen dos gaps específicos:

### Gap 1 — INFORME_ENVIADO sin revisor (bug silencioso)
`resolverDestinatario()` retorna `contrato.getRevisor()` para `INFORME_ENVIADO`. Cuando el contrato
no tiene revisor (válido desde I4), el destinatario es `null`, `publicar()` logea una advertencia
y descarta el evento — **no se envía ni notificación in-app ni email**.

**Fix:** Fallback al supervisor cuando `getRevisor() == null`.

```java
case INFORME_ENVIADO:
    Usuario revisor = contrato.getRevisor();
    return revisor != null ? revisor : contrato.getSupervisor();
```

### Gap 2 — INFORME_APROBADO duplica email al contratista (bug)
En `InformeEstadoService.aprobar()`:
1. `eventoInformeService.publicar(INFORME_APROBADO, ...)` → llama `emailService.enviar(contratista, ...)`
2. `emailNotificacionService.notificarAprobacion(informe)` → llama `enviar(contratista, ...)` + `enviar(admin, ...)`

El contratista recibe **2 emails** cuando su informe es aprobado.

**Fix:** `notificarAprobacion()` solo envía la copia al admin. El email al contratista queda en manos de `publicar()`.

---

## T1 — Fix `EventoInformeService`

**Archivo:** `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/EventoInformeService.java`

Cambio en `resolverDestinatario()`:
```java
// ANTES:
case INFORME_ENVIADO: return contrato.getRevisor();

// DESPUÉS:
case INFORME_ENVIADO:
    Usuario rev = contrato.getRevisor();
    return rev != null ? rev : contrato.getSupervisor();
```

**Test nuevo** en `EventoInformeServiceTest` (crear si no existe):
- `informeEnviado_sinRevisor_notificaAlSupervisor()` — verifica que `notificacionService.crear()` se llama con el supervisor
- `informeEnviado_conRevisor_notificaAlRevisor()` — verifica que el revisor sigue siendo el destinatario cuando existe

**Gate:** tests GREEN.

---

## T2 — Fix `EmailNotificacionService.notificarAprobacion()`

**Archivo:** `sigcon-backend/src/main/java/co/gov/bogota/sed/sigcon/application/service/EmailNotificacionService.java`

Cambio: eliminar el envío al contratista de `notificarAprobacion()`, dejar solo el admin:

```java
public void notificarAprobacion(Informe informe) {
    // El email al contratista ya fue enviado por EventoInformeService.publicar()
    // Este método solo gestiona la copia adicional al correo administrador
    String adminEmail = mailProperties.getAdminEmail();
    if (adminEmail != null && !adminEmail.trim().isEmpty()) {
        Usuario adminVirtual = new Usuario();
        adminVirtual.setEmail(adminEmail.trim());
        adminVirtual.setNombre("Administrador SIGCON");
        enviar(adminVirtual, TipoEvento.INFORME_APROBADO, informe.getId(),
               construirDescripcionAprobacion(informe));
    } else {
        log.info("EMAIL ADMIN no configurado — omitiendo copia admin para informe id={}", informe.getId());
    }
}
```

**Test** en `EmailNotificacionServiceTest` (crear si no existe):
- `notificarAprobacion_conAdminConfigurado_soloEnviaAdmin()` — verifica que `enviar()` se llama 1 vez (no 2)
- `notificarAprobacion_sinAdminConfigurado_noEnviaCorreo()` — verifica que no se llama `enviar()` cuando admin no configurado

**Gate:** compilación + tests GREEN.

---

## Orden de ejecución

```
T1 → Fix INFORME_ENVIADO + tests  [mvn test -Dtest=EventoInformeServiceTest GREEN]
T2 → Fix notificarAprobacion + tests  [mvn test GREEN]
```

**Gate final:** suite completa sin regresiones.

---

*Plan generado mediante SDD — SIGCON — Fix Email — 2026-05-18.*
