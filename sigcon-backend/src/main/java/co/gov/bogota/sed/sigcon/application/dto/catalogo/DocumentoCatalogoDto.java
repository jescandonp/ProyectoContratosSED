package co.gov.bogota.sed.sigcon.application.dto.catalogo;

import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;

public class DocumentoCatalogoDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean obligatorio;
    private TipoContrato tipoContrato;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean obligatorio) { this.obligatorio = obligatorio; }
    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }
}
