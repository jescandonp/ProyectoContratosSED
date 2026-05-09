package co.gov.bogota.sed.sigcon.application.dto.informe;

import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InformeUpdateDto {
    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;

    private Integer numeroDesembolso;
    private BigDecimal valorDesembolso;
    private BigDecimal porcentajeEjecucion;
    private Boolean correspondenciaPendiente;

    @Valid
    private List<AporteSgssiRequest> aportesSgssi = new ArrayList<>();

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Integer getNumeroDesembolso() { return numeroDesembolso; }
    public void setNumeroDesembolso(Integer numeroDesembolso) { this.numeroDesembolso = numeroDesembolso; }
    public BigDecimal getValorDesembolso() { return valorDesembolso; }
    public void setValorDesembolso(BigDecimal valorDesembolso) { this.valorDesembolso = valorDesembolso; }
    public BigDecimal getPorcentajeEjecucion() { return porcentajeEjecucion; }
    public void setPorcentajeEjecucion(BigDecimal porcentajeEjecucion) { this.porcentajeEjecucion = porcentajeEjecucion; }
    public Boolean getCorrespondenciaPendiente() { return correspondenciaPendiente; }
    public void setCorrespondenciaPendiente(Boolean correspondenciaPendiente) { this.correspondenciaPendiente = correspondenciaPendiente; }
    public List<AporteSgssiRequest> getAportesSgssi() { return aportesSgssi; }
    public void setAportesSgssi(List<AporteSgssiRequest> aportesSgssi) { this.aportesSgssi = aportesSgssi; }
}
