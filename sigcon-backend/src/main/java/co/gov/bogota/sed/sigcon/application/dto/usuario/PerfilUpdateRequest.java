package co.gov.bogota.sed.sigcon.application.dto.usuario;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PerfilUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 200)
    private String cargo;

    @Size(max = 200)
    private String sgssiSaludEntidad;

    @Size(max = 200)
    private String sgssiPensionEntidad;

    @Size(max = 200)
    private String sgssiArlEntidad;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getSgssiSaludEntidad() { return sgssiSaludEntidad; }
    public void setSgssiSaludEntidad(String sgssiSaludEntidad) { this.sgssiSaludEntidad = sgssiSaludEntidad; }
    public String getSgssiPensionEntidad() { return sgssiPensionEntidad; }
    public void setSgssiPensionEntidad(String sgssiPensionEntidad) { this.sgssiPensionEntidad = sgssiPensionEntidad; }
    public String getSgssiArlEntidad() { return sgssiArlEntidad; }
    public void setSgssiArlEntidad(String sgssiArlEntidad) { this.sgssiArlEntidad = sgssiArlEntidad; }
}
