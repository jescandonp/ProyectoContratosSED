package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionDto;
import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionesCountDto;
import co.gov.bogota.sed.sigcon.application.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de notificaciones in-app (I3).
 * Cada usuario accede unicamente a sus propias notificaciones.
 */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Notificaciones in-app del Incremento 3")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Lista las notificaciones del usuario autenticado, ordenadas por fecha descendente")
    public Page<NotificacionDto> listar(Pageable pageable) {
        return notificacionService.listar(pageable);
    }

    @GetMapping("/no-leidas/count")
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Retorna el número de notificaciones no leídas del usuario autenticado")
    public NotificacionesCountDto contarNoLeidas() {
        return notificacionService.contarNoLeidas();
    }

    @PatchMapping("/{id}/leida")
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Marca una notificación como leída; falla si no pertenece al usuario")
    public NotificacionDto marcarLeida(@PathVariable Long id) {
        return notificacionService.marcarLeida(id);
    }

    @PatchMapping("/leidas")
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'REVISOR', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Marca todas las notificaciones del usuario autenticado como leídas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }
}
