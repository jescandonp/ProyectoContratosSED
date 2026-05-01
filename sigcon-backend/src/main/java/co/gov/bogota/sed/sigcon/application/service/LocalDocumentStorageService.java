package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
