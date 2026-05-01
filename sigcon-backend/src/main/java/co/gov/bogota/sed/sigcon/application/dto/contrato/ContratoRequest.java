package co.gov.bogota.sed.sigcon.application.dto.contrato;

import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ContratoRequest {
    @NotBlank
    @Size(max = 50)
    private String numero;
    @NotBlank
    @Size(max = 1000)
    private String objeto;
    @NotNull
    private TipoContrato tipo;
    @NotNull
    private BigDecimal valorTotal;
    @NotNull
    private LocalDate fechaInicio;
    @NotNull
    private LocalDate fechaFin;
    @NotNull
    private Long idContratista;
    private Long idRevisor;
    private Long idSupervisor;

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getObjeto() { return objeto; }
    public void setObjeto(String objeto) { this.objeto = objeto; }
    public TipoContrato getTipo() { return tipo; }
    public void setTipo(TipoContrato tipo) { this.tipo = tipo; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Long getIdContratista() { return idContratista; }
    public void setIdContratista(Long idContratista) { this.idContratista = idContratista; }
    public Long getIdRevisor() { return idRevisor; }
    public void setIdRevisor(Long idRevisor) { this.idRevisor = idRevisor; }
    public Long getIdSupervisor() { return idSupervisor; }
    public void setIdSupervisor(Long idSupervisor) { this.idSupervisor = idSupervisor; }
}
