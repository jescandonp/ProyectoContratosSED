package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.catalogo.DocumentoCatalogoDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.obligacion.ObligacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoCatalogo;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContratoMapper {

    private final UsuarioMapper usuarioMapper;
    private final ObligacionMapper obligacionMapper;
    private final DocumentoCatalogoMapper documentoCatalogoMapper;

    public ContratoMapper(
        UsuarioMapper usuarioMapper,
        ObligacionMapper obligacionMapper,
        DocumentoCatalogoMapper documentoCatalogoMapper
    ) {
        this.usuarioMapper = usuarioMapper;
        this.obligacionMapper = obligacionMapper;
        this.documentoCatalogoMapper = documentoCatalogoMapper;
    }

    public ContratoResumenDto toResumenDto(Contrato contrato) {
        ContratoResumenDto dto = new ContratoResumenDto();
        fillResumen(dto, contrato);
        return dto;
    }

    public ContratoDetalleDto toDetalleDto(
        Contrato contrato,
        List<Obligacion> obligaciones,
        List<DocumentoCatalogo> docsAplicables
    ) {
        ContratoDetalleDto dto = new ContratoDetalleDto();
        fillResumen(dto, contrato);
        dto.setContratista(usuarioMapper.toDto(contrato.getContratista()));
        dto.setRevisor(usuarioMapper.toDto(contrato.getRevisor()));
        dto.setSupervisor(usuarioMapper.toDto(contrato.getSupervisor()));
        dto.setObligaciones(obligaciones.stream().map(obligacionMapper::toDto).collect(Collectors.toList()));
        dto.setDocsAplicables(docsAplicables.stream().map(documentoCatalogoMapper::toDto).collect(Collectors.toList()));
        return dto;
    }

    private void fillResumen(ContratoResumenDto dto, Contrato contrato) {
        dto.setId(contrato.getId());
        dto.setNumero(contrato.getNumero());
        dto.setObjeto(contrato.getObjeto());
        dto.setTipo(contrato.getTipo());
        dto.setEstado(contrato.getEstado());
        dto.setFechaInicio(contrato.getFechaInicio());
        dto.setFechaFin(contrato.getFechaFin());
        dto.setValorTotal(contrato.getValorTotal());
        if (contrato.getContratista() != null) {
            dto.setContratistaNombre(contrato.getContratista().getNombre());
        }
        if (contrato.getSupervisor() != null) {
            dto.setSupervisorNombre(contrato.getSupervisor().getNombre());
        }
    }
}
