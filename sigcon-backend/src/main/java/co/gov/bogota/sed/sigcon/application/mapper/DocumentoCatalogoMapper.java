package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoDto;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import org.springframework.stereotype.Component;

@Component
public class DocumentoCatalogoMapper {

    public DocumentoCatalogoDto toDto(DocumentoCatalogo documento) {
        if (documento == null) {
            return null;
        }
        DocumentoCatalogoDto dto = new DocumentoCatalogoDto();
        dto.setId(documento.getId());
        dto.setNombre(documento.getNombre());
        dto.setDescripcion(documento.getDescripcion());
        dto.setObligatorio(documento.getObligatorio());
        dto.setTipoContrato(documento.getTipoContrato());
        return dto;
    }
}
