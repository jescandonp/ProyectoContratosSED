package co.gov.bogota.sed.sigcon.application.dto.informe;

public class DocumentoAdicionalDto {
    private Long id;
    private Long idCatalogo;
    private String nombreCatalogo;
    private Boolean obligatorio;
    private String referencia;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Long idCatalogo) { this.idCatalogo = idCatalogo; }
    public String getNombreCatalogo() { return nombreCatalogo; }
    public void setNombreCatalogo(String nombreCatalogo) { this.nombreCatalogo = nombreCatalogo; }
    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean obligatorio) { this.obligatorio = obligatorio; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
