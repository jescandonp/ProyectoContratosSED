package co.gov.bogota.sed.sigcon.application.dto.contrato;

import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContratoResumenDto {
    private Long id;
    private String numero;
    private String objeto;
    private TipoContrato tipo;
    private EstadoContrato estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal valorTotal;
    private String contratistaNombre;
    private String supervisorNombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getObjeto() { return objeto; }
    public void setObjeto(String objeto) { this.objeto = objeto; }
    public TipoContrato getTipo() { return tipo; }
    public void setTipo(TipoContrato tipo) { this.tipo = tipo; }
    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getContratistaNombre() { return contratistaNombre; }
    public void setContratistaNombre(String contratistaNombre) { this.contratistaNombre = contratistaNombre; }
    public String getSupervisorNombre() { return supervisorNombre; }
    public void setSupervisorNombre(String supervisorNombre) { this.supervisorNombre = supervisorNombre; }
}
