package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiDto;
import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiRequest;
import co.gov.bogota.sed.sigcon.application.service.AporteSgssiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/informes/{informeId}/aportes-sgssi")
@Tag(name = "Aportes SGSSI", description = "Aportes a seguridad social del informe I6")
public class AporteSgssiController {

    private final AporteSgssiService service;

    public AporteSgssiController(AporteSgssiService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista aportes SGSSI activos del informe")
    public ResponseEntity<List<AporteSgssiDto>> listar(@PathVariable Long informeId) {
        return ResponseEntity.ok(service.listar(informeId));
    }

    @PutMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Reemplaza todos los aportes SGSSI del informe")
    public ResponseEntity<List<AporteSgssiDto>> guardarTodos(
        @PathVariable Long informeId,
        @Valid @RequestBody List<AporteSgssiRequest> requests
    ) {
        return ResponseEntity.ok(service.guardarTodos(informeId, requests));
    }
}
