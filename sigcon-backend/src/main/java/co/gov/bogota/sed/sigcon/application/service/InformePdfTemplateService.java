package co.gov.bogota.sed.sigcon.application.service;

import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.SoporteAdjunto;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.AporteSgssiRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Construye el PDF institucional SED del informe aprobado en XHTML y lo renderiza
 * via Flying Saucer + OpenPDF. Layout segun plantilla 11-IF-023 V1.
 */
@Service
public class InformePdfTemplateService {

    private static final Logger log = LoggerFactory.getLogger(InformePdfTemplateService.class);

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale            LOCALE_CO = new Locale("es", "CO");
    private static final String LOGO_WIDTH_PT = "62.08pt";  // 2.19 cm
    private static final String LOGO_HEIGHT_PT = "50.17pt"; // 1.77 cm
    private static final String CODIGO_DOCUMENTO = "11-IF-023";
    private static final String VERSION_DOCUMENTO = "V1";

    private final ActividadInformeRepository   actividadRepository;
    private final SoporteAdjuntoRepository     soporteRepository;
    private final DocumentoAdicionalRepository documentoAdicionalRepository;
    private final AporteSgssiRepository        aporteSgssiRepository;

    private String logoBase64 = null;

    public InformePdfTemplateService(ActividadInformeRepository actividadRepository,
                                     SoporteAdjuntoRepository soporteRepository,
                                     DocumentoAdicionalRepository documentoAdicionalRepository,
                                     AporteSgssiRepository aporteSgssiRepository) {
        this.actividadRepository          = actividadRepository;
        this.soporteRepository            = soporteRepository;
        this.documentoAdicionalRepository = documentoAdicionalRepository;
        this.aporteSgssiRepository        = aporteSgssiRepository;
    }

