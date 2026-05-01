package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoDto;
import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoRequest;
import co.gov.bogota.sed.sigcon.application.service.DocumentoCatalogoService;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/documentos-catalogo")
public class DocumentoCatalogoController {

    private final DocumentoCatalogoService documentoCatalogoService;

    public DocumentoCatalogoController(DocumentoCatalogoService documentoCatalogoService) {
        this.documentoCatalogoService = documentoCatalogoService;
    }

    @GetMapping
    public Page<DocumentoCatalogoDto> listar(@RequestParam(required = false) TipoContrato tipoContrato, Pageable pageable) {
        return documentoCatalogoService.listar(tipoContrato, pageable);
    }

    @PostMapping
    public ResponseEntity<DocumentoCatalogoDto> crear(@Valid @RequestBody DocumentoCatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoCatalogoService.crear(request));
    }

    @PutMapping("/{id}")
    public DocumentoCatalogoDto actualizar(@PathVariable Long id, @Valid @RequestBody DocumentoCatalogoRequest request) {
        return documentoCatalogoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoCatalogoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
