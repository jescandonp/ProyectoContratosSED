package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalRequest;
import co.gov.bogota.sed.sigcon.application.mapper.DocumentoAdicionalMapper;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentoAdicionalInformeService {

    private final DocumentoAdicionalRepository documentoAdicionalRepository;
    private final DocumentoCatalogoRepository documentoCatalogoRepository;
    private final InformeService informeService;
    private final CurrentUserService currentUserService;
    private final DocumentoAdicionalMapper documentoAdicionalMapper;

    public DocumentoAdicionalInformeService(
        DocumentoAdicionalRepository documentoAdicionalRepository,
        DocumentoCatalogoRepository documentoCatalogoRepository,
        InformeService informeService,
        CurrentUserService currentUserService,
        DocumentoAdicionalMapper documentoAdicionalMapper
    ) {
        this.documentoAdicionalRepository = documentoAdicionalRepository;
        this.documentoCatalogoRepository = documentoCatalogoRepository;
        this.informeService = informeService;
        this.currentUserService = currentUserService;
        this.documentoAdicionalMapper = documentoAdicionalMapper;
    }

    public DocumentoAdicionalDto agregar(Long informeId, DocumentoAdicionalRequest request) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        DocumentoCatalogo catalogo = documentoCatalogoRepository.findById(request.getIdCatalogo())
            .filter(c -> Boolean.TRUE.equals(c.getActivo()))
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.DOCUMENTO_ADICIONAL_REQUERIDO,
                "Documento de catálogo no encontrado",
                HttpStatus.BAD_REQUEST
            ));
        DocumentoAdicional documento = new DocumentoAdicional();
        documento.setInforme(informe);
        documento.setCatalogo(catalogo);
        documento.setReferencia(request.getReferencia());
        documento.setActivo(true);
        return documentoAdicionalMapper.toDto(documentoAdicionalRepository.save(documento));
    }

    public void eliminar(Long informeId, Long documentoId) {
        Informe informe = informeService.findActiveInforme(informeId);
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        DocumentoAdicional documento = documentoAdicionalRepository.findByIdAndActivoTrue(documentoId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.DOCUMENTO_ADICIONAL_REQUERIDO,
                "Documento adicional no encontrado",
                HttpStatus.NOT_FOUND
            ));
        if (documento.getInforme() == null || !documento.getInforme().getId().equals(informeId)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "El documento no pertenece al informe",
                HttpStatus.FORBIDDEN
            );
        }
        documento.setActivo(false);
        documentoAdicionalRepository.save(documento);
    }
}
