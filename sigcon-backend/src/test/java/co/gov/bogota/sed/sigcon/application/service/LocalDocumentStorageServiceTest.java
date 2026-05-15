package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalDocumentStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalDocumentStorageService(tempDir.toString(), 10L * 1024L * 1024L);
    }

    @Test
    void storeFile_extensionNoPermitida_lanzaSoporteFormatoInvalido() {
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.storeFile("soportes/1/2/3", archivo))
            .isInstanceOf(SigconBusinessException.class)
            .satisfies(ex ->
                assertThat(((SigconBusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOPORTE_FORMATO_INVALIDO));
    }

    @Test
    void storeFile_tamanioExcedido_lanzaSoporteTamanioExcedido() {
        LocalDocumentStorageService serviceLimitado =
            new LocalDocumentStorageService(tempDir.toString(), 5L);
        byte[] contenido = new byte[10];
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "documento.pdf", "application/pdf", contenido
        );

        assertThatThrownBy(() -> serviceLimitado.storeFile("soportes/1/2/3", archivo))
            .isInstanceOf(SigconBusinessException.class)
            .satisfies(ex ->
                assertThat(((SigconBusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SOPORTE_TAMANIO_EXCEDIDO));
    }

    @Test
    void storeFile_pdfValido_retornaReferenciaYAlmacena() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
            "file", "informe.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        String referencia = service.storeFile("soportes/1/2/3", archivo);

        assertThat(referencia).startsWith("soportes/1/2/3/");
        assertThat(referencia).endsWith("_informe.pdf");
    }
}
