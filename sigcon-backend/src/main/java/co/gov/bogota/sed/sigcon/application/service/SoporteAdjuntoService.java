package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteAdjuntoDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteUrlRequest;
import co.gov.bogota.sed.sigcon.application.mapper.SoporteAdjuntoMapper;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
public class SoporteAdjuntoService {

    private final SoporteAdjuntoRepository soporteRepository;
    private final ActividadInformeRepository actividadRepository;
    private final InformeService informeService;
    private final CurrentUserService currentUserService;
    private final DocumentStorageService documentStorageService;
    private final SoporteAdjuntoMapper soporteMapper;

    public SoporteAdjuntoService(
        SoporteAdjuntoRepository soporteRepository,
        ActividadInformeRepository actividadRepository,
        InformeService informeService,
        CurrentUserService currentUserService,
        DocumentStorageService documentStorageService,
        SoporteAdjuntoMapper soporteMapper
    ) {
        this.soporteRepository = soporteRepository;
        this.actividadRepository = actividadRepository;
        this.informeService = informeService;
        this.currentUserService = currentUserService;
        this.documentStorageService = documentStorageService;
        this.soporteMapper = soporteMapper;
    }

    public SoporteAdjuntoDto agregarSoporteUrl(Long actividadId, SoporteUrlRequest request) {
        ActividadInforme actividad = loadEditableActividad(actividadId);
        validateUrl(request.getUrl());
        SoporteAdjunto soporte = new SoporteAdjunto();
        soporte.setActividad(actividad);
        soporte.setTipo(TipoSoporte.URL);
        soporte.setNombre(request.getNombre());
        soporte.setReferencia(request.getUrl());
        soporte.setActivo(true);
        return soporteMapper.toDto(soporteRepository.save(soporte));
    }

    public SoporteAdjuntoDto agregarSoporteArchivo(Long actividadId, MultipartFile file) {
        ActividadInforme actividad = loadEditableActividad(actividadId);
        if (file == null || file.isEmpty()) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Archivo vacío o ausente",
                HttpStatus.BAD_REQUEST
            );
        }
        Informe informe = actividad.getInforme();
        String subdir = "soportes/" + informe.getContrato().getId() + "/" + informe.getId() + "/" + actividad.getId();
        String referencia;
        try {
            referencia = documentStorageService.storeFile(subdir, file);
        } catch (IOException ex) {
            throw new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "No fue posible almacenar el soporte",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        SoporteAdjunto soporte = new SoporteAdjunto();
        soporte.setActividad(actividad);
        soporte.setTipo(TipoSoporte.ARCHIVO);
        soporte.setNombre(file.getOriginalFilename() != null ? file.getOriginalFilename() : "soporte");
        soporte.setReferencia(referencia);
        soporte.setActivo(true);
        return soporteMapper.toDto(soporteRepository.save(soporte));
    }

    public void eliminar(Long actividadId, Long soporteId) {
        loadEditableActividad(actividadId);
        SoporteAdjunto soporte = soporteRepository.findByIdAndActivoTrue(soporteId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Soporte no encontrado",
                HttpStatus.NOT_FOUND
            ));
        if (soporte.getActividad() == null || !soporte.getActividad().getId().equals(actividadId)) {
            throw new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "El soporte no pertenece a la actividad",
                HttpStatus.FORBIDDEN
            );
        }
        soporte.setActivo(false);
        soporteRepository.save(soporte);
    }

    private ActividadInforme loadEditableActividad(Long actividadId) {
        ActividadInforme actividad = actividadRepository.findByIdAndActivoTrue(actividadId)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.SOPORTE_INVALIDO,
                "Actividad no encontrada",
                HttpStatus.NOT_FOUND
            ));
        Informe informe = actividad.getInforme();
        Usuario usuario = currentUserService.getCurrentUser();
        informeService.assertCanEditInforme(usuario, informe);
        return actividad;
    }

    private static void validateUrl(String url) {
        if (url == null) {
            throw invalidUrl();
        }
        String trimmed = url.trim();
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            throw invalidUrl();
        }
    }

    private static SigconBusinessException invalidUrl() {
        return new SigconBusinessException(
            ErrorCode.SOPORTE_INVALIDO,
            "La URL del soporte debe iniciar con http:// o https://",
            HttpStatus.BAD_REQUEST
        );
    }
}
