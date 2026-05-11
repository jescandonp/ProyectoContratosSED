package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoRequeridoDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.EmlPreviewDto;
import co.gov.bogota.sed.sigcon.application.service.DocumentoRequeridoInformeService;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoRequeridoInforme;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * I7: Endpoints para documentos requeridos del informe (PDF / EML).
 * Separado de /documentos-adicionales (documentos adicionales libres).
 */
@RestController
@RequestMapping("/api/informes/{informeId}/documentos-requeridos")
@Tag(name = "Documentos requeridos de informe", description = "I7 — Carga, descarga y preview de documentos requeridos (PDF/EML)")
public class DocumentoRequeridoInformeController {

    private final DocumentoRequeridoInformeService service;

    public DocumentoRequeridoInformeController(DocumentoRequeridoInformeService service) {
        this.service = service;
    }

    /**
     * Lista los documentos requeridos del informe.
     * Incluye FACTURA dinamica si el contratista es responsable de IVA.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista documentos requeridos del informe")
    public ResponseEntity<List<DocumentoRequeridoDto>> listar(@PathVariable Long informeId) {
        return ResponseEntity.ok(service.listar(informeId));
    }

    /**
     * Carga o reemplaza el archivo de un documento requerido.
     * Solo CONTRATISTA propietario, solo en BORRADOR o DEVUELTO.
     */
    @PostMapping("/{claveLogica}/archivo")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Carga o reemplaza el archivo de un documento requerido")
    public ResponseEntity<DocumentoRequeridoDto> cargarArchivo(
        @PathVariable Long informeId,
        @PathVariable String claveLogica,
        @RequestParam("archivo") MultipartFile archivo
    ) {
        DocumentoRequeridoDto dto = service.cargarArchivo(informeId, claveLogica, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Descarga el archivo de un documento requerido.
     * Cualquier usuario con acceso al informe puede descargar.
     */
    @GetMapping("/{documentoId}/archivo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descarga el archivo de un documento requerido")
    public ResponseEntity<InputStreamResource> descargarArchivo(
        @PathVariable Long informeId,
        @PathVariable Long documentoId
    ) {
        DocumentoRequeridoInforme registro = service.obtenerRegistroParaDescarga(informeId, documentoId);
        InputStream is = service.descargarArchivo(informeId, documentoId);

        String contentType = registro.getContentType() != null
            ? registro.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String nombreArchivo = registro.getNombreArchivo() != null
            ? registro.getNombreArchivo()
            : registro.getClaveLogica() + (registro.getExtension() != null ? registro.getExtension() : "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(nombreArchivo, StandardCharsets.UTF_8)
                .build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType(contentType))
            .body(new InputStreamResource(is));
    }

    /**
     * Preview basico de un archivo EML.
     * Retorna asunto, remitente, destinatarios, fecha y cuerpo texto.
     * Cualquier usuario con acceso al informe puede previsualizar.
     */
    @GetMapping("/{documentoId}/preview")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Preview basico de un archivo EML")
    public ResponseEntity<EmlPreviewDto> previewEml(
        @PathVariable Long informeId,
        @PathVariable Long documentoId
    ) {
        return ResponseEntity.ok(service.previewEml(informeId, documentoId));
    }

    /**
     * Elimina (soft-delete) el archivo de un documento requerido.
     * Solo CONTRATISTA propietario, solo en BORRADOR o DEVUELTO.
     */
    @DeleteMapping("/{documentoId}/archivo")
    @PreAuthorize("hasRole('CONTRATISTA')")
    @Operation(summary = "Elimina el archivo de un documento requerido")
    public ResponseEntity<Void> eliminarArchivo(
        @PathVariable Long informeId,
        @PathVariable Long documentoId
    ) {
        service.eliminarArchivo(informeId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
