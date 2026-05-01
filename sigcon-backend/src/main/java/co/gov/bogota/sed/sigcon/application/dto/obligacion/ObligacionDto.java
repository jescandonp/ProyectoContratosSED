package co.gov.bogota.sed.sigcon.application.dto.obligacion;

public class ObligacionDto {
    private Long id;
    private String descripcion;
    private Integer orden;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
