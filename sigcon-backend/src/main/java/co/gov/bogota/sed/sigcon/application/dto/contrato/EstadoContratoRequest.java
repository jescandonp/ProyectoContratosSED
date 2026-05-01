package co.gov.bogota.sed.sigcon.application.dto.contrato;

import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;

import javax.validation.constraints.NotNull;

public class EstadoContratoRequest {
    @NotNull
    private EstadoContrato estado;

    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }
}
