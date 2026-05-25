package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.parametro.CargaInformesRequest;
import co.gov.bogota.sed.sigcon.application.dto.parametro.ParametroVbDto;
import co.gov.bogota.sed.sigcon.application.service.NotificacionService;
import co.gov.bogota.sed.sigcon.application.service.ParametroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * I9: Gestión de parámetros del sistema.
 * Solo accesible por el rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/parametros")
@Tag(name = "Parámetros", description = "Gestión de parámetros del sistema — I9")
public class ParametroController {

    private final ParametroService parametroService;
    private final NotificacionService notificacionService;

    public ParametroController(ParametroService parametroService, NotificacionService notificacionService) {
        this.parametroService = parametroService;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene el estado actual del parámetro VB_ACTIVO")
    public ResponseEntity<ParametroVbDto> getParametros() {
        ParametroVbDto dto = new ParametroVbDto();
        dto.setActivo(parametroService.isVbActivo());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/vb-activo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activa o desactiva el Visto Bueno Administrativo",
               description = "Al desactivar, migra automáticamente todos los informes EN_VISTO_BUENO → EN_REVISION")
    public ResponseEntity<ParametroVbDto> putVbActivo(@RequestBody ParametroVbDto request) {
        parametroService.setVbActivo(request.isActivo());
        ParametroVbDto response = new ParametroVbDto();
        response.setActivo(parametroService.isVbActivo());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carga-informes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtiene el estado del control de carga de informes")
    public ResponseEntity<ParametroVbDto> getCargaInformes() {
        ParametroVbDto dto = new ParametroVbDto();
        dto.setActivo(parametroService.isCargaInformesActiva());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/carga-informes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activa o desactiva la carga de nuevos informes por contratistas")
    public ResponseEntity<ParametroVbDto> putCargaInformes(@Valid @RequestBody CargaInformesRequest request) {
        boolean anterior = parametroService.setCargaInformesActiva(request.getActivo());
        if (anterior && !request.getActivo()) {
            notificacionService.notificarBloqueoMasivo();
        }
        ParametroVbDto response = new ParametroVbDto();
        response.setActivo(parametroService.isCargaInformesActiva());
        return ResponseEntity.ok(response);
    }
}
