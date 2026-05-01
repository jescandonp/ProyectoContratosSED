package co.gov.bogota.sed.sigcon.application.dto.usuario;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PerfilUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 200)
    private String cargo;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
}
