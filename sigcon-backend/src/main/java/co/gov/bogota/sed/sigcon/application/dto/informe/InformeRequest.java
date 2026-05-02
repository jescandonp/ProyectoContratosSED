package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class InformeRequest {
    @NotNull
    private Long idContrato;
    @NotNull
    private LocalDate fechaInicio;
    @NotNull
    private LocalDate fechaFin;

    public Long getIdContrato() { return idContrato; }
    public void setIdContrato(Long idContrato) { this.idContrato = idContrato; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}
