package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionDto;
import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionesCountDto;
import co.gov.bogota.sed.sigcon.application.mapper.NotificacionMapper;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Notificacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.NotificacionRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona el ciclo de vida de las notificaciones in-app (I3).
 * Creacion delegada por EventoInformeService; consulta y marcado via API REST.
 */
@Service
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;
    private final CurrentUserService currentUserService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               NotificacionMapper notificacionMapper,
                               CurrentUserService currentUserService) {
        this.notificacionRepository = notificacionRepository;
        this.notificacionMapper = notificacionMapper;
        this.currentUserService = currentUserService;
    }

    /**
     * Crea una notificacion in-app para el destinatario indicado.
     * Llamado exclusivamente por EventoInformeService.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notificacion crear(Usuario destinatario, TipoEvento evento, Informe informe, String descripcion) {
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTipoEvento(evento);
        n.setInforme(informe);
        n.setTitulo(tituloParaEvento(evento));
        n.setDescripcion(descripcion != null ? descripcion : "");
        n.setLeida(false);
        return notificacionRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDto> listar(Pageable pageable) {
        Usuario usuario = currentUserService.getCurrentUser();
        return notificacionRepository.findByUsuarioOrderByFechaDesc(usuario, pageable)
            .map(notificacionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public NotificacionesCountDto contarNoLeidas() {
        Usuario usuario = currentUserService.getCurrentUser();
        return new NotificacionesCountDto(notificacionRepository.countByUsuarioAndLeidaFalse(usuario));
    }

    public NotificacionDto marcarLeida(Long id) {
        Usuario usuario = currentUserService.getCurrentUser();
        Notificacion n = notificacionRepository.findByIdAndUsuario(id, usuario)
            .orElseThrow(() -> new SigconBusinessException(
                ErrorCode.NOTIFICACION_NO_ENCONTRADA,
                "Notificacion no encontrada o no pertenece al usuario",
                HttpStatus.NOT_FOUND
            ));
        n.setLeida(true);
        return notificacionMapper.toDto(notificacionRepository.save(n));
    }

    public void marcarTodasLeidas() {
        Usuario usuario = currentUserService.getCurrentUser();
        notificacionRepository.findByUsuarioOrderByFechaDesc(usuario, Pageable.unpaged())
            .forEach(n -> {
                if (!n.isLeida()) {
                    n.setLeida(true);
                    notificacionRepository.save(n);
                }
            });
    }

    private static String tituloParaEvento(TipoEvento evento) {
        switch (evento) {
            case INFORME_ENVIADO:    return "Informe enviado para revisión";
            case REVISION_APROBADA:  return "Informe listo para aprobación";
            case REVISION_DEVUELTA:  return "Informe devuelto por revisión";
            case INFORME_APROBADO:   return "Informe aprobado";
            case INFORME_DEVUELTO:   return "Informe devuelto por supervisor";
            default:                 return evento.name();
        }
    }
}
