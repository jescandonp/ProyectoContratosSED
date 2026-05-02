package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Centraliza los efectos secundarios de las transiciones de estado del informe (I3).
 * Determina el destinatario de la notificacion segun el evento y delega
 * a NotificacionService (in-app) y EmailNotificacionService (correo).
 *
 * <p>Los errores de notificacion/email no propagan — quedan registrados en log.</p>
 */
@Service
public class EventoInformeService {

    private static final Logger log = LoggerFactory.getLogger(EventoInformeService.class);

    private final NotificacionService notificacionService;
    private final EmailNotificacionService emailService;

    public EventoInformeService(NotificacionService notificacionService,
                                EmailNotificacionService emailService) {
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    /**
     * Publica efectos secundarios de un evento de transicion.
     *
     * @param evento      evento que ocurrio en el flujo del informe
     * @param informe     informe con contrato, contratista, revisor y supervisor inicializados
     * @param observacion texto de observacion si aplica (puede ser null)
     */
    public void publicar(TipoEvento evento, Informe informe, String observacion) {
        try {
            Contrato contrato = informe.getContrato();
            Usuario destinatario = resolverDestinatario(evento, contrato);
            if (destinatario == null) {
                log.warn("No se puede determinar destinatario para evento={} informe={}", evento, informe.getId());
                return;
            }

            String descripcion = construirDescripcion(evento, informe, observacion);
            notificacionService.crear(destinatario, evento, informe, descripcion);
            emailService.enviar(destinatario, evento, informe.getId(), descripcion);

        } catch (Exception e) {
            // Nunca propagar — las notificaciones son efectos secundarios no criticos
            log.error("Error publicando evento={} para informe={}: {}", evento, informe.getId(), e.getMessage(), e);
        }
    }

    private static Usuario resolverDestinatario(TipoEvento evento, Contrato contrato) {
        switch (evento) {
            case INFORME_ENVIADO:    return contrato.getRevisor();
            case REVISION_APROBADA:  return contrato.getSupervisor();
            case REVISION_DEVUELTA:  return contrato.getContratista();
            case INFORME_APROBADO:   return contrato.getContratista();
            case INFORME_DEVUELTO:   return contrato.getContratista();
            default:
                return null;
        }
    }

    private static String construirDescripcion(TipoEvento evento, Informe informe, String observacion) {
        String contratista = informe.getContrato().getContratista() != null
            ? informe.getContrato().getContratista().getNombre() : "Contratista";
        String contrato = informe.getContrato().getNumero();
        String periodo = informe.getFechaInicio() + " - " + informe.getFechaFin();

        StringBuilder sb = new StringBuilder();
        switch (evento) {
            case INFORME_ENVIADO:
                sb.append("El informe No. ").append(informe.getNumero())
                  .append(" del contratista ").append(contratista)
                  .append(" (contrato ").append(contrato).append(", periodo ").append(periodo)
                  .append(") ha sido enviado y está pendiente de revisión.");
                break;
            case REVISION_APROBADA:
                sb.append("El informe No. ").append(informe.getNumero())
                  .append(" del contratista ").append(contratista)
                  .append(" (contrato ").append(contrato).append(", periodo ").append(periodo)
                  .append(") fue aprobado por el revisor y está listo para su aprobación final.");
                break;
            case REVISION_DEVUELTA:
                sb.append("Su informe No. ").append(informe.getNumero())
                  .append(" (contrato ").append(contrato).append(", periodo ").append(periodo)
                  .append(") fue devuelto por el revisor para corrección.");
                if (observacion != null && !observacion.isEmpty()) {
                    sb.append(" Observación: ").append(observacion);
                }
                break;
            case INFORME_APROBADO:
                sb.append("Su informe No. ").append(informe.getNumero())
                  .append(" (contrato ").append(contrato).append(", periodo ").append(periodo)
                  .append(") ha sido aprobado. El PDF institucional está disponible para descarga.");
                break;
            case INFORME_DEVUELTO:
                sb.append("Su informe No. ").append(informe.getNumero())
                  .append(" (contrato ").append(contrato).append(", periodo ").append(periodo)
                  .append(") fue devuelto por el supervisor.");
                if (observacion != null && !observacion.isEmpty()) {
                    sb.append(" Observación: ").append(observacion);
                }
                break;
            default:
                sb.append("Evento ").append(evento).append(" en informe ").append(informe.getId());
        }
        return sb.toString();
    }
}
