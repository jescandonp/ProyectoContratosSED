package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalRequest;
import co.gov.bogota.sed.sigcon.application.service.DocumentoAdicionalInformeService;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/informes/{informeId}/documentos-adicionales")
@Tag(name = "Documentos adicionales de informe", description = "Documentos complementarios exigidos en I2")
public class DocumentoAdicionalInformeController {

    private final DocumentoAdicionalInformeService documentoAdicionalInformeService;

    public DocumentoAdicionalInformeController(DocumentoAdicionalInformeService documentoAdicionalInformeService) {
        this.documentoAdicionalInformeService = documentoAdicionalInformeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Agrega un documento adicional al informe")
    public ResponseEntity<DocumentoAdicionalDto> agregarDocumento(
        @PathVariable Long informeId,
        @Valid @RequestBody DocumentoAdicionalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoAdicionalInformeService.agregar(informeId, request));
    }

    @DeleteMapping("/{documentoId}")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Elimina lógicamente un documento adicional")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Long informeId, @PathVariable Long documentoId) {
        documentoAdicionalInformeService.eliminar(informeId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
