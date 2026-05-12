package co.gov.bogota.sed.sigcon.application.dto.busqueda;

/** I7: Resultado de búsqueda global — contratista. */
public class ContratistaResultadoDto {
    private Long id;
    private String nombre;
    private String email;
    private String cargo;

    public ContratistaResultadoDto() {}

    public ContratistaResultadoDto(Long id, String nombre, String email, String cargo) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.cargo = cargo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
}
