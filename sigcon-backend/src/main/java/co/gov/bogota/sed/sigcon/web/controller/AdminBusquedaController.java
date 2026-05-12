package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminResponse;
import co.gov.bogota.sed.sigcon.application.service.BusquedaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * I7: Búsqueda administrativa global.
 * Ruta: GET /api/admin/busqueda?q=&fechaInicio=&fechaFin=
 * Solo ADMIN — cubierto por hasRole('ADMIN') en SecurityConfig y @PreAuthorize.
 */
@RestController
@RequestMapping("/api/admin/busqueda")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Búsqueda global", description = "I7 — Búsqueda global por contratista, contrato e informe (solo ADMIN)")
public class AdminBusquedaController {

    private final BusquedaAdminService busquedaAdminService;

    public AdminBusquedaController(BusquedaAdminService busquedaAdminService) {
        this.busquedaAdminService = busquedaAdminService;
    }

    @GetMapping
    @Operation(
        summary = "Búsqueda administrativa global",
        description = "Busca contratistas, contratos e informes por texto libre. "
            + "El rango fechaInicio/fechaFin filtra por periodo del informe."
    )
    public ResponseEntity<BusquedaAdminResponse> buscar(
        @RequestParam(required = false, defaultValue = "") String q,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return ResponseEntity.ok(busquedaAdminService.buscar(q, fechaInicio, fechaFin));
    }
}
