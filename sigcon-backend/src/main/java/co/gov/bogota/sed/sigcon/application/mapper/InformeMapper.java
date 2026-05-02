package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ObservacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InformeMapper {

    private final UsuarioMapper usuarioMapper;
    private final ActividadInformeMapper actividadInformeMapper;
    private final DocumentoAdicionalMapper documentoAdicionalMapper;
    private final ObservacionMapper observacionMapper;

    public InformeMapper(
        UsuarioMapper usuarioMapper,
        ActividadInformeMapper actividadInformeMapper,
        DocumentoAdicionalMapper documentoAdicionalMapper,
        ObservacionMapper observacionMapper
    ) {
        this.usuarioMapper = usuarioMapper;
        this.actividadInformeMapper = actividadInformeMapper;
        this.documentoAdicionalMapper = documentoAdicionalMapper;
        this.observacionMapper = observacionMapper;
    }

    public InformeResumenDto toResumenDto(Informe informe) {
        InformeResumenDto dto = new InformeResumenDto();
        fillResumen(dto, informe);
        return dto;
    }

    public InformeDetalleDto toDetalleDto(
        Informe informe,
        List<ActividadInforme> actividades,
        Map<Long, List<SoporteAdjunto>> soportesPorActividad,
        List<DocumentoAdicional> documentos,
        List<Observacion> observaciones
    ) {
        InformeDetalleDto dto = new InformeDetalleDto();
        fillResumen(dto, informe);
        if (informe.getContrato() != null) {
            dto.setContratista(usuarioMapper.toDto(informe.getContrato().getContratista()));
            dto.setRevisor(usuarioMapper.toDto(informe.getContrato().getRevisor()));
            dto.setSupervisor(usuarioMapper.toDto(informe.getContrato().getSupervisor()));
        }
        List<ActividadInformeDto> actividadDtos = actividades.stream()
            .map(a -> actividadInformeMapper.toDto(a, soportesPorActividad.getOrDefault(a.getId(), java.util.Collections.emptyList())))
            .collect(Collectors.toList());
        dto.setActividades(actividadDtos);
        List<DocumentoAdicionalDto> docDtos = documentos.stream()
            .map(documentoAdicionalMapper::toDto)
            .collect(Collectors.toList());
        dto.setDocumentosAdicionales(docDtos);
        List<ObservacionDto> obsDtos = observaciones.stream()
            .map(observacionMapper::toDto)
            .collect(Collectors.toList());
        dto.setObservaciones(obsDtos);
        return dto;
    }

    private void fillResumen(InformeResumenDto dto, Informe informe) {
        dto.setId(informe.getId());
        dto.setNumero(informe.getNumero());
        if (informe.getContrato() != null) {
            dto.setContratoId(informe.getContrato().getId());
            dto.setContratoNumero(informe.getContrato().getNumero());
        }
        dto.setFechaInicio(informe.getFechaInicio());
        dto.setFechaFin(informe.getFechaFin());
        dto.setEstado(informe.getEstado());
        dto.setFechaUltimoEnvio(informe.getFechaUltimoEnvio());
        dto.setFechaAprobacion(informe.getFechaAprobacion());
        dto.setPdfRuta(informe.getPdfRuta());
        dto.setPdfGeneradoAt(informe.getPdfGeneradoAt());
        dto.setPdfHash(informe.getPdfHash());
    }
}
