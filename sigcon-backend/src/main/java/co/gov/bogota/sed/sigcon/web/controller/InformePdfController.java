package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * Descarga del PDF institucional del informe aprobado (I3).
 * El endpoint comparte la base /api/informes con InformeController.
 */
@RestController
@RequestMapping("/api/informes")
@Tag(name = "PDF", description = "Descarga del PDF institucional generado en la aprobacion I3")
public class InformePdfController {

    private final InformeService informeService;
    private final PdfInformeService pdfInformeService;

    public InformePdfController(InformeService informeService, PdfInformeService pdfInformeService) {
        this.informeService = informeService;
        this.pdfInformeService = pdfInformeService;
    }

    /**
     * Descarga el PDF del informe aprobado.
     * Retorna PDF_NO_DISPONIBLE si el informe no ha sido aprobado aun.
     */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('CONTRATISTA', 'SUPERVISOR', 'ADMIN')")
    @Operation(summary = "Descarga el PDF institucional del informe aprobado")
    public ResponseEntity<InputStreamResource> descargarPdf(@PathVariable Long id) throws IOException {
        Informe informe = informeService.findActiveInforme(id);
        InputStream inputStream = pdfInformeService.cargarPdf(informe);
        String filename = "informe-" + informe.getNumero() + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(new InputStreamResource(inputStream));
    }
}
