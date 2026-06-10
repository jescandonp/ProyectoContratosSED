package co.gov.bogota.sed.sigcon.application.dto.contrato;

import javax.validation.constraints.NotNull;

public class BloqueoInformeRequest {
    @NotNull
    private Boolean bloqueado;

    public BloqueoInformeRequest() {}

    public Boolean getBloqueado() { return bloqueado; }
    public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
}
