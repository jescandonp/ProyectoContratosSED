package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.AporteSgssiRepository;
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
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Construye el PDF institucional SED del informe aprobado en XHTML y lo renderiza
 * via Flying Saucer + OpenPDF. Layout de 8 secciones segun plantilla SED I6.
 */
@Service
public class InformePdfTemplateService {

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale            LOCALE_CO    = new Locale("es", "CO");

    private final ActividadInformeRepository  actividadRepository;
    private final SoporteAdjuntoRepository    soporteRepository;
    private final DocumentoAdicionalRepository documentoAdicionalRepository;
    private final AporteSgssiRepository       aporteSgssiRepository;

    public InformePdfTemplateService(ActividadInformeRepository actividadRepository,
                                     SoporteAdjuntoRepository soporteRepository,
                                     DocumentoAdicionalRepository documentoAdicionalRepository,
                                     AporteSgssiRepository aporteSgssiRepository) {
        this.actividadRepository         = actividadRepository;
        this.soporteRepository           = soporteRepository;
        this.documentoAdicionalRepository = documentoAdicionalRepository;
        this.aporteSgssiRepository        = aporteSgssiRepository;
    }

    /**
     * Genera los bytes del PDF institucional SED para el informe indicado.
     *
     * @param informe          informe APROBADO con contrato, contratista y supervisor cargados
     * @param firmaContratista bytes de la imagen de firma del contratista
     * @param firmaSupervisor  bytes de la imagen de firma del supervisor
     * @param firmaRevisor     bytes de la imagen de firma del revisor (puede ser null)
     */
    public byte[] generarPdf(Informe informe,
                             byte[] firmaContratista,
                             byte[] firmaSupervisor,
                             byte[] firmaRevisor)
            throws IOException, DocumentException, Exception {

        List<ActividadInforme>  actividades = actividadRepository.findByInformeIdAndActivoTrue(informe.getId());
        List<DocumentoAdicional> documentos = documentoAdicionalRepository.findByInformeIdAndActivoTrue(informe.getId());
        List<AporteSgssi>        aportes    = aporteSgssiRepository.findByInformeIdAndActivoTrue(informe.getId());

        String html = buildHtml(informe, actividades, documentos, aportes,
                firmaContratista, firmaSupervisor, firmaRevisor);

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

    // ─────────────────────────────────────────────────────────────────────────

    private String buildHtml(Informe informe,
                              List<ActividadInforme>  actividades,
                              List<DocumentoAdicional> documentos,
                              List<AporteSgssi>        aportes,
                              byte[] firmaContratista,
                              byte[] firmaSupervisor,
                              byte[] firmaRevisor) {

        Usuario contratista = informe.getContrato().getContratista();
        Usuario supervisor  = informe.getContrato().getSupervisor();
        Usuario revisor     = informe.getContrato().getRevisor();

        StringBuilder sb = new StringBuilder(8192);

        // ── XHTML boilerplate ─────────────────────────────────────────────
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        sb.append(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        appendCss(sb);
        sb.append("</head><body>");

        // ── Sec 1: Encabezado institucional ──────────────────────────────
        sb.append("<div class=\"hdr\">");
        sb.append("<div class=\"hdr-entity\">SECRETAR&#205;A DE EDUCACI&#211;N DEL DISTRITO</div>");
        sb.append("<div class=\"hdr-sub\">Subdirecci&#243;n de Gesti&#243;n Contractual</div>");
        sb.append("<div class=\"hdr-title\">INFORME DE ACTIVIDADES N&#250;m. ")
          .append(informe.getNumero()).append("</div>");
        sb.append("</div>");

        // ── Sec 2: Datos del Contrato ────────────────────────────────────
        sb.append("<div class=\"sec-title\">DATOS DEL CONTRATO</div>");
        sb.append("<table><tbody>");
        row2(sb, "N&#250;mero de Contrato", esc(informe.getContrato().getNumero()), false);
        row2(sb, "Tipo de Contrato", esc(informe.getContrato().getTipo().name()), true);
        row2(sb, "Valor Total", formatPesos(informe.getContrato().getValorTotal()), false);
        row2(sb, "Valor en Letras", esc(NumeroPesosConverter.convertir(informe.getContrato().getValorTotal())), true);
        row2(sb, "Vigencia", informe.getContrato().getFechaInicio().format(DATE_FMT)
                + " al " + informe.getContrato().getFechaFin().format(DATE_FMT), false);
        if (notEmpty(informe.getContrato().getDependencia())) {
            row2(sb, "Dependencia", esc(informe.getContrato().getDependencia()), true);
        }
        if (notEmpty(informe.getContrato().getFormaPago())) {
            row2(sb, "Forma de Pago", esc(informe.getContrato().getFormaPago()), false);
        }
        sb.append("</tbody></table>");

        // Objeto en fila completa
        sb.append("<table><tbody>");
        sb.append("<tr><td class=\"lbl\">Objeto</td><td class=\"val\">")
          .append(esc(informe.getContrato().getObjeto())).append("</td></tr>");
        sb.append("</tbody></table>");

        if (notEmpty(informe.getContrato().getModificaciones())) {
            sb.append("<table><tbody>");
            sb.append("<tr><td class=\"lbl\">Modificaciones / Adiciones</td><td class=\"val\">")
              .append(esc(informe.getContrato().getModificaciones())).append("</td></tr>");
            sb.append("</tbody></table>");
        }

        // ── Sec 3: Partes del Contrato ───────────────────────────────────
        sb.append("<div class=\"sec-title\">PARTES DEL CONTRATO</div>");
        sb.append("<table><thead><tr>");
        sb.append("<th>Rol</th><th>Nombre</th><th>Cargo / Entidad</th>");
        sb.append("</tr></thead><tbody>");

        sb.append("<tr class=\"alt\">");
        sb.append("<td>Contratista</td>");
        sb.append("<td>").append(esc(contratista.getNombre())).append("</td>");
        sb.append("<td>").append(esc(safe(contratista.getCargo()))).append("</td>");
        sb.append("</tr>");

        if (revisor != null) {
            sb.append("<tr>");
            sb.append("<td>Revisor</td>");
            sb.append("<td>").append(esc(revisor.getNombre())).append("</td>");
            sb.append("<td>").append(esc(safe(revisor.getCargo()))).append("</td>");
            sb.append("</tr>");
        }

        if (supervisor != null) {
            sb.append("<tr class=\"alt\">");
            sb.append("<td>Supervisor</td>");
            sb.append("<td>").append(esc(supervisor.getNombre())).append("</td>");
            sb.append("<td>").append(esc(safe(supervisor.getCargo()))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");

        // ── Sec 4: Datos del Informe ─────────────────────────────────────
        sb.append("<div class=\"sec-title\">DATOS DEL INFORME</div>");
        sb.append("<table><tbody>");
        row2(sb, "N&#250;mero de Informe", String.valueOf(informe.getNumero()), false);
        row2(sb, "Per&#237;odo", informe.getFechaInicio().format(DATE_FMT)
                + " al " + informe.getFechaFin().format(DATE_FMT), true);
        row2(sb, "Estado", "APROBADO", false);

        if (informe.getNumeroDesembolso() != null) {
            row2(sb, "N&#250;mero de Desembolso", String.valueOf(informe.getNumeroDesembolso()), true);
        }
        if (informe.getValorDesembolso() != null) {
            row2(sb, "Valor del Desembolso", formatPesos(informe.getValorDesembolso()), false);
        }
        if (informe.getPorcentajeEjecucion() != null) {
            row2(sb, "Porcentaje de Ejecuci&#243;n", informe.getPorcentajeEjecucion().stripTrailingZeros().toPlainString() + " %", true);
        }
        boolean corrPendiente = informe.getCorrespondenciaPendiente() != null
                && informe.getCorrespondenciaPendiente() > 0;
        row2(sb, "Correspondencia Pendiente", corrPendiente ? "S&#237;" : "No", false);
        sb.append("</tbody></table>");

        // ── Sec 5: Aportes al Sistema de Seguridad Social (SGSSI) ────────
        sb.append("<div class=\"sec-title\">APORTES AL SISTEMA GENERAL DE SEGURIDAD SOCIAL</div>");
        if (aportes.isEmpty()) {
            sb.append("<p class=\"empty-msg\">Sin aportes SGSSI registrados para este informe.</p>");
        } else {
            YearMonth periodoSgssi = YearMonth.from(informe.getFechaInicio()).minusMonths(1);
            String periodoLabel = periodoSgssi.atDay(1)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_CO)).toUpperCase();
            sb.append("<p class=\"sgssi-periodo\">Per&#237;odo de aportes: <b>").append(esc(periodoLabel)).append("</b></p>");
            sb.append("<table><thead><tr>");
            sb.append("<th>Concepto</th><th>Entidad</th><th>Fecha de Pago</th><th>Valor Aportado</th>");
            sb.append("</tr></thead><tbody>");
            boolean alt = false;
            for (AporteSgssi aporte : aportes) {
                sb.append(alt ? "<tr class=\"alt\">" : "<tr>");
                sb.append("<td>").append(esc(labelSgssi(aporte.getItem()))).append("</td>");
                sb.append("<td>").append(esc(safe(aporte.getEntidad()))).append("</td>");
                sb.append("<td>").append(aporte.getFechaPago() != null ? aporte.getFechaPago().format(DATE_FMT) : "").append("</td>");
                sb.append("<td>").append(formatPesos(aporte.getValorAportado())).append("</td>");
                sb.append("</tr>");
                alt = !alt;
            }
            sb.append("</tbody></table>");
        }

        // ── Sec 6: Obligaciones y Actividades ────────────────────────────
        sb.append("<div class=\"sec-title\">OBLIGACIONES Y ACTIVIDADES</div>");
        if (actividades.isEmpty()) {
            sb.append("<p class=\"empty-msg\">Sin actividades registradas.</p>");
        } else {
            sb.append("<table><thead><tr>");
            sb.append("<th>Obligaci&#243;n</th><th>Descripci&#243;n</th><th>Soportes</th>");
            sb.append("</tr></thead><tbody>");
            boolean alt = false;
            for (ActividadInforme act : actividades) {
                List<SoporteAdjunto> soportes = soporteRepository.findByActividadIdAndActivoTrue(act.getId());
                sb.append(alt ? "<tr class=\"alt\">" : "<tr>");
                sb.append("<td>")
                  .append(esc(act.getObligacion() != null ? safe(act.getObligacion().getDescripcion()) : ""))
                  .append("</td>");
                sb.append("<td>").append(esc(act.getDescripcion())).append("</td>");
                sb.append("<td>");
                if (soportes.isEmpty()) {
                    sb.append("Sin soportes");
                } else {
                    for (SoporteAdjunto s : soportes) {
                        sb.append("&#8226; ").append(esc(s.getNombre()))
                          .append(" (").append(s.getTipo().name()).append(")<br/>");
                    }
                }
                sb.append("</td></tr>");
                alt = !alt;
            }
            sb.append("</tbody></table>");
        }

        // ── Sec 7: Documentos Adicionales ────────────────────────────────
        if (!documentos.isEmpty()) {
            sb.append("<div class=\"sec-title\">DOCUMENTOS ADICIONALES</div>");
            sb.append("<table><thead><tr><th>Documento</th><th>Referencia</th></tr></thead><tbody>");
            boolean alt = false;
            for (DocumentoAdicional doc : documentos) {
                sb.append(alt ? "<tr class=\"alt\">" : "<tr>");
                sb.append("<td>")
                  .append(esc(doc.getCatalogo() != null ? doc.getCatalogo().getNombre() : ""))
                  .append("</td>");
                sb.append("<td>").append(esc(safe(doc.getReferencia()))).append("</td>");
                sb.append("</tr>");
                alt = !alt;
            }
            sb.append("</tbody></table>");
        }

        // ── Sec 8: Firmas ─────────────────────────────────────────────────
        sb.append("<div class=\"sec-title\">FIRMAS</div>");
        boolean hasRevisorFirma = firmaRevisor != null && firmaRevisor.length > 0;
        int cols = hasRevisorFirma ? 3 : 2;
        String colWidth = cols == 3 ? "33%" : "50%";

        sb.append("<table class=\"firma-table\"><tbody><tr>");

        // Contratista
        appendFirmaCell(sb, firmaContratista, contratista.getNombre(), safe(contratista.getCargo()), "Contratista", colWidth);

        // Revisor (solo si tiene firma)
        if (hasRevisorFirma && revisor != null) {
            appendFirmaCell(sb, firmaRevisor, revisor.getNombre(), safe(revisor.getCargo()), "Revisor", colWidth);
        }

        // Supervisor
        if (supervisor != null) {
            appendFirmaCell(sb, firmaSupervisor, supervisor.getNombre(), safe(supervisor.getCargo()), "Supervisor", colWidth);
        }

        sb.append("</tr></tbody></table>");

        // Metadata
        String fechaAprobacion = informe.getFechaAprobacion() != null
                ? informe.getFechaAprobacion().format(DATETIME_FMT) : "N/A";
        sb.append("<div class=\"meta\">");
        sb.append("Informe N&#250;m. ").append(informe.getNumero());
        sb.append(" &#183; Contrato ").append(esc(informe.getContrato().getNumero()));
        sb.append(" &#183; Aprobado: ").append(fechaAprobacion);
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // ─── CSS ─────────────────────────────────────────────────────────────────

    private static void appendCss(StringBuilder sb) {
        sb.append("<style type=\"text/css\">");
        sb.append("body{font-family:Arial,sans-serif;font-size:10pt;color:#1a1a1a;margin:24pt 30pt 24pt 30pt}");
        sb.append(".hdr{text-align:center;margin-bottom:14pt;padding-bottom:6pt;border-bottom:1.5pt solid #002869}");
        sb.append(".hdr-entity{font-weight:bold;font-size:13pt;color:#002869;text-transform:uppercase}");
        sb.append(".hdr-sub{font-size:10pt;color:#444;margin-top:2pt}");
        sb.append(".hdr-title{font-size:12pt;font-weight:bold;color:#002869;margin-top:6pt;text-transform:uppercase}");
        sb.append(".sec-title{background:#C0C0C0;color:#002869;font-weight:bold;font-size:10pt;");
        sb.append("padding:4pt 8pt;margin-top:10pt;margin-bottom:4pt;text-transform:uppercase}");
        sb.append("table{width:100%;border-collapse:collapse;margin-bottom:4pt;font-size:9.5pt}");
        sb.append("th{background:#002869;color:#fff;padding:4pt 6pt;text-align:left;font-size:9pt;font-weight:bold}");
        sb.append("td{padding:3pt 6pt;border:0.5pt solid #c0c4cc;vertical-align:top}");
        sb.append(".alt{background:#eff4ff}");
        sb.append(".lbl{color:#333;font-weight:bold;width:38%}");
        sb.append(".val{color:#111}");
        sb.append(".sgssi-periodo{font-size:9pt;color:#444;margin:2pt 0 4pt 0}");
        sb.append(".empty-msg{font-size:9pt;color:#666;font-style:italic;margin:2pt 0 6pt 0}");
        sb.append(".firma-table{border-collapse:collapse;margin-top:10pt}");
        sb.append(".firma-cell{text-align:center;border:none;padding:8pt 14pt;vertical-align:bottom}");
        sb.append(".firma-img{max-height:55pt;max-width:140pt}");
        sb.append(".firma-name{font-weight:bold;font-size:9.5pt;margin-top:4pt}");
        sb.append(".firma-cargo{font-size:8.5pt;color:#444}");
        sb.append(".firma-rol{font-size:8pt;color:#002869;font-weight:bold;text-transform:uppercase;margin-top:2pt}");
        sb.append(".meta{font-size:7.5pt;color:#999;text-align:right;margin-top:14pt;");
        sb.append("border-top:0.5pt solid #ddd;padding-top:4pt}");
        sb.append("</style>");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void row2(StringBuilder sb, String label, String value, boolean alt) {
        sb.append(alt ? "<tr class=\"alt\">" : "<tr>");
        sb.append("<td class=\"lbl\">").append(label).append("</td>");
        sb.append("<td class=\"val\">").append(value).append("</td>");
        sb.append("</tr>");
    }

    private static void appendFirmaCell(StringBuilder sb, byte[] firma,
                                         String nombre, String cargo, String rol, String width) {
        sb.append("<td class=\"firma-cell\" style=\"width:").append(width).append(";border:none\">");
        String b64 = toBase64(firma);
        if (b64 != null) {
            sb.append("<img src=\"data:image/png;base64,").append(b64)
              .append("\" class=\"firma-img\" alt=\"Firma ").append(rol.toLowerCase()).append("\"/>");
        } else {
            sb.append("<div style=\"height:55pt\"/>"); // espacio en blanco si no hay firma
        }
        sb.append("<br/><div class=\"firma-name\">").append(esc(nombre)).append("</div>");
        if (!cargo.isEmpty()) {
            sb.append("<div class=\"firma-cargo\">").append(esc(cargo)).append("</div>");
        }
        sb.append("<div class=\"firma-rol\">").append(esc(rol)).append("</div>");
        sb.append("</td>");
    }

    private static String labelSgssi(ItemSgssi item) {
        if (item == null) return "";
        switch (item) {
            case SALUD:   return "Salud";
            case PENSION: return "Pensión";
            case ARL:     return "A.R.L.";
            default:      return item.name();
        }
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

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String formatPesos(BigDecimal value) {
        if (value == null) return "";
        return "$ " + String.format(LOCALE_CO, "%,.0f", value);
    }

    private static String toBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return Base64.getEncoder().encodeToString(bytes);
    }
}
