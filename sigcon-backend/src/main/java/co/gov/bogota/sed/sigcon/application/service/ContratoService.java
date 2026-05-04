package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoRequest;
import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.contrato.EstadoContratoRequest;
import co.gov.bogota.sed.sigcon.application.mapper.ContratoMapper;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObligacionRepository obligacionRepository;
    private final DocumentoCatalogoRepository documentoCatalogoRepository;
    private final CurrentUserService currentUserService;
    private final ContratoMapper contratoMapper;

    public ContratoService(
        ContratoRepository contratoRepository,
        UsuarioRepository usuarioRepository,
        ObligacionRepository obligacionRepository,
        DocumentoCatalogoRepository documentoCatalogoRepository,
        CurrentUserService currentUserService,
        ContratoMapper contratoMapper
    ) {
        this.contratoRepository = contratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.obligacionRepository = obligacionRepository;
        this.documentoCatalogoRepository = documentoCatalogoRepository;
        this.currentUserService = currentUserService;
        this.contratoMapper = contratoMapper;
    }

    @Transactional(readOnly = true)
    public Page<ContratoResumenDto> listarContratos(Pageable pageable) {
        Usuario usuario = currentUserService.getCurrentUser();
        Page<Contrato> contratos;
        if (usuario.getRol() == RolUsuario.ADMIN) {
            contratos = contratoRepository.findByActivoTrue(pageable);
        } else if (usuario.getRol() == RolUsuario.CONTRATISTA) {
            contratos = contratoRepository.findByContratistaAndActivoTrue(usuario, pageable);
        } else if (usuario.getRol() == RolUsuario.SUPERVISOR) {
            contratos = contratoRepository.findBySupervisorAndActivoTrue(usuario, pageable);
        } else if (usuario.getRol() == RolUsuario.REVISOR) {
            contratos = contratoRepository.findByRevisorAndActivoTrue(usuario, pageable);
        } else {
            throw accessDenied();
        }
        return contratos.map(contratoMapper::toResumenDto);
    }

    @Transactional(readOnly = true)
    public ContratoDetalleDto obtenerDetalle(Long id) {
        Contrato contrato = findActiveContrato(id);
        Usuario usuario = currentUserService.getCurrentUser();
        assertCanView(usuario, contrato);
        return contratoMapper.toDetalleDto(
            contrato,
            obligacionRepository.findByContratoIdAndActivoTrueOrderByOrdenAsc(contrato.getId()),
            documentoCatalogoRepository.findByTipoContratoAndActivoTrue(TipoContrato.OPS)
        );
    }

    public ContratoDetalleDto crearContrato(ContratoRequest request) {
        contratoRepository.findByNumeroAndActivoTrue(request.getNumero()).ifPresent(existing -> {
            throw new SigconBusinessException(
                ErrorCode.NUMERO_CONTRATO_DUPLICADO,
                "El número de contrato ya existe",
                HttpStatus.CONFLICT
            );
        });
        Contrato contrato = new Contrato();
        applyRequest(contrato, request);
        Contrato saved = contratoRepository.save(contrato);
        return contratoMapper.toDetalleDto(saved, java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    public ContratoDetalleDto actualizarContrato(Long id, ContratoRequest request) {
        Contrato contrato = findActiveContrato(id);
        contratoRepository.findByNumeroAndActivoTrue(request.getNumero())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new SigconBusinessException(
                    ErrorCode.NUMERO_CONTRATO_DUPLICADO,
                    "El número de contrato ya existe",
                    HttpStatus.CONFLICT
                );
            });
        applyRequest(contrato, request);
        Contrato saved = contratoRepository.save(contrato);
        return contratoMapper.toDetalleDto(saved, java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    public void cambiarEstado(Long id, EstadoContratoRequest request) {
        if (request.getEstado() == null) {
            throw new SigconBusinessException(ErrorCode.ESTADO_INVALIDO, "Estado de contrato inválido", HttpStatus.BAD_REQUEST);
        }
        Contrato contrato = findActiveContrato(id);
        contrato.setEstado(request.getEstado());
        contratoRepository.save(contrato);
    }

    public void eliminarContrato(Long id) {
        Contrato contrato = findActiveContrato(id);
        contrato.setActivo(false);
        contratoRepository.save(contrato);
    }

    private Contrato findActiveContrato(Long id) {
        return contratoRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.CONTRATO_NO_ENCONTRADO,
                "Contrato no encontrado",
                HttpStatus.NOT_FOUND
            ));
    }

    private void applyRequest(Contrato contrato, ContratoRequest request) {
        contrato.setNumero(request.getNumero());
        contrato.setObjeto(request.getObjeto());
        contrato.setTipo(request.getTipo());
        contrato.setValorTotal(request.getValorTotal());
        contrato.setFechaInicio(request.getFechaInicio());
        contrato.setFechaFin(request.getFechaFin());
        contrato.setEstado(contrato.getEstado() == null ? EstadoContrato.EN_EJECUCION : contrato.getEstado());
        contrato.setContratista(findActiveUsuario(request.getIdContratista(), RolUsuario.CONTRATISTA, "contratista"));
        contrato.setRevisor(findActiveUsuario(request.getIdRevisor(), RolUsuario.REVISOR, "revisor"));
        contrato.setSupervisor(findActiveUsuario(request.getIdSupervisor(), RolUsuario.SUPERVISOR, "supervisor"));
        contrato.setActivo(true);
    }

    private Usuario findActiveUsuario(Long id, RolUsuario expectedRole, String label) {
        if (id == null) {
            throw new SigconBusinessException(
                ErrorCode.USUARIO_NO_ENCONTRADO,
                "Debe seleccionar " + label + " del contrato",
                HttpStatus.BAD_REQUEST
            );
        }
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.USUARIO_NO_ENCONTRADO,
                "Usuario no encontrado",
                HttpStatus.NOT_FOUND
            ));
        if (usuario.getRol() != expectedRole) {
            throw new SigconBusinessException(
                ErrorCode.USUARIO_NO_ENCONTRADO,
                "El usuario seleccionado no corresponde al rol " + expectedRole.name(),
                HttpStatus.BAD_REQUEST
            );
        }
        return usuario;
    }

    private void assertCanView(Usuario usuario, Contrato contrato) {
        if (usuario.getRol() == RolUsuario.ADMIN) {
            return;
        }
        Long usuarioId = usuario.getId();
        if (usuario.getRol() == RolUsuario.CONTRATISTA && hasSameId(contrato.getContratista(), usuarioId)) {
            return;
        }
        if (usuario.getRol() == RolUsuario.SUPERVISOR && hasSameId(contrato.getSupervisor(), usuarioId)) {
            return;
        }
        if (usuario.getRol() == RolUsuario.REVISOR && hasSameId(contrato.getRevisor(), usuarioId)) {
            return;
        }
        throw accessDenied();
    }

    private boolean hasSameId(Usuario usuario, Long id) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(id);
    }

    private SigconBusinessException accessDenied() {
        return new SigconBusinessException(ErrorCode.ACCESO_DENEGADO, "Acceso denegado", HttpStatus.FORBIDDEN);
    }
}
