package co.gov.bogota.sed.sigcon.application.dto.usuario;

import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UsuarioRequest {
    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 200)
    private String cargo;

    @NotNull
    private RolUsuario rol;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }
}
