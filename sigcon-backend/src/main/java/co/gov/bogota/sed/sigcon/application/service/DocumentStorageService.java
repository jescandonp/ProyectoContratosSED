package co.gov.bogota.sed.sigcon.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentStorageService {

    String storeSignature(Long usuarioId, MultipartFile file) throws IOException;

    /**
     * Guarda un archivo binario bajo el subdirectorio indicado y retorna la ruta relativa.
     * Reusado en I2 por SoporteAdjuntoService para soportes tipo ARCHIVO.
     */
    String storeFile(String subdir, MultipartFile file) throws IOException;
}
