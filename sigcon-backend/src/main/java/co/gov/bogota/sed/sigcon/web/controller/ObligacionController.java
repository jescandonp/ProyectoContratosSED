package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionDto;
import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionRequest;
import co.gov.bogota.sed.sigcon.application.service.ObligacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/contratos/{contratoId}/obligaciones")
public class ObligacionController {

    private final ObligacionService obligacionService;

    public ObligacionController(ObligacionService obligacionService) {
        this.obligacionService = obligacionService;
    }

    @GetMapping
    public List<ObligacionDto> listarPorContrato(@PathVariable Long contratoId) {
        return obligacionService.listarPorContrato(contratoId);
    }

    @PostMapping
    public ResponseEntity<ObligacionDto> crear(@PathVariable Long contratoId, @Valid @RequestBody ObligacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(obligacionService.crear(contratoId, request));
    }

    @PutMapping("/{id}")
    public ObligacionDto actualizar(
        @PathVariable Long contratoId,
        @PathVariable Long id,
        @Valid @RequestBody ObligacionRequest request
    ) {
        return obligacionService.actualizar(contratoId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long contratoId, @PathVariable Long id) {
        obligacionService.eliminar(contratoId, id);
        return ResponseEntity.noContent().build();
    }
}
