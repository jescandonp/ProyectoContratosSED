package co.gov.bogota.sed.sigcon.application.dto.usuario;

import javax.validation.constraints.NotNull;

public class EstadoUsuarioRequest {
    @NotNull
    private Boolean activo;

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
