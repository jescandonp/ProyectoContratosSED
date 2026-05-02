package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SoporteUrlRequest {
    @NotBlank
    @Size(max = 200)
    private String nombre;
    @NotBlank
    @Size(max = 1000)
    private String url;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
