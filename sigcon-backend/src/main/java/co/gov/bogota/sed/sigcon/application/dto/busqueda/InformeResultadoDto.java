package co.gov.bogota.sed.sigcon.application.dto.busqueda;

/** I7: Resultado de búsqueda global — informe. */
public class InformeResultadoDto {
    private Long id;
    private Integer numero;
    private String estado;
    private String fechaInicio;
    private String fechaFin;
    private String contratoNumero;
    private String contratistaNombre;

    public InformeResultadoDto() {}

    public InformeResultadoDto(Long id, Integer numero, String estado,
                                String fechaInicio, String fechaFin,
                                String contratoNumero, String contratistaNombre) {
        this.id = id;
        this.numero = numero;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.contratoNumero = contratoNumero;
        this.contratistaNombre = contratistaNombre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
    public String getContratoNumero() { return contratoNumero; }
    public void setContratoNumero(String contratoNumero) { this.contratoNumero = contratoNumero; }
    public String getContratistaNombre() { return contratistaNombre; }
    public void setContratistaNombre(String contratistaNombre) { this.contratistaNombre = contratistaNombre; }
}
