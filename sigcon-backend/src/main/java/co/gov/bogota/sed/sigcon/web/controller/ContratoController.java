package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoRequest;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.EstadoContratoRequest;
import co.gov.bogota.sed.sigcon.application.service.ContratoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    public Page<ContratoResumenDto> listarContratos(Pageable pageable) {
        return contratoService.listarContratos(pageable);
    }

    @GetMapping("/{id}")
    public ContratoDetalleDto obtenerDetalle(@PathVariable Long id) {
        return contratoService.obtenerDetalle(id);
    }

    @PostMapping
    public ResponseEntity<ContratoDetalleDto> crearContrato(@Valid @RequestBody ContratoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.crearContrato(request));
    }

    @PutMapping("/{id}")
    public ContratoDetalleDto actualizarContrato(@PathVariable Long id, @Valid @RequestBody ContratoRequest request) {
        return contratoService.actualizarContrato(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoContratoRequest request) {
        contratoService.cambiarEstado(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarContrato(@PathVariable Long id) {
        contratoService.eliminarContrato(id);
        return ResponseEntity.noContent().build();
    }
}
