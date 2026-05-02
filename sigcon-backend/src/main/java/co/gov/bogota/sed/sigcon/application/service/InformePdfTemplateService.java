package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Construye el PDF del informe aprobado como XHTML y lo renderiza via Flying Saucer + OpenPDF.
 * El layout institucional esta basado en el DOCX de referencia SED.
 */
@Service
public class InformePdfTemplateService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ActividadInformeRepository actividadRepository;
    private final SoporteAdjuntoRepository soporteRepository;
    private final DocumentoAdicionalRepository documentoAdicionalRepository;

    public InformePdfTemplateService(ActividadInformeRepository actividadRepository,
                                     SoporteAdjuntoRepository soporteRepository,
                                     DocumentoAdicionalRepository documentoAdicionalRepository) {
        this.actividadRepository = actividadRepository;
        this.soporteRepository = soporteRepository;
        this.documentoAdicionalRepository = documentoAdicionalRepository;
    }

    /**
     * Genera los bytes del PDF institucional para el informe indicado.
     *
     * @param informe         informe APROBADO con contrato, contratista y supervisor cargados
     * @param firmaContratista bytes de la imagen de firma del contratista
     * @param firmaSupervisor  bytes de la imagen de firma del supervisor
     * @return bytes del PDF generado
     */
    public byte[] generarPdf(Informe informe, byte[] firmaContratista, byte[] firmaSupervisor)
            throws IOException, DocumentException, Exception {

        List<ActividadInforme> actividades = actividadRepository.findByInformeIdAndActivoTrue(informe.getId());
        List<DocumentoAdicional> documentos = documentoAdicionalRepository.findByInformeIdAndActivoTrue(informe.getId());

        String html = buildHtml(informe, actividades, documentos, firmaContratista, firmaSupervisor);

        // Parsear como XHTML estricto para Flying Saucer
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setFeature("http://xml.org/sax/features/namespaces", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(new InputSource(new StringReader(html)));

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocument(xmlDoc, null);
        renderer.layout();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        renderer.createPDF(baos);
        return baos.toByteArray();
    }

    private String buildHtml(Informe informe,
                              List<ActividadInforme> actividades,
                              List<DocumentoAdicional> documentos,
                              byte[] firmaContratista,
                              byte[] firmaSupervisor) {

        Usuario contratista = informe.getContrato().getContratista();
        Usuario supervisor  = informe.getContrato().getSupervisor();

        String firmaContratistaB64 = toBase64(firmaContratista);
        String firmaSupervisorB64  = toBase64(firmaSupervisor);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        sb.append(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        sb.append("<style type=\"text/css\">");
        sb.append("body{font-family:Arial,sans-serif;font-size:11pt;color:#222;margin:30pt}");
        sb.append("h1{font-size:13pt;color:#003366;text-align:center;margin-bottom:4pt}");
        sb.append("h2{font-size:11pt;color:#003366;margin-top:14pt;margin-bottom:4pt;border-bottom:1pt solid #003366}");
        sb.append("table{width:100%;border-collapse:collapse;margin-bottom:8pt}");
        sb.append("th{background:#003366;color:#fff;padding:4pt 6pt;text-align:left;font-size:10pt}");
        sb.append("td{padding:4pt 6pt;border:0.5pt solid #ccc;font-size:10pt;vertical-align:top}");
        sb.append(".header-block{text-align:center;margin-bottom:16pt}");
        sb.append(".entity{font-weight:bold;font-size:12pt;color:#003366}");
        sb.append(".label{color:#555;font-size:9pt}");
        sb.append(".firma-box{display:inline-block;text-align:center;width:45%;vertical-align:top;padding:6pt}");
        sb.append(".firma-img{max-height:60pt;max-width:150pt}");
        sb.append(".meta{font-size:8pt;color:#777;text-align:right;margin-top:20pt}");
        sb.append(".highlight{background:#f0f4ff}");
        sb.append("</style>");
        sb.append("</head><body>");

        // Encabezado institucional
        sb.append("<div class=\"header-block\">");
        sb.append("<div class=\"entity\">SECRETARÍA DE EDUCACIÓN DEL DISTRITO</div>");
        sb.append("<div>Subdirección de Gesti&#243;n Contractual</div>");
        sb.append("<div><b>INFORME DE ACTIVIDADES</b></div>");
        sb.append("</div>");

        // Datos del contrato
        sb.append("<h2>Datos del Contrato</h2>");
        sb.append("<table><tr>");
        sb.append("<th>N&#250;mero de Contrato</th><th>Tipo</th><th>Valor Total</th>");
        sb.append("</tr><tr class=\"highlight\">");
        sb.append("<td>").append(esc(informe.getContrato().getNumero())).append("</td>");
        sb.append("<td>").append(esc(informe.getContrato().getTipo().name())).append("</td>");
        sb.append("<td>").append(formatPesos(informe.getContrato().getValorTotal())).append("</td>");
        sb.append("</tr></table>");

        sb.append("<table><tr><th>Objeto</th></tr><tr><td>");
        sb.append(esc(informe.getContrato().getObjeto())).append("</td></tr></table>");

        sb.append("<table><tr>");
        sb.append("<th>Contratista</th><th>Cargo</th><th>Vigencia inicio</th><th>Vigencia fin</th>");
        sb.append("</tr><tr>");
        sb.append("<td>").append(esc(contratista.getNombre())).append("</td>");
        sb.append("<td>").append(esc(safe(contratista.getCargo()))).append("</td>");
        sb.append("<td>").append(informe.getContrato().getFechaInicio().format(DATE_FMT)).append("</td>");
        sb.append("<td>").append(informe.getContrato().getFechaFin().format(DATE_FMT)).append("</td>");
        sb.append("</tr></table>");

        // Supervisor
        if (supervisor != null) {
            sb.append("<table><tr><th>Supervisor</th><th>Cargo</th></tr><tr>");
            sb.append("<td>").append(esc(supervisor.getNombre())).append("</td>");
            sb.append("<td>").append(esc(safe(supervisor.getCargo()))).append("</td>");
            sb.append("</tr></table>");
        }

        // Periodo del informe
        sb.append("<h2>Periodo del Informe</h2>");
        sb.append("<table><tr>");
        sb.append("<th>N&#250;mero de Informe</th><th>Desde</th><th>Hasta</th><th>Estado</th>");
        sb.append("</tr><tr class=\"highlight\">");
        sb.append("<td>").append(informe.getNumero()).append("</td>");
        sb.append("<td>").append(informe.getFechaInicio().format(DATE_FMT)).append("</td>");
        sb.append("<td>").append(informe.getFechaFin().format(DATE_FMT)).append("</td>");
        sb.append("<td>APROBADO</td>");
        sb.append("</tr></table>");

        // Obligaciones y actividades
        sb.append("<h2>Obligaciones y Actividades</h2>");
        if (actividades.isEmpty()) {
            sb.append("<p>Sin actividades registradas.</p>");
        } else {
            sb.append("<table><tr>");
            sb.append("<th>Obligaci&#243;n</th><th>Descripci&#243;n</th><th>Avance (%)</th><th>Soportes</th>");
            sb.append("</tr>");
            for (ActividadInforme act : actividades) {
                List<SoporteAdjunto> soportes = soporteRepository.findByActividadIdAndActivoTrue(act.getId());
                sb.append("<tr>");
                sb.append("<td>").append(esc(safe(act.getObligacion() != null ? act.getObligacion().getDescripcion() : ""))).append("</td>");
                sb.append("<td>").append(esc(act.getDescripcion())).append("</td>");
                sb.append("<td>").append(act.getPorcentaje()).append("%</td>");
                sb.append("<td>");
                for (SoporteAdjunto s : soportes) {
                    sb.append("&#8226; ").append(esc(s.getNombre())).append(" (").append(s.getTipo().name()).append(")").append("<br/>");
                }
                if (soportes.isEmpty()) sb.append("Sin soportes");
                sb.append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        // Documentos adicionales
        if (!documentos.isEmpty()) {
            sb.append("<h2>Documentos Adicionales</h2>");
            sb.append("<table><tr><th>Documento</th><th>Referencia</th></tr>");
            for (DocumentoAdicional doc : documentos) {
                sb.append("<tr>");
                sb.append("<td>").append(esc(doc.getCatalogo() != null ? doc.getCatalogo().getNombre() : "")).append("</td>");
                sb.append("<td>").append(esc(safe(doc.getReferencia()))).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        // Firmas
        sb.append("<h2>Firmas</h2>");
        sb.append("<table><tr>");
        sb.append("<td style=\"width:50%;text-align:center;border:none\">");
        if (firmaContratistaB64 != null) {
            sb.append("<img src=\"data:image/png;base64,").append(firmaContratistaB64).append("\" class=\"firma-img\" alt=\"Firma contratista\"/><br/>");
        }
        sb.append("<b>").append(esc(contratista.getNombre())).append("</b><br/>");
        sb.append(esc(safe(contratista.getCargo()))).append("<br/>");
        sb.append("Contratista").append("</td>");
        sb.append("<td style=\"width:50%;text-align:center;border:none\">");
        if (supervisor != null) {
            if (firmaSupervisorB64 != null) {
                sb.append("<img src=\"data:image/png;base64,").append(firmaSupervisorB64).append("\" class=\"firma-img\" alt=\"Firma supervisor\"/><br/>");
            }
            sb.append("<b>").append(esc(supervisor.getNombre())).append("</b><br/>");
            sb.append(esc(safe(supervisor.getCargo()))).append("<br/>");
            sb.append("Supervisor");
        }
        sb.append("</td></tr></table>");

        // Metadata
        String fechaAprobacion = informe.getFechaAprobacion() != null
            ? informe.getFechaAprobacion().format(DATETIME_FMT) : "N/A";
        sb.append("<div class=\"meta\">");
        sb.append("Informe N&#250;mero ").append(informe.getNumero());
        sb.append(" &#183; Contrato ").append(esc(informe.getContrato().getNumero()));
        sb.append(" &#183; Fecha de aprobaci&#243;n: ").append(fechaAprobacion);
        sb.append(" &#183; Estado: APROBADO");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String formatPesos(java.math.BigDecimal value) {
        if (value == null) return "";
        return "$ " + String.format("%,.0f", value);
    }

    private static String toBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return Base64.getEncoder().encodeToString(bytes);
    }
}
