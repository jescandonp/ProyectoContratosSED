package co.gov.bogota.sed.sigcon.application.dto.usuario;

import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;

public class UsuarioDto {
    private Long id;
    private String email;
    private String nombre;
    private String cargo;
    private RolUsuario rol;
    private String firmaImagen;
    private Boolean activo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }
    public String getFirmaImagen() { return firmaImagen; }
    public void setFirmaImagen(String firmaImagen) { this.firmaImagen = firmaImagen; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
