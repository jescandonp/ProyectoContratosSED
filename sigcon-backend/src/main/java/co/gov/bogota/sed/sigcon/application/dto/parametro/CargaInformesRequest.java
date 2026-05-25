package co.gov.bogota.sed.sigcon.application.dto.parametro;

import javax.validation.constraints.NotNull;

public class CargaInformesRequest {

    @NotNull
    private Boolean activo;

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
