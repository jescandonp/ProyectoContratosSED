package co.gov.bogota.sed.sigcon.web.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String error;
    private String mensaje;
    private LocalDateTime timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, String mensaje, LocalDateTime timestamp) {
        this.error = error;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
