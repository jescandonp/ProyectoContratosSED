package co.gov.bogota.sed.sigcon.application.dto.catalogo;

import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class DocumentoCatalogoRequest {
    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    private Boolean obligatorio;

    @NotNull
    private TipoContrato tipoContrato;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean obligatorio) { this.obligatorio = obligatorio; }
    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }
}
