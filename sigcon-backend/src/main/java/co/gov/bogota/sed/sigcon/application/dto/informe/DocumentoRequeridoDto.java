package co.gov.bogota.sed.sigcon.application.dto.informe;

/**
 * I7: Representa un documento requerido del informe en la lista de requeridos.
 * Puede estar pendiente (storagePath null) o cargado.
 */
public class DocumentoRequeridoDto {

    private Long id;
    private String claveLogica;
    private String nombreDisplay;
    private boolean cargado;
    private String nombreArchivo;
    private String contentType;
    private String extension;
    private Long tamanoBytes;
    /** true si este requerido es dinamico por IVA (FACTURA). */
    private boolean porIva;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClaveLogica() { return claveLogica; }
    public void setClaveLogica(String claveLogica) { this.claveLogica = claveLogica; }

    public String getNombreDisplay() { return nombreDisplay; }
    public void setNombreDisplay(String nombreDisplay) { this.nombreDisplay = nombreDisplay; }

    public boolean isCargado() { return cargado; }
    public void setCargado(boolean cargado) { this.cargado = cargado; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public boolean isPorIva() { return porIva; }
    public void setPorIva(boolean porIva) { this.porIva = porIva; }
}
