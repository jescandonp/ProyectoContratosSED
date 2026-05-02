package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeRequest;
import co.gov.bogota.sed.sigcon.application.service.ActividadInformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/informes/{informeId}/actividades")
@Tag(name = "Actividades de informe", description = "Actividades ejecutadas asociadas a informes I2")
public class ActividadInformeController {

    private final ActividadInformeService actividadInformeService;

    public ActividadInformeController(ActividadInformeService actividadInformeService) {
        this.actividadInformeService = actividadInformeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Agrega una actividad al informe")
    public ResponseEntity<ActividadInformeDto> crearActividad(
        @PathVariable Long informeId,
        @Valid @RequestBody ActividadInformeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actividadInformeService.crear(informeId, request));
    }

    @PutMapping("/{actividadId}")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Actualiza una actividad del informe")
    public ActividadInformeDto actualizarActividad(
        @PathVariable Long informeId,
        @PathVariable Long actividadId,
        @Valid @RequestBody ActividadInformeRequest request
    ) {
        return actividadInformeService.actualizar(informeId, actividadId, request);
    }

    @DeleteMapping("/{actividadId}")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Elimina lógicamente una actividad del informe")
    public ResponseEntity<Void> eliminarActividad(@PathVariable Long informeId, @PathVariable Long actividadId) {
        actividadInformeService.eliminar(informeId, actividadId);
        return ResponseEntity.noContent().build();
    }
}
