package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class ActividadInformeRequest {
    @NotNull
    private Long idObligacion;
    @NotBlank
    @Size(max = 3000)
    private String descripcion;
    @NotNull
    private BigDecimal porcentaje;

    public Long getIdObligacion() { return idObligacion; }
    public void setIdObligacion(Long idObligacion) { this.idObligacion = idObligacion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
}
