package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeRequest;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.mapper.InformeMapper;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class InformeService {

    private final InformeRepository informeRepository;
    private final ContratoRepository contratoRepository;
    private final ActividadInformeRepository actividadRepository;
    private final SoporteAdjuntoRepository soporteRepository;
    private final DocumentoAdicionalRepository documentoAdicionalRepository;
    private final ObservacionRepository observacionRepository;
    private final CurrentUserService currentUserService;
    private final InformeMapper informeMapper;

    public InformeService(
        InformeRepository informeRepository,
        ContratoRepository contratoRepository,
        ActividadInformeRepository actividadRepository,
        SoporteAdjuntoRepository soporteRepository,
        DocumentoAdicionalRepository documentoAdicionalRepository,
        ObservacionRepository observacionRepository,
        CurrentUserService currentUserService,
        InformeMapper informeMapper
    ) {
        this.informeRepository = informeRepository;
        this.contratoRepository = contratoRepository;
        this.actividadRepository = actividadRepository;
        this.soporteRepository = soporteRepository;
        this.documentoAdicionalRepository = documentoAdicionalRepository;
        this.observacionRepository = observacionRepository;
        this.currentUserService = currentUserService;
        this.informeMapper = informeMapper;
    }

    @Transactional(readOnly = true)
    public Page<InformeResumenDto> listarMisInformes(Pageable pageable) {
        Usuario usuario = currentUserService.getCurrentUser();
        if (usuario.getRol() != RolUsuario.CONTRATISTA) {
            throw accessDenied();
        }
        return informeRepository.findByContratoContratistaAndActivoTrue(usuario, pageable)
            .map(informeMapper::toResumenDto);
    }

    @Transactional(readOnly = true)
    public Page<InformeResumenDto> listarParaRevisor(Pageable pageable) {
        Usuario usuario = currentUserService.getCurrentUser();
        if (usuario.getRol() != RolUsuario.REVISOR) {
            throw accessDenied();
        }
        return informeRepository
            .findByContratoRevisorAndEstadoAndActivoTrue(usuario, EstadoInforme.ENVIADO, pageable)
            .map(informeMapper::toResumenDto);
    }

    @Transactional(readOnly = true)
    public Page<InformeResumenDto> listarParaSupervisor(Pageable pageable) {
        Usuario usuario = currentUserService.getCurrentUser();
        if (usuario.getRol() != RolUsuario.SUPERVISOR) {
            throw accessDenied();
        }
        return informeRepository
            .findByContratoSupervisorAndEstadoAndActivoTrue(usuario, EstadoInforme.EN_REVISION, pageable)
            .map(informeMapper::toResumenDto);
    }

    @Transactional(readOnly = true)
    public Page<InformeResumenDto> listarPorContrato(Long contratoId, Pageable pageable) {
        Contrato contrato = findActiveContrato(contratoId);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanViewContrato(usuario, contrato);
        return informeRepository.findByContratoIdAndActivoTrue(contratoId, pageable)
            .map(informeMapper::toResumenDto);
    }

    @Transactional(readOnly = true)
    public InformeDetalleDto obtenerDetalle(Long id) {
        Informe informe = findActiveInforme(id);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanViewInforme(usuario, informe);
        return buildDetalle(informe);
    }

    @Transactional(readOnly = true)
    public Informe obtenerInformeAutorizado(Long id) {
        Informe informe = findActiveInforme(id);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanViewInforme(usuario, informe);
        return informe;
    }

    public InformeDetalleDto crearInforme(InformeRequest request) {
        Usuario usuario = currentUserService.getCurrentUser();
        if (usuario.getRol() != RolUsuario.CONTRATISTA) {
            throw accessDenied();
        }
        Contrato contrato = findActiveContrato(request.getIdContrato());
        if (!isAssigned(contrato.getContratista(), usuario.getId())) {
            throw accessDenied();
        }
        if (contrato.getEstado() != EstadoContrato.EN_EJECUCION) {
            throw new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ACTIVO,
                "El contrato no está en ejecución",
                HttpStatus.CONFLICT
            );
        }
        Integer existing = informeRepository.countByContratoId(contrato.getId());
        Informe informe = new Informe();
        informe.setContrato(contrato);
        informe.setNumero(existing == null ? 1 : existing + 1);
        informe.setFechaInicio(request.getFechaInicio());
        informe.setFechaFin(request.getFechaFin());
        informe.setEstado(EstadoInforme.BORRADOR);
        informe.setActivo(true);
        Informe saved = informeRepository.save(informe);
        return buildDetalle(saved);
    }

    public InformeDetalleDto actualizarInforme(Long id, InformeRequest request) {
        Informe informe = findActiveInforme(id);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanEditInforme(usuario, informe);
        informe.setFechaInicio(request.getFechaInicio());
        informe.setFechaFin(request.getFechaFin());
        informeRepository.save(informe);
        return buildDetalle(informe);
    }

    public void eliminarInforme(Long id) {
        Informe informe = findActiveInforme(id);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanEditInforme(usuario, informe);
        informe.setActivo(false);
        informeRepository.save(informe);
    }

    // ------- Helpers usados por servicios I2 hermanos y por InformeEstadoService (Task 5) -------

    public Informe findActiveInforme(Long id) {
        return informeRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.INFORME_NO_ENCONTRADO,
                "Informe no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }

    public InformeDetalleDto buildDetalle(Informe informe) {
        List<ActividadInforme> actividades = actividadRepository.findByInformeIdAndActivoTrue(informe.getId());
        Map<Long, List<SoporteAdjunto>> soportesPorActividad = new HashMap<>();
        for (ActividadInforme actividad : actividades) {
            soportesPorActividad.put(
                actividad.getId(),
                soporteRepository.findByActividadIdAndActivoTrue(actividad.getId())
            );
        }
        List<DocumentoAdicional> documentos = documentoAdicionalRepository.findByInformeIdAndActivoTrue(informe.getId());
        List<Observacion> observaciones = observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(informe.getId());
        return informeMapper.toDetalleDto(informe, actividades, soportesPorActividad, documentos, observaciones);
    }

    public void assertCanEditInforme(Usuario usuario, Informe informe) {
        if (usuario.getRol() != RolUsuario.CONTRATISTA
            || !isAssigned(informe.getContrato().getContratista(), usuario.getId())) {
            throw accessDenied();
        }
        EstadoInforme estado = informe.getEstado();
        if (estado != EstadoInforme.BORRADOR && estado != EstadoInforme.DEVUELTO) {
            throw new SigconBusinessException(
                ErrorCode.INFORME_NO_EDITABLE,
                "El informe no es editable en su estado actual",
                HttpStatus.CONFLICT
            );
        }
    }

    public void assertCanViewInforme(Usuario usuario, Informe informe) {
        Contrato contrato = informe.getContrato();
        assertCanViewContrato(usuario, contrato);
    }

    private void assertCanViewContrato(Usuario usuario, Contrato contrato) {
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        Long usuarioId = usuario.getId();
        if (usuario.getRol() == RolUsuario.CONTRATISTA && isAssigned(contrato.getContratista(), usuarioId)) {
            return;
        }
        if (usuario.getRol() == RolUsuario.REVISOR && isAssigned(contrato.getRevisor(), usuarioId)) {
            return;
        }
        if (usuario.getRol() == RolUsuario.SUPERVISOR && isAssigned(contrato.getSupervisor(), usuarioId)) {
            return;
        }
        throw accessDenied();
    }

    private Contrato findActiveContrato(Long id) {
        return contratoRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ENCONTRADO,
                "Contrato no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }

    private static boolean isAssigned(Usuario usuario, Long id) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(id);
    }

    private static SigconBusinessException accessDenied() {
        return new SigconBusinessException(ErrorCode.ACCESO_DENEGADO, "Acceso denegado", HttpStatus.FORBIDDEN);
    }
}
