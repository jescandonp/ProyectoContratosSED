package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import org.springframework.stereotype.Component;

@Component
public class DocumentoAdicionalMapper {

    public DocumentoAdicionalDto toDto(DocumentoAdicional entity) {
        DocumentoAdicionalDto dto = new DocumentoAdicionalDto();
        dto.setId(entity.getId());
        if (entity.getCatalogo() != null) {
            dto.setIdCatalogo(entity.getCatalogo().getId());
            dto.setNombreCatalogo(entity.getCatalogo().getNombre());
            dto.setObligatorio(entity.getCatalogo().getObligatorio());
        }
        dto.setReferencia(entity.getReferencia());
        return dto;
    }
}
