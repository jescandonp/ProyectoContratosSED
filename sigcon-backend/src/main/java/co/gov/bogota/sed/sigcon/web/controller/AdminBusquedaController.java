package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminFiltros;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminPageResponse;
import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminResponse;
import co.gov.bogota.sed.sigcon.application.service.BusquedaAdminService;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
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
 * I7/T11: Búsqueda administrativa global.
 * <ul>
 *   <li>GET /api/admin/busqueda — búsqueda simple T8 (compatibilidad)</li>
 *   <li>GET /api/admin/busqueda/avanzada — búsqueda con filtros combinados T11</li>
 * </ul>
 * Solo ADMIN — cubierto por hasRole('ADMIN') en SecurityConfig y @PreAuthorize.
 */
@RestController
@RequestMapping("/api/admin/busqueda")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Búsqueda global", description = "I7/T11 — Búsqueda global por contratista, contrato e informe (solo ADMIN)")
public class AdminBusquedaController {

    private final BusquedaAdminService busquedaAdminService;

    public AdminBusquedaController(BusquedaAdminService busquedaAdminService) {
        this.busquedaAdminService = busquedaAdminService;
    }

    /**
     * T8 legacy: búsqueda simple por texto libre + rango de fechas.
     * Retorna grupos separados de contratistas, contratos e informes.
     */
    @GetMapping
    @Operation(
        summary = "Búsqueda administrativa global (simple)",
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

    /**
     * T11: búsqueda avanzada con filtros combinados, paginación y ordenamiento.
     * Retorna contratos con sus informes anidados que cumplen los filtros.
     */
    @GetMapping("/avanzada")
    @Operation(
        summary = "Búsqueda administrativa avanzada (T11)",
        description = "Filtros combinados: texto libre, estado contrato, periodo informe, "
            + "contratista, revisor, estado informe. Paginación de 20 registros. "
            + "Ordenamiento: periodo más reciente, prioridad operativa de estado, número contrato, contratista."
    )
    public ResponseEntity<BusquedaAdminPageResponse> buscarAvanzado(
        @RequestParam(required = false, defaultValue = "") String q,
        @RequestParam(required = false) EstadoContrato estadoContrato,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
        @RequestParam(required = false) Long contratistaId,
        @RequestParam(required = false) Long revisorId,
        @RequestParam(required = false) EstadoInforme estadoInforme,
        @RequestParam(required = false, defaultValue = "0") int pagina,
        @RequestParam(required = false, defaultValue = "20") int tamano
    ) {
        BusquedaAdminFiltros filtros = new BusquedaAdminFiltros();
        filtros.setQ(q);
        filtros.setEstadoContrato(estadoContrato);
        filtros.setFechaInicio(fechaInicio);
        filtros.setFechaFin(fechaFin);
        filtros.setContratistaId(contratistaId);
        filtros.setRevisorId(revisorId);
        filtros.setEstadoInforme(estadoInforme);
        filtros.setPagina(pagina);
        filtros.setTamano(tamano);
        return ResponseEntity.ok(busquedaAdminService.buscarConFiltros(filtros));
    }
}
