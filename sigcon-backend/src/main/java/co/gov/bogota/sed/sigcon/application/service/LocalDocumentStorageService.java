package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile("local-dev")
public class LocalDocumentStorageService implements DocumentStorageService {

    private static final long MAX_SIGNATURE_BYTES = 2L * 1024L * 1024L;

    private final Path rootPath;

    public LocalDocumentStorageService(@Value("${sigcon.storage.signatures-path:${java.io.tmpdir}/sigcon}") String rootPath) {
        this.rootPath = Paths.get(rootPath);
    }

    @Override
    public String storeSignature(Long usuarioId, MultipartFile file) throws IOException {
        validateSignature(file);
        String extension = extensionFor(file.getContentType());
        String relativeReference = "firmas/" + usuarioId + "/" + UUID.randomUUID() + extension;
        Path target = rootPath.resolve(relativeReference).normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return relativeReference;
    }

    @Override
    public String storeFile(String subdir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Archivo vacío o ausente",
                HttpStatus.BAD_REQUEST
            );
        }
        if (subdir == null || subdir.trim().isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Subdirectorio requerido para almacenar el soporte",
                HttpStatus.BAD_REQUEST
            );
        }
        String safeName = sanitize(file.getOriginalFilename());
        String relativeReference = subdir + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = rootPath.resolve(relativeReference).normalize();
        if (!target.startsWith(rootPath)) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Ruta de soporte inválida",
                HttpStatus.BAD_REQUEST
            );
        }
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return relativeReference;
    }

    @Override
    public String storeBytes(String subdir, String filename, byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new SigconBusinessException(
                ErrorCode.PDF_GENERACION_FALLIDA,
                "Contenido del PDF vacío",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        String safeFilename = sanitize(filename);
        String relativeReference = subdir + "/" + safeFilename;
        Path target = rootPath.resolve(relativeReference).normalize();
        if (!target.startsWith(rootPath)) {
            throw new SigconBusinessException(
                ErrorCode.PDF_GENERACION_FALLIDA,
                "Ruta de PDF inválida",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return relativeReference;
    }

    @Override
    public InputStream loadFile(String relativePath) throws IOException {
        Path target = rootPath.resolve(relativePath).normalize();
        if (!target.startsWith(rootPath) || !Files.exists(target)) {
            throw new IOException("Archivo no encontrado: " + relativePath);
        }
        return new FileInputStream(target.toFile());
    }

    private String sanitize(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "soporte.bin";
        }
        String trimmed = name.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        return trimmed.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void validateSignature(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIGNATURE_BYTES) {
            throw invalidFormat();
        }
        String contentType = file.getContentType();
        if (!"image/png".equals(contentType) && !"image/jpeg".equals(contentType)) {
            throw invalidFormat();
        }
    }

    private String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }

    private SigconBusinessException invalidFormat() {
        return new SigconBusinessException(
            ErrorCode.FORMATO_IMAGEN_INVALIDO,
            "La firma debe ser una imagen JPG o PNG de máximo 2MB",
            HttpStatus.BAD_REQUEST
        );
    }
}
