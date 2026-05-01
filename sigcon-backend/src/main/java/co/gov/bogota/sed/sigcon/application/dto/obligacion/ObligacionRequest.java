package co.gov.bogota.sed.sigcon.application.dto.obligacion;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ObligacionRequest {
    @NotBlank
    @Size(max = 2000)
    private String descripcion;

    @NotNull
    private Integer orden;

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
