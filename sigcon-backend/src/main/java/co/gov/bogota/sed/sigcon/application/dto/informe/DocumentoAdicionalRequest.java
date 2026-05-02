package co.gov.bogota.sed.sigcon.application.dto.informe;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class DocumentoAdicionalRequest {
    @NotNull
    private Long idCatalogo;
    @NotBlank
    @Size(max = 1000)
    private String referencia;

    public Long getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Long idCatalogo) { this.idCatalogo = idCatalogo; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
