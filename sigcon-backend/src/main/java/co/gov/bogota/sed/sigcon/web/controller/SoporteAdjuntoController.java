package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteAdjuntoDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteUrlRequest;
import co.gov.bogota.sed.sigcon.application.service.SoporteAdjuntoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/actividades/{actividadId}/soportes")
@Tag(name = "Soportes de actividad", description = "Soportes URL o archivo de actividades I2")
public class SoporteAdjuntoController {

    private final SoporteAdjuntoService soporteAdjuntoService;

    public SoporteAdjuntoController(SoporteAdjuntoService soporteAdjuntoService) {
        this.soporteAdjuntoService = soporteAdjuntoService;
    }

    @PostMapping("/url")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Agrega un soporte por URL")
    public ResponseEntity<SoporteAdjuntoDto> agregarSoporteUrl(
        @PathVariable Long actividadId,
        @Valid @RequestBody SoporteUrlRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(soporteAdjuntoService.agregarSoporteUrl(actividadId, request));
    }

    @PostMapping("/archivo")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Agrega un soporte por archivo")
    public ResponseEntity<SoporteAdjuntoDto> agregarSoporteArchivo(
        @PathVariable Long actividadId,
        @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(soporteAdjuntoService.agregarSoporteArchivo(actividadId, file));
    }

    @DeleteMapping("/{soporteId}")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Elimina lógicamente un soporte")
    public ResponseEntity<Void> eliminarSoporte(@PathVariable Long actividadId, @PathVariable Long soporteId) {
        soporteAdjuntoService.eliminar(actividadId, soporteId);
        return ResponseEntity.noContent().build();
    }
}
