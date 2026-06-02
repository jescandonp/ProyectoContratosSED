package co.gov.bogota.sed.sigcon.web.controller;

import co.gov.bogota.sed.sigcon.application.service.InformePdfTemplateService;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.DocumentoAdicional;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Endpoint de previsualización del PDF de informe para entorno local-dev.
 * Genera un PDF con datos mock sin requerir DB, firmas reales ni flujo de aprobación.
 * Solo disponible con perfil local-dev — no se despliega en producción.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("local-dev")
@Tag(name = "DEV", description = "Herramientas de desarrollo — no disponibles en producción")
public class InformePdfPreviewController {

    private final InformePdfTemplateService templateService;

    public InformePdfPreviewController(InformePdfTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Genera y devuelve un PDF de informe con datos de ejemplo.
     * Acceso: GET /api/dev/pdf-preview
     * No requiere autenticación en perfil local-dev (DevSecurityConfig permite /api/dev/**).
     */
    @GetMapping(value = "/pdf-preview", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "[DEV] Previsualiza el PDF del informe con datos mock")
    public ResponseEntity<ByteArrayResource> previewPdf() throws Exception {

        Informe informe = buildInformeMock();

        List<ActividadInforme>   actividades = buildActividadesMock(informe);
        List<DocumentoAdicional> documentos  = Collections.emptyList();
        List<AporteSgssi>        aportes     = buildAportesMock(informe);

        byte[] firmaPlaceholder = buildFirmaPlaceholder();

        byte[] pdfBytes = templateService.generarPdf(
                informe, actividades, documentos, aportes,
                firmaPlaceholder, firmaPlaceholder, null);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview-informe.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdfBytes));
    }

    // ─── Builders de datos mock ───────────────────────────────────────────────

    private static Informe buildInformeMock() {
        Usuario contratista = usuario(1L, "Juan Pérez Gómez",
                "Profesional de apoyo", RolUsuario.CONTRATISTA);
        Usuario supervisor  = usuario(2L, "María López Castro",
                "Coordinadora de Área", RolUsuario.SUPERVISOR);

        Contrato contrato = new Contrato();
        contrato.setId(1L);
        contrato.setNumero("OPS-2026-0042");
        contrato.setObjeto("Prestar servicios profesionales para apoyar los procesos de " +
                "gestión de contratos en la Secretaría de Educación del Distrito, " +
                "en el marco del proyecto SIGCON.");
        contrato.setTipo(TipoContrato.OPS);
        contrato.setValorTotal(new BigDecimal("24000000.00"));
        contrato.setFechaInicio(LocalDate.of(2026, 1, 15));
        contrato.setFechaFin(LocalDate.of(2026, 12, 31));
        contrato.setEstado(EstadoContrato.EN_EJECUCION);
        contrato.setFormaPago("Pagos mensuales previa presentación y aprobación de informe de actividades");
        contrato.setDependencia("Dirección de Contratación — Secretaría de Educación del Distrito");
        contrato.setModificaciones("No se han presentado modificaciones al contrato.");
        contrato.setContratista(contratista);
        contrato.setSupervisor(supervisor);
        contrato.setActivo(true);

        Informe informe = new Informe();
        informe.setId(99L);
        informe.setNumero(3);
        informe.setContrato(contrato);
        informe.setFechaInicio(LocalDate.of(2026, 4, 1));
        informe.setFechaFin(LocalDate.of(2026, 4, 30));
        informe.setFechaElaboracion(LocalDate.of(2026, 5, 5));
        informe.setEstado(EstadoInforme.APROBADO);
        informe.setNumeroDesembolso(3);
        informe.setValorDesembolso(new BigDecimal("2000000.00"));
        informe.setPorcentajeEjecucion(new BigDecimal("25.00"));
        informe.setCorrespondenciaPendiente(0);
        informe.setActivo(true);
        return informe;
    }

    private static List<ActividadInforme> buildActividadesMock(Informe informe) {
        Obligacion obl1 = new Obligacion();
        obl1.setId(1L);
        obl1.setDescripcion("Apoyar el análisis, diseño e implementación de los módulos del sistema SIGCON.");

        Obligacion obl2 = new Obligacion();
        obl2.setId(2L);
        obl2.setDescripcion("Elaborar documentación técnica y funcional de los procesos automatizados.");

        ActividadInforme a1 = new ActividadInforme();
        a1.setId(101L);
        a1.setInforme(informe);
        a1.setObligacion(obl1);
        a1.setDescripcion("Implementación del módulo de gestión de informes de actividades.\n" +
                "Corrección de validaciones de porcentaje de ejecución por rol y estado.\n" +
                "Desarrollo de la vista Visto Bueno con panel de acciones.");
        a1.setPorcentaje(new BigDecimal("60.00"));
        a1.setActivo(true);

        ActividadInforme a2 = new ActividadInforme();
        a2.setId(102L);
        a2.setInforme(informe);
        a2.setObligacion(obl2);
        a2.setDescripcion("Redacción de especificaciones funcionales I14.\n" +
                "Actualización del plan de implementación con evidencias de ejecución.");
        a2.setPorcentaje(new BigDecimal("40.00"));
        a2.setActivo(true);

        return Arrays.asList(a1, a2);
    }

    private static List<AporteSgssi> buildAportesMock(Informe informe) {
        AporteSgssi salud = new AporteSgssi();
        salud.setId(201L);
        salud.setInforme(informe);
        salud.setItem(ItemSgssi.SALUD);
        salud.setFechaPago(LocalDate.of(2026, 3, 28));
        salud.setValorAportado(new BigDecimal("320000.00"));
        salud.setEntidad("Nueva EPS");
        salud.setActivo(true);

        AporteSgssi pension = new AporteSgssi();
        pension.setId(202L);
        pension.setInforme(informe);
        pension.setItem(ItemSgssi.PENSION);
        pension.setFechaPago(LocalDate.of(2026, 3, 28));
        pension.setValorAportado(new BigDecimal("320000.00"));
        pension.setEntidad("Colpensiones");
        pension.setActivo(true);

        AporteSgssi arl = new AporteSgssi();
        arl.setId(203L);
        arl.setInforme(informe);
        arl.setItem(ItemSgssi.ARL);
        arl.setFechaPago(LocalDate.of(2026, 3, 28));
        arl.setValorAportado(new BigDecimal("40000.00"));
        arl.setEntidad("Positiva ARL");
        arl.setActivo(true);

        return Arrays.asList(salud, pension, arl);
    }

    private static Usuario usuario(Long id, String nombre, String cargo, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setCargo(cargo);
        u.setRol(rol);
        u.setEmail("preview" + id + "@educacionbogota.edu.co");
        u.setActivo(true);
        return u;
    }

    /**
     * Imagen PNG mínima 1x1 px para usar como placeholder de firma.
     * Evita NPE en el renderizador sin necesitar archivos en disco.
     */
    private static byte[] buildFirmaPlaceholder() {
        // PNG 1x1 px transparente (bytes canónicos)
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
            (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte)0x9C, 0x62, 0x00, 0x00, 0x00, 0x02,
            0x00, 0x01, (byte)0xE5, 0x27, (byte)0xDE, (byte)0xFC, 0x00, 0x00,
            0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42,
            0x60, (byte)0x82
        };
    }
}
