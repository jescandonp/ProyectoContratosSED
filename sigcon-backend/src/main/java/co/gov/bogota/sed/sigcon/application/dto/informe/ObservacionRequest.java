package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ObservacionRequest {
    @NotBlank
    @Size(max = 2000)
    private String texto;

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
