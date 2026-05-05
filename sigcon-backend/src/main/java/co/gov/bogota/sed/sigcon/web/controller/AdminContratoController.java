package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoRequest;
import co.gov.bogota.sed.sigcon.application.service.ContratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Endpoints de administracion de contratos restringidos a rol ADMIN.
 * La ruta /api/admin/** esta cubierta por hasRole('ADMIN') en SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/contratos")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Contratos", description = "Gestion administrativa de contratos (solo ADMIN)")
public class AdminContratoController {

    private final ContratoService contratoService;

    public AdminContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @Operation(summary = "Actualizar contrato",
               description = "Permite al ADMIN modificar cualquier campo de un contrato activo en cualquier estado.")
    @PutMapping("/{id}")
    public ResponseEntity<ContratoDetalleDto> actualizarContrato(
            @PathVariable Long id,
            @Valid @RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.actualizarContrato(id, request));
    }
}
