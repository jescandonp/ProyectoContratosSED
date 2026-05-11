package co.gov.bogota.sed.sigcon.application.dto.informe;

/**
 * I7: Preview basico de un archivo .eml.
 * Si el contenido es complejo, previewParcial=true y se conserva la descarga del original.
 */
public class EmlPreviewDto {

    private String asunto;
    private String remitente;
    private String destinatarios;
    private String fecha;
    private String cuerpoTexto;
    /** true si el contenido es complejo y el preview es parcial. */
    private boolean previewParcial;

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }

    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(String destinatarios) { this.destinatarios = destinatarios; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getCuerpoTexto() { return cuerpoTexto; }
    public void setCuerpoTexto(String cuerpoTexto) { this.cuerpoTexto = cuerpoTexto; }

    public boolean isPreviewParcial() { return previewParcial; }
    public void setPreviewParcial(boolean previewParcial) { this.previewParcial = previewParcial; }
}
