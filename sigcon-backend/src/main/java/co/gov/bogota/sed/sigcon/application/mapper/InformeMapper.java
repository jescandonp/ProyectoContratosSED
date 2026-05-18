package co.gov.bogota.sed.sigcon.application.mapper;

import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ObservacionDto;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InformeMapper {

    private final UsuarioMapper usuarioMapper;
    private final ActividadInformeMapper actividadInformeMapper;
    private final DocumentoAdicionalMapper documentoAdicionalMapper;
    private final ObservacionMapper observacionMapper;
    private final AporteSgssiMapper aporteSgssiMapper;

    public InformeMapper(
        UsuarioMapper usuarioMapper,
        ActividadInformeMapper actividadInformeMapper,
        DocumentoAdicionalMapper documentoAdicionalMapper,
        ObservacionMapper observacionMapper,
        AporteSgssiMapper aporteSgssiMapper
    ) {
        this.usuarioMapper = usuarioMapper;
        this.actividadInformeMapper = actividadInformeMapper;
        this.documentoAdicionalMapper = documentoAdicionalMapper;
        this.observacionMapper = observacionMapper;
        this.aporteSgssiMapper = aporteSgssiMapper;
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
        List<Observacion> observaciones,
        List<AporteSgssi> aportes
    ) {
        InformeDetalleDto dto = new InformeDetalleDto();
        fillResumen(dto, informe);
        if (informe.getContrato() != null) {
            dto.setContratista(usuarioMapper.toDto(informe.getContrato().getContratista()));
            dto.setRevisor(usuarioMapper.toDto(informe.getContrato().getRevisor()));
            dto.setSupervisor(usuarioMapper.toDto(informe.getContrato().getSupervisor()));
        }
        dto.setNumeroDesembolso(informe.getNumeroDesembolso());
        dto.setValorDesembolso(informe.getValorDesembolso());
        dto.setPorcentajeEjecucion(informe.getPorcentajeEjecucion());
        dto.setCorrespondenciaPendiente(informe.getCorrespondenciaPendiente() != null
            && informe.getCorrespondenciaPendiente() > 0);
        List<ActividadInformeDto> actividadDtos = actividades.stream()
            .map(a -> actividadInformeMapper.toDto(a, soportesPorActividad.getOrDefault(a.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
        dto.setActividades(actividadDtos);
        dto.setDocumentosAdicionales(documentos.stream()
            .map(documentoAdicionalMapper::toDto)
            .collect(Collectors.toList()));
        dto.setObservaciones(observaciones.stream()
            .map(observacionMapper::toDto)
            .collect(Collectors.toList()));
        dto.setAportesSgssi(aportes.stream()
            .map(aporteSgssiMapper::toDto)
            .collect(Collectors.toList()));
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
        dto.setFechaElaboracion(informe.getFechaElaboracion());
    }
}
