package co.gov.bogota.sed.sigcon.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentStorageService {

    String storeSignature(Long usuarioId, MultipartFile file) throws IOException;
}
