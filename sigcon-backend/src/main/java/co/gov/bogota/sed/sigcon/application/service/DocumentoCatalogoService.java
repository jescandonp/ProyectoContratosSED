package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoDto;
import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoRequest;
import co.gov.bogota.sed.sigcon.application.mapper.DocumentoCatalogoMapper;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentoCatalogoService {

    private final DocumentoCatalogoRepository documentoCatalogoRepository;
    private final DocumentoCatalogoMapper documentoCatalogoMapper;

    public DocumentoCatalogoService(
        DocumentoCatalogoRepository documentoCatalogoRepository,
        DocumentoCatalogoMapper documentoCatalogoMapper
    ) {
        this.documentoCatalogoRepository = documentoCatalogoRepository;
        this.documentoCatalogoMapper = documentoCatalogoMapper;
    }

    @Transactional(readOnly = true)
    public Page<DocumentoCatalogoDto> listar(TipoContrato tipoContrato, Pageable pageable) {
        if (tipoContrato == null) {
            return documentoCatalogoRepository.findByActivoTrue(pageable).map(documentoCatalogoMapper::toDto);
        }
        List<DocumentoCatalogoDto> items = documentoCatalogoRepository.findByTipoContratoAndActivoTrue(tipoContrato)
            .stream()
            .map(documentoCatalogoMapper::toDto)
            .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, items.size());
    }

    public DocumentoCatalogoDto crear(DocumentoCatalogoRequest request) {
        DocumentoCatalogo documento = new DocumentoCatalogo();
        applyRequest(documento, request);
        return documentoCatalogoMapper.toDto(documentoCatalogoRepository.save(documento));
    }

    public DocumentoCatalogoDto actualizar(Long id, DocumentoCatalogoRequest request) {
        DocumentoCatalogo documento = findActiveDocumento(id);
        applyRequest(documento, request);
        return documentoCatalogoMapper.toDto(documentoCatalogoRepository.save(documento));
    }

    public void eliminar(Long id) {
        DocumentoCatalogo documento = findActiveDocumento(id);
        documento.setActivo(false);
        documentoCatalogoRepository.save(documento);
    }

    private void applyRequest(DocumentoCatalogo documento, DocumentoCatalogoRequest request) {
        if (request.getTipoContrato() != TipoContrato.OPS) {
            throw new SigconBusinessException(ErrorCode.ESTADO_INVALIDO, "Solo se permite tipo de contrato OPS en I1", HttpStatus.BAD_REQUEST);
        }
        documento.setNombre(request.getNombre());
        documento.setDescripcion(request.getDescripcion());
        documento.setObligatorio(request.getObligatorio());
        documento.setTipoContrato(request.getTipoContrato());
        documento.setActivo(true);
    }

    private DocumentoCatalogo findActiveDocumento(Long id) {
        return documentoCatalogoRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ENCONTRADO,
                "Documento de catálogo no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }
}
