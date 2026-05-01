package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.LocalDocumentStorageService;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesPngSignatureUnderConfiguredLocalPath() throws Exception {
        LocalDocumentStorageService service = new LocalDocumentStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "firma.png", "image/png", new byte[] { 1, 2, 3 });

        String reference = service.storeSignature(7L, file);

        assertThat(reference).startsWith("firmas/7/");
        assertThat(Files.exists(tempDir.resolve(reference.replace("/", java.io.File.separator)))).isTrue();
    }

    @Test
    void rejectsNonImageSignatureFormats() {
        LocalDocumentStorageService service = new LocalDocumentStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "firma.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> service.storeSignature(7L, file))
            .isInstanceOfSatisfying(SigconBusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORMATO_IMAGEN_INVALIDO));
    }
}
