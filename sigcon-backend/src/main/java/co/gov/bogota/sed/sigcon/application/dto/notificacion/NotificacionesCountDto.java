package co.gov.bogota.sed.sigcon.application.dto.notificacion;

/** Respuesta del endpoint GET /api/notificaciones/no-leidas/count */
public class NotificacionesCountDto {

    private long count;

    public NotificacionesCountDto() {}

    public NotificacionesCountDto(long count) {
        this.count = count;
    }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
