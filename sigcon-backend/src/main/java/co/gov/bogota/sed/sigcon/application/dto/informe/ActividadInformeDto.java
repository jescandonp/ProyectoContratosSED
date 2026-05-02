package co.gov.bogota.sed.sigcon.application.dto.informe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ActividadInformeDto {
    private Long id;
    private Long idObligacion;
    private Integer ordenObligacion;
    private String descripcionObligacion;
    private String descripcion;
    private BigDecimal porcentaje;
    private List<SoporteAdjuntoDto> soportes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdObligacion() { return idObligacion; }
    public void setIdObligacion(Long idObligacion) { this.idObligacion = idObligacion; }
    public Integer getOrdenObligacion() { return ordenObligacion; }
    public void setOrdenObligacion(Integer ordenObligacion) { this.ordenObligacion = ordenObligacion; }
    public String getDescripcionObligacion() { return descripcionObligacion; }
    public void setDescripcionObligacion(String descripcionObligacion) { this.descripcionObligacion = descripcionObligacion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
    public List<SoporteAdjuntoDto> getSoportes() { return soportes; }
    public void setSoportes(List<SoporteAdjuntoDto> soportes) { this.soportes = soportes; }
}
