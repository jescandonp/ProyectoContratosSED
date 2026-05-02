package co.gov.bogota.sed.sigcon.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorageService {

    String storeSignature(Long usuarioId, MultipartFile file) throws IOException;

    /**
     * Guarda un archivo binario bajo el subdirectorio indicado y retorna la ruta relativa.
     * Reusado en I2 por SoporteAdjuntoService para soportes tipo ARCHIVO.
     */
    String storeFile(String subdir, MultipartFile file) throws IOException;

    /**
     * Guarda bytes crudos (ej. PDF generado) bajo la ruta indicada y retorna la ruta relativa.
     * Usado por PdfInformeService en I3 para almacenar el PDF institucional.
     *
     * @param subdir subdirectorio relativo bajo la raiz de almacenamiento
     * @param filename nombre del archivo final
     * @param bytes   contenido del archivo
     * @return ruta relativa almacenada (subdir/filename)
     */
    String storeBytes(String subdir, String filename, byte[] bytes) throws IOException;

    /**
     * Carga el contenido de un archivo previamente almacenado como stream.
     * Usado por InformePdfController para servir descargas.
     *
     * @param relativePath ruta relativa tal como fue retornada por storeFile/storeBytes
     * @return InputStream del archivo
     */
    InputStream loadFile(String relativePath) throws IOException;
}
