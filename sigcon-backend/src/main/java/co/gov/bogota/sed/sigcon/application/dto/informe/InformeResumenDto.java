package co.gov.bogota.sed.sigcon.application.dto.informe;

import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InformeResumenDto {
    private Long id;
    private Integer numero;
    private Long contratoId;
    private String contratoNumero;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoInforme estado;
    private LocalDateTime fechaUltimoEnvio;
    private LocalDateTime fechaAprobacion;
    private String pdfRuta;
    private LocalDateTime pdfGeneradoAt;
    private String pdfHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public Long getContratoId() { return contratoId; }
    public void setContratoId(Long contratoId) { this.contratoId = contratoId; }
    public String getContratoNumero() { return contratoNumero; }
    public void setContratoNumero(String contratoNumero) { this.contratoNumero = contratoNumero; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public EstadoInforme getEstado() { return estado; }
    public void setEstado(EstadoInforme estado) { this.estado = estado; }
    public LocalDateTime getFechaUltimoEnvio() { return fechaUltimoEnvio; }
    public void setFechaUltimoEnvio(LocalDateTime fechaUltimoEnvio) { this.fechaUltimoEnvio = fechaUltimoEnvio; }
    public LocalDateTime getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDateTime fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }
    public String getPdfRuta() { return pdfRuta; }
    public void setPdfRuta(String pdfRuta) { this.pdfRuta = pdfRuta; }
    public LocalDateTime getPdfGeneradoAt() { return pdfGeneradoAt; }
    public void setPdfGeneradoAt(LocalDateTime pdfGeneradoAt) { this.pdfGeneradoAt = pdfGeneradoAt; }
    public String getPdfHash() { return pdfHash; }
    public void setPdfHash(String pdfHash) { this.pdfHash = pdfHash; }
}
