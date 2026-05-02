package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Coordina la generacion, almacenamiento, hash e inmutabilidad del PDF institucional.
 * Punto de entrada unico para operaciones de PDF; llamado por InformeEstadoService.aprobar().
 */
@Service
public class PdfInformeService {

    private static final Logger log = LoggerFactory.getLogger(PdfInformeService.class);

    private final InformePdfTemplateService templateService;
    private final DocumentStorageService storageService;
    private final InformeRepository informeRepository;

    public PdfInformeService(InformePdfTemplateService templateService,
                             DocumentStorageService storageService,
                             InformeRepository informeRepository) {
        this.templateService = templateService;
        this.storageService = storageService;
        this.informeRepository = informeRepository;
    }

    /**
     * Genera y persiste el PDF del informe aprobado.
     *
     * <p>Reglas:</p>
     * <ul>
     *   <li>Si ya existe {@code pdfRuta}, no regenera (PDF inmutable).</li>
     *   <li>Si falta firma del contratista o supervisor lanza {@code FIRMA_REQUERIDA}.</li>
     *   <li>Si el proceso falla lanza {@code PDF_GENERACION_FALLIDA}; el estado del informe no cambia.</li>
     * </ul>
     *
     * @param informe informe con contrato, contratista y supervisor inicializados
     */
    @Transactional
    public void generarYPersistir(Informe informe) {
        // Inmutabilidad: no regenerar si ya existe
        if (informe.getPdfRuta() != null && !informe.getPdfRuta().isEmpty()) {
            log.debug("PDF ya existe para informe {}, omitiendo generacion", informe.getId());
            return;
        }

        // Validar firmas
        Usuario contratista = informe.getContrato().getContratista();
        Usuario supervisor  = informe.getContrato().getSupervisor();

        if (contratista == null || contratista.getFirmaImagen() == null) {
            throw new SigconBusinessException(
                ErrorCode.FIRMA_REQUERIDA,
                "El contratista no tiene firma registrada. Debe cargar su firma antes de aprobar el informe.",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        if (supervisor == null || supervisor.getFirmaImagen() == null) {
            throw new SigconBusinessException(
                ErrorCode.FIRMA_REQUERIDA,
                "El supervisor no tiene firma registrada. Debe cargar su firma antes de aprobar el informe.",
                HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        try {
            // Leer bytes de firma desde la ruta almacenada
            byte[] firmaContratista = readSignatureBytes(contratista);
            byte[] firmaSupervisor  = readSignatureBytes(supervisor);

            // Generar PDF
            byte[] pdfBytes = templateService.generarPdf(informe, firmaContratista, firmaSupervisor);

            // Calcular hash SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pdfBytes);
            String hash = Base64.getEncoder().encodeToString(hashBytes);

            // Almacenar PDF
            String subdir = "pdfs/" + informe.getContrato().getId() + "/" + informe.getId();
            String filename = "informe-" + informe.getNumero() + ".pdf";
            String ruta = storageService.storeBytes(subdir, filename, pdfBytes);

            // Persistir metadatos en el informe
            informe.setPdfRuta(ruta);
            informe.setPdfHash(hash);
            informe.setPdfGeneradoAt(LocalDateTime.now());
            informeRepository.save(informe);

            log.info("PDF generado para informe {} en ruta {}", informe.getId(), ruta);

        } catch (SigconBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generando PDF para informe {}: {}", informe.getId(), e.getMessage(), e);
            throw new SigconBusinessException(
                ErrorCode.PDF_GENERACION_FALLIDA,
                "No fue posible generar el PDF del informe: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Carga el InputStream del PDF aprobado para descarga.
     *
     * @param informe informe con {@code pdfRuta} definida
     * @return InputStream del archivo PDF
     */
    public InputStream cargarPdf(Informe informe) throws java.io.IOException {
        if (informe.getPdfRuta() == null || informe.getPdfRuta().isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.PDF_NO_DISPONIBLE,
                "El informe no tiene un PDF generado. Debe estar en estado APROBADO.",
                HttpStatus.NOT_FOUND
            );
        }
        return storageService.loadFile(informe.getPdfRuta());
    }

    private byte[] readSignatureBytes(Usuario usuario) throws java.io.IOException {
        // firmaImagen contiene la ruta relativa almacenada por LocalDocumentStorageService
        try (InputStream is = storageService.loadFile(usuario.getFirmaImagen())) {
            return readAllBytes(is);
        }
    }

    /** Compatible con Java 8 (InputStream.readAllBytes() es Java 9+). */
    private static byte[] readAllBytes(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }
}