    @PostConstruct
    public void cargarLogo() {
        try {
            ClassPathResource res = new ClassPathResource("logo-alcaldia.png");
            if (res.exists()) {
                byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
                logoBase64 = Base64.getEncoder().encodeToString(bytes);
            } else {
                log.warn("logo-alcaldia.png no encontrado en classpath; encabezado se generara sin logo.");
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar logo-alcaldia.png: {}", e.getMessage());
        }
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
            throws IOException, DocumentException, ParserConfigurationException, SAXException {

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

        StringBuilder sb = new StringBuilder(16384);

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
        sb.append(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        appendCss(sb);
        sb.append("</head><body>");

        appendRunningHeader(sb, informe);

        appendSeccion1(sb, informe);
        appendSeccion2(sb, actividades);
        appendSeccion3(sb, informe, aportes);
        appendSeccion4(sb, informe);
        appendSeccion5(sb, informe);

        appendFirmas(sb, informe, contratista, supervisor, revisor,
                     firmaContratista, firmaSupervisor, firmaRevisor);

        appendRunningFooter(sb);

        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendRunningFooter(StringBuilder sb) {
        sb.append("<div class=\"running-footer\">");
        sb.append("<div class=\"footer-code\">").append(CODIGO_DOCUMENTO).append("<br/>")
          .append(VERSION_DOCUMENTO).append("</div>");
        sb.append("<div class=\"footer-page\">P&#225;gina: <span class=\"page-num\"></span> de <span class=\"page-total\"></span></div>");
        sb.append("<div class=\"footer-address\">");
        sb.append("Avenida El Dorado N&#176; 66-63 &nbsp;&nbsp;&nbsp; PBX: 3241000 &nbsp;&nbsp;&nbsp; ");
        sb.append("www.educacionbogota.edu.co &nbsp;&nbsp;&nbsp; L&#237;nea 195");
        sb.append("</div>");
        sb.append("</div>");
    }

    // ─── CSS ─────────────────────────────────────────────────────────────────

    private void appendCss(StringBuilder sb) {
        sb.append("<style type=\"text/css\">");
        sb.append("@page{margin:82pt 36pt 58pt 36pt;}");
        sb.append("@page{@top-center{content:element(pageHeader)}@bottom-center{content:element(pageFooter)}}");
        sb.append(".running-header{position:running(pageHeader);width:100%;}");
        sb.append(".running-footer{position:running(pageFooter);width:100%;");
        sb.append("font-size:7.5pt;color:#666;padding-top:6pt;position:relative;height:36pt;}");
        sb.append(".footer-code{position:absolute;right:0;top:0;text-align:center;line-height:8pt;}");
        sb.append(".footer-page{text-align:center;margin-top:9pt;}");
        sb.append(".footer-address{text-align:center;margin-top:8pt;white-space:nowrap;}");
        sb.append(".page-num:before{content:counter(page);}");
        sb.append(".page-total:before{content:counter(pages);}");
        sb.append("body{font-family:Arial,sans-serif;font-size:9pt;color:#1a1a1a;margin:0;}");
        sb.append(".ph-wrap{border:0.8pt solid #000;width:100%;border-collapse:collapse;font-size:8pt;}");
        sb.append(".ph-logo{width:14%;text-align:center;padding:2pt;border-right:0.8pt solid #000;vertical-align:middle;}");
        sb.append(".ph-logo img{width:").append(LOGO_WIDTH_PT).append(";height:")
          .append(LOGO_HEIGHT_PT).append(";}");
        sb.append(".ph-center{width:60%;text-align:center;vertical-align:top;padding:2pt 4pt;}");
        sb.append(".ph-center-title{font-weight:bold;font-size:9.5pt;}");
        sb.append(".ph-center-sub{font-size:7.5pt;margin-top:1pt;}");
        sb.append(".ph-right{width:26%;text-align:center;vertical-align:top;padding:2pt;border-left:0.8pt solid #000;}");
        sb.append(".ph-right-title{font-weight:bold;font-size:8pt;}");
        sb.append(".ph-right-period{font-size:7.5pt;margin-top:1pt;}");
        sb.append(".data-table{table-layout:fixed;}");
        sb.append(".sec-title{background:#d9d9d9;color:#000;font-weight:bold;font-size:9pt;");
        sb.append("padding:3pt 6pt;margin-top:8pt;margin-bottom:0;text-transform:uppercase;border:0.5pt solid #999;}");
        sb.append("table{width:100%;border-collapse:collapse;font-size:8.5pt;margin-bottom:0;}");
        sb.append("th{background:#000;color:#fff;padding:3pt 5pt;text-align:left;font-size:8pt;font-weight:bold;border:0.5pt solid #555;}");
        sb.append("td{padding:3pt 5pt;border:0.5pt solid #999;vertical-align:top;}");
        sb.append(".lbl{font-weight:bold;width:30%;background:#f0f0f0;}");
        sb.append(".val{width:70%;}");
        sb.append(".lbl4{font-weight:bold;background:#f0f0f0;white-space:nowrap;}");
        sb.append(".val4{white-space:nowrap;}");
        sb.append(".bullet-list{margin:0;padding:0 0 0 10pt;list-style-type:disc;}");
        sb.append(".bullet-list li{margin-bottom:2pt;}");
        sb.append(".parr{font-size:8.5pt;text-align:justify;margin:4pt 0;}");
        sb.append(".firma-section{margin-top:14pt;}");
        sb.append(".firma-table{width:100%;border-collapse:collapse;}");
        sb.append(".firma-cell{text-align:center;padding:8pt 10pt;vertical-align:bottom;border:none;width:50%;}");
        sb.append(".firma-cell-full{text-align:center;padding:8pt 10pt;vertical-align:bottom;border:none;width:100%;}");
        sb.append(".firma-img-wrap{height:50pt;text-align:center;vertical-align:bottom;overflow:hidden;}");
        sb.append(".firma-img{height:44pt;width:150pt;}");
        sb.append(".firma-line{border-top:0.8pt solid #000;margin:2pt auto;width:180pt;}");
        sb.append(".firma-name{font-weight:bold;font-size:8.5pt;margin-top:2pt;}");
        sb.append(".firma-cargo{font-size:8pt;color:#333;}");
        sb.append(".firma-rol{font-size:7.5pt;font-weight:bold;text-transform:uppercase;color:#000;margin-top:1pt;}");
        sb.append("</style>");
    }

    // ─── Running header ───────────────────────────────────────────────────────

    private void appendRunningHeader(StringBuilder sb, Informe informe) {
        String numContrato  = esc(informe.getContrato().getNumero());
        int    anioContrato = informe.getContrato().getFechaInicio().getYear();
        String periodoDesde = informe.getFechaInicio().format(DATE_FMT);
        String periodoHasta = informe.getFechaFin().format(DATE_FMT);

        sb.append("<div class=\"running-header\">");
        sb.append("<table class=\"ph-wrap\"><tbody><tr>");

        sb.append("<td class=\"ph-logo\">");
        if (logoBase64 != null) {
            sb.append("<img src=\"data:image/png;base64,").append(logoBase64)
              .append("\" style=\"width:").append(LOGO_WIDTH_PT)
              .append(";height:").append(LOGO_HEIGHT_PT)
              .append(";\" alt=\"Logo SED\"/>");
        }
        sb.append("</td>");

        sb.append("<td class=\"ph-center\">");
        sb.append("<div class=\"ph-center-title\">INFORME DE ACTIVIDADES No. ")
          .append(String.format("%02d", informe.getNumero())).append("</div>");
        sb.append("<div class=\"ph-center-sub\">")
          .append(textoTipoContrato(informe.getContrato().getTipo()))
          .append("</div>");
        sb.append("<div class=\"ph-center-sub\">").append(numContrato)
          .append(" DEL ").append(anioContrato).append("</div>");
        sb.append("</td>");

        sb.append("<td class=\"ph-right\">");
        sb.append("<div class=\"ph-right-title\">P&#225;gina <span class=\"page-num\"></span> de <span class=\"page-total\"></span></div>");
        sb.append("<div class=\"ph-right-title\" style=\"margin-top:5pt;\">PERIODO DEL INFORME</div>");
        sb.append("<div class=\"ph-right-period\">Desde (").append(periodoDesde).append(")</div>");
        sb.append("<div class=\"ph-right-period\">Hasta (").append(periodoHasta).append(")</div>");
        sb.append("</td>");

        sb.append("</tr></tbody></table>");
        sb.append("</div>");
    }

    private String textoTipoContrato(TipoContrato tipoContrato) {
        if (tipoContrato == TipoContrato.PRO) {
            return "CONTRATO DE APOYO A LA GESTION";
        }
        return "CONTRATO DE PRESTACION DE SERVICIOS PROFESIONALES";
    }

    // ─── Secciones ────────────────────────────────────────────────────────────

    private void appendSeccion1(StringBuilder sb, Informe informe) {
        Contrato c   = informe.getContrato();
        Usuario  sup = c.getSupervisor();

        sb.append("<div class=\"sec-title\">1. DATOS DEL CONTRATO</div>");
        sb.append("<table class=\"data-table\"><tbody>");

        fila2(sb, "Contratista:", esc(c.getContratista().getNombre()), false);
        fila2(sb, "Objeto:", esc(c.getObjeto()), true);

        String valorTexto = "El valor del contrato es de "
            + esc(NumeroPesosConverter.convertir(c.getValorTotal()))
            + " (" + formatPesos(c.getValorTotal()) + ").";
        fila2(sb, "Valor del Contrato:", valorTexto, false);

        fila2(sb, "Forma de Pago:", esc(safe(c.getFormaPago())), true);

        String plazo = "El plazo del contrato ser&#225; hasta el " + c.getFechaFin().format(DATE_FMT)
            + " y a partir de la suscripci&#243;n del acta de inicio, previo cumplimiento de los"
            + " requisitos de perfeccionamiento y ejecuci&#243;n. En todo caso, la fecha de Inicio"
            + " no podr&#225; ser anterior al " + c.getFechaInicio().format(DATE_FMT) + ".";
        fila2(sb, "Plazo:", plazo, false);

        fila2(sb, "Modificaciones:", esc(notEmpty(c.getModificaciones()) ? c.getModificaciones() : "No se han presentado"), true);

        fila4(sb,
            "Fecha de Inicio:", c.getFechaInicio().format(DATE_FMT),
            "Fecha de Terminaci&#243;n:", c.getFechaFin().format(DATE_FMT),
            false);

        fila2(sb, "Dependencia:", esc(safe(c.getDependencia())), false);

        String supNombreCargo = sup != null
            ? esc(sup.getNombre()) + " &#8211; " + esc(safe(sup.getCargo()))
            : "No asignado";
        fila2(sb, "Supervisor - Cargo:", supNombreCargo, true);

        sb.append("</tbody></table>");
    }

    private void appendSeccion2(StringBuilder sb, List<ActividadInforme> actividades) {
        sb.append("<div class=\"sec-title\">2. EJECUCI&#211;N DE ACTIVIDADES FRENTE A LAS OBLIGACIONES DURANTE EL PER&#205;ODO REPORTADO</div>");
        sb.append("<table><thead><tr>");
        sb.append("<th style=\"width:30%\">Obligaci&#243;n Contractual</th>");
        sb.append("<th style=\"width:40%\">Actividades realizadas</th>");
        sb.append("<th style=\"width:30%\">Evidencia Verificable</th>");
        sb.append("</tr></thead><tbody>");

        boolean alt   = false;
        int     orden = 1;
        for (ActividadInforme act : actividades) {
            List<SoporteAdjunto> soportes = soporteRepository.findByActividadIdAndActivoTrue(act.getId());
            sb.append(alt ? "<tr style=\"background:#f7f7f7\">" : "<tr>");

            String descObl = act.getObligacion() != null ? safe(act.getObligacion().getDescripcion()) : "";
            sb.append("<td>").append(orden).append(". ").append(esc(descObl)).append("</td>");

            sb.append("<td>");
            String   desc   = safe(act.getDescripcion());
            String[] lineas = desc.split("\\n");
            if (lineas.length > 1) {
                sb.append("<ul class=\"bullet-list\">");
                for (String linea : lineas) {
                    String trimmed = linea.trim();
                    if (!trimmed.isEmpty()) {
                        sb.append("<li>").append(esc(trimmed)).append("</li>");
                    }
                }
                sb.append("</ul>");
            } else {
                sb.append("&#8226; ").append(esc(desc));
            }
            sb.append("</td>");

            sb.append("<td>");
            if (soportes.isEmpty()) {
                sb.append("&#8212;");
            } else {
                for (SoporteAdjunto s : soportes) {
                    if (s.getTipo() == co.gov.bogota.sed.sigcon.domain.enums.TipoSoporte.URL
                            && notEmpty(s.getReferencia())) {
                        sb.append("<div><a href=\"").append(esc(s.getReferencia())).append("\"")
                          .append(" style=\"color:#0a0e5a;text-decoration:underline;\">")
                          .append(esc(s.getNombre())).append("</a></div>");
                    } else {
                        sb.append("<div>").append(esc(s.getNombre())).append("</div>");
                    }
                }
            }
            sb.append("</td>");

            sb.append("</tr>");
            alt = !alt;
            orden++;
        }
        sb.append("</tbody></table>");
    }

    private void appendSeccion3(StringBuilder sb, Informe informe, List<AporteSgssi> aportes) {
        sb.append("<div class=\"sec-title\">3. RELACI&#211;N DEL PAGO DE APORTES AL SISTEMA DE SEGURIDAD SOCIAL INTEGRAL</div>");
        sb.append("<table><thead><tr>");
        sb.append("<th>ITEM</th>");
        sb.append("<th>PER&#205;ODO PAGO<br/>(mm/aaaa)</th>");
        sb.append("<th>FECHA DE PAGO<br/>(dd/mm/aaaa)</th>");
        sb.append("<th>VALOR APORTADO<br/>(Sobre el 40% del ingreso mensual)</th>");
        sb.append("<th>ENTIDAD</th>");
        sb.append("</tr></thead><tbody>");

        YearMonth periodo    = YearMonth.from(informe.getFechaInicio()).minusMonths(1);
        String    periodoStr = periodo.format(DateTimeFormatter.ofPattern("MM/yyyy"));

        boolean alt = false;
        for (AporteSgssi aporte : aportes) {
            sb.append(alt ? "<tr style=\"background:#f7f7f7\">" : "<tr>");
            sb.append("<td><b>").append(esc(labelSgssi(aporte.getItem()))).append("</b></td>");
            sb.append("<td>").append(periodoStr).append("</td>");
            sb.append("<td>").append(aporte.getFechaPago() != null ? aporte.getFechaPago().format(DATE_FMT) : "").append("</td>");
            sb.append("<td>").append(formatPesos(aporte.getValorAportado())).append("</td>");
            sb.append("<td>").append(esc(safe(aporte.getEntidad()))).append("</td>");
            sb.append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");
    }

    private void appendSeccion4(StringBuilder sb, Informe informe) {
        boolean pendiente = informe.getCorrespondenciaPendiente() != null
                         && informe.getCorrespondenciaPendiente() > 0;
        String marcaSi = pendiente ? "<u>SI<b>_X_</b></u>" : "<u>SI<b>___</b></u>";
        String marcaNo = pendiente ? "<u>NO<b>___</b></u>" : "<u>NO<b>_X_</b></u>";

        sb.append("<div class=\"sec-title\">4. ESTADO RADICACI&#211;N DE LA CORRESPONDENCIA</div>");
        sb.append("<table><tbody><tr><td style=\"border:0.5pt solid #999;padding:5pt 6pt;\">");

        sb.append("<p class=\"parr\">Una vez revisado el aplicativo de seguimiento de la correspondencia")
          .append(" a cargo del contratista, se identific&#243; que ")
          .append(marcaSi).append(" &nbsp; ").append(marcaNo)
          .append(" se encuentran radicados pendientes a la fecha, para el per&#237;odo objeto")
          .append(" del presente informe.</p>");

        sb.append("<p class=\"parr\">La anterior informaci&#243;n corresponde a la verificaci&#243;n")
          .append(" realizada por el responsable del manejo de la correspondencia en el &#225;rea,")
          .append(" remitida por correo electr&#243;nico, el cual se adjunta al presente en")
          .append(" <b>01</b> folios.</p>");

        sb.append("</td></tr></tbody></table>");
    }

    private void appendSeccion5(StringBuilder sb, Informe informe) {
        sb.append("<div class=\"sec-title\">5. DECLARACI&#211;N ESPECIAL</div>");
        sb.append("<table><tbody><tr><td style=\"border:0.5pt solid #999;padding:5pt 6pt;\">");

        sb.append("<p class=\"parr\" style=\"text-align:justify\">")
          .append("El contratista declara que toda la informaci&#243;n relacionada en el presente informe")
          .append(" corresponde fidedignamente a todas las actividades ejecutadas dentro del respectivo")
          .append(" periodo, as&#237; como los pagos efectuados en el marco del Sistema General de")
          .append(" Seguridad Social Integral &#8211; SGSSI. Esta declaraci&#243;n se realiza bajo la")
          .append(" responsabilidad del contratista.")
          .append("</p>");

        String numDesembolso = informe.getNumeroDesembolso() != null
            ? String.valueOf(informe.getNumeroDesembolso()) : "&#8212;";
        String valDesembolso = informe.getValorDesembolso() != null
            ? formatPesos(informe.getValorDesembolso()) : "&#8212;";
        String pctEjecucion  = informe.getPorcentajeEjecucion() != null
            ? informe.getPorcentajeEjecucion().setScale(2, java.math.RoundingMode.HALF_UP)
                     .toPlainString() + "%" : "&#8212;";

        sb.append("<p class=\"parr\" style=\"text-align:justify\">")
          .append("La supervisi&#243;n verific&#243; el cumplimiento de las actividades a cargo del")
          .append(" contratista, en virtud de lo cual se establece la procedencia de la autorizaci&#243;n")
          .append(" del pago/ desembolso No.<b>").append(numDesembolso).append("</b>")
          .append(", el cual corresponde a <b>").append(valDesembolso).append("</b> m/cte.")
          .append(" A la fecha el porcentaje de ejecuci&#243;n es: <b>").append(pctEjecucion).append("</b>")
          .append("</p>");

        String fechaElab = informe.getFechaElaboracion() != null
            ? informe.getFechaElaboracion().format(DATE_FMT) : "N/A";
        sb.append("<p class=\"parr\">Fecha de elaboraci&#243;n: <b>").append(fechaElab).append("</b></p>");

        sb.append("</td></tr></tbody></table>");
    }

    // ─── Firmas ───────────────────────────────────────────────────────────────

    private void appendFirmas(StringBuilder sb, Informe informe,
                               Usuario contratista, Usuario supervisor, Usuario revisor,
                               byte[] firmaContratista, byte[] firmaSupervisor, byte[] firmaRevisor) {

        LocalDate fe        = informe.getFechaElaboracion() != null ? informe.getFechaElaboracion() : LocalDate.now();
        String    diaNro    = nombreDia(fe);
        String    mesNombre = fe.getMonth().getDisplayName(TextStyle.FULL, LOCALE_CO).toUpperCase();
        int       anio      = fe.getYear();

        sb.append("<div class=\"firma-section\">");
        sb.append("<p class=\"parr\">Para constancia se firma por quienes en ella intervinieron al")
          .append(" <b>").append(diaNro).append("</b> d&#237;as")
          .append(" del mes de <b>").append(mesNombre).append("</b> de <b>").append(anio).append("</b>")
          .append("</p>");

        sb.append("<table class=\"firma-table\"><tbody><tr>");
        appendFirmaCell(sb, firmaContratista, contratista.getNombre(), safe(contratista.getCargo()), "Contratista", "50%");
        if (supervisor != null) {
            appendFirmaCell(sb, firmaSupervisor,
                "Vo. Bo " + supervisor.getNombre(),
                safe(supervisor.getCargo()), "Supervisor(a)", "50%");
        }
        sb.append("</tr></tbody></table>");

        if (revisor != null) {
            sb.append("<table class=\"firma-table\"><tbody><tr>");
            sb.append("<td class=\"firma-cell-full\">");
            String b64 = toBase64(firmaRevisor);
            if (b64 != null) {
                sb.append("<div class=\"firma-img-wrap\">");
                sb.append("<img src=\"data:image/png;base64,").append(b64)
                  .append("\" class=\"firma-img\" style=\"height:44pt;width:150pt;\" alt=\"Firma revisor\"/>");
                sb.append("</div>");
            } else {
                sb.append("<div class=\"firma-img-wrap\"></div>");
            }
            sb.append("<div class=\"firma-line\"></div>");
            sb.append("<div class=\"firma-name\">Revis&#243;: ").append(esc(revisor.getNombre())).append("</div>");
            if (notEmpty(revisor.getCargo())) {
                sb.append("<div class=\"firma-cargo\">").append(esc(revisor.getCargo())).append("</div>");
            }
            sb.append("<div class=\"firma-rol\">Apoyo a la Supervisi&#243;n</div>");
            sb.append("</td>");
            sb.append("</tr></tbody></table>");
        }

        sb.append("</div>");
    }

    private static void appendFirmaCell(StringBuilder sb, byte[] firma,
                                         String nombre, String cargo, String rol, String width) {
        sb.append("<td class=\"firma-cell\" style=\"width:").append(width).append("\">");
        String b64 = toBase64(firma);
        if (b64 != null) {
            sb.append("<div class=\"firma-img-wrap\">");
            sb.append("<img src=\"data:image/png;base64,").append(b64)
              .append("\" class=\"firma-img\" style=\"height:44pt;width:150pt;\" alt=\"Firma\"/>");
            sb.append("</div>");
        } else {
            sb.append("<div class=\"firma-img-wrap\"></div>");
        }
        sb.append("<div class=\"firma-line\"></div>");
        sb.append("<div class=\"firma-name\">").append(esc(nombre)).append("</div>");
        if (notEmpty(cargo)) {
            sb.append("<div class=\"firma-cargo\">").append(esc(cargo)).append("</div>");
        }
        sb.append("<div class=\"firma-rol\">").append(esc(rol)).append("</div>");
        sb.append("</td>");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void fila4(StringBuilder sb, String label1, String value1,
                                String label2, String value2, boolean alt) {
        sb.append(alt ? "<tr style=\"background:#f7f7f7\">" : "<tr>");
        sb.append("<td class=\"lbl4\" style=\"width:22%\">").append(label1).append("</td>");
        sb.append("<td class=\"val4\" style=\"width:25%\">").append(value1).append("</td>");
        sb.append("<td class=\"lbl4\" style=\"width:28%\">").append(label2).append("</td>");
        sb.append("<td class=\"val4\" style=\"width:25%\">").append(value2).append("</td>");
        sb.append("</tr>");
    }

    private static void fila2(StringBuilder sb, String label, String value, boolean alt) {
        sb.append(alt ? "<tr style=\"background:#f7f7f7\">" : "<tr>");
        sb.append("<td class=\"lbl\">").append(label).append("</td>");
        sb.append("<td class=\"val\">").append(value).append("</td>");
        sb.append("</tr>");
    }

    private static String nombreDia(LocalDate fecha) {
        return String.valueOf(fecha.getDayOfMonth());
    }

    private static String labelSgssi(ItemSgssi item) {
        if (item == null) return "";
        switch (item) {
            case SALUD:   return "SALUD";
            case PENSION: return "PENSI&#211;N";
            case ARL:     return "ARL";
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
