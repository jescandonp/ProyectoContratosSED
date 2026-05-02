package co.gov.bogota.sed.sigcon.web;

import co.gov.bogota.sed.sigcon.SigconBackendApplication;
import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.DocumentoAdicionalDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteAdjuntoDto;
import co.gov.bogota.sed.sigcon.application.dto.notificacion.NotificacionesCountDto;
import co.gov.bogota.sed.sigcon.application.service.ActividadInformeService;
import co.gov.bogota.sed.sigcon.application.service.ContratoService;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentoAdicionalInformeService;
import co.gov.bogota.sed.sigcon.application.service.DocumentoCatalogoService;
import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.EmailNotificacionService;
import co.gov.bogota.sed.sigcon.application.service.EventoInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformePdfTemplateService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.NotificacionService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
import co.gov.bogota.sed.sigcon.application.service.ObligacionService;
import co.gov.bogota.sed.sigcon.application.service.SoporteAdjuntoService;
import co.gov.bogota.sed.sigcon.application.service.UsuarioService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SigconBackendApplication.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "sigcon.storage.signatures-path=${java.io.tmpdir}/sigcon-test"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("local-dev")
class InformeSecurityTest {

    private static final String ADMIN_EMAIL = "admin@educacionbogota.edu.co";
    private static final String CONTRACTOR_EMAIL = "juan.escandon@educacionbogota.edu.co";
    private static final String REVIEWER_EMAIL = "revisor1@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL = "supervisor1@educacionbogota.edu.co";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InformeService informeService;

    @MockBean
    private InformeEstadoService informeEstadoService;

    @MockBean
    private ContratoService contratoService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private DocumentoCatalogoService documentoCatalogoService;

    @MockBean
    private ObligacionService obligacionService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ObservacionService observacionService;

    @MockBean
    private DocumentStorageService documentStorageService;

    @MockBean
    private ActividadInformeService actividadInformeService;

    @MockBean
    private SoporteAdjuntoService soporteAdjuntoService;

    @MockBean
    private DocumentoAdicionalInformeService documentoAdicionalInformeService;

    // I3 beans — mocked to keep MockMvc context loadable without Oracle
    @MockBean
    private PdfInformeService pdfInformeService;

    @MockBean
    private InformePdfTemplateService informePdfTemplateService;

    @MockBean
    private NotificacionService notificacionService;

    @MockBean
    private EventoInformeService eventoInformeService;

    @MockBean
    private EmailNotificacionService emailNotificacionService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void informesAndActividadSupportEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/informes"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/actividades/11/soportes/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSoporteUrlJson()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reviewerAndSupervisorCannotCreateInformes() throws Exception {
        mockMvc.perform(post("/api/informes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInformeJson())
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/informes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInformeJson())
                .with(httpBasic(SUPERVISOR_EMAIL, "supervisor123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void contractorCannotApproveInforme() throws Exception {
        mockMvc.perform(post("/api/informes/10/aprobar")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidInformeTransitionReturnsConflict() throws Exception {
        when(informeEstadoService.aprobarRevision(eq(10L), eq(REVIEWER_EMAIL), any()))
            .thenThrow(new SigconBusinessException(
                ErrorCode.TRANSICION_INVALIDA,
                "Transición de estado inválida",
                HttpStatus.CONFLICT
            ));

        mockMvc.perform(post("/api/informes/10/aprobar-revision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Listo para supervisión\"}")
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("TRANSICION_INVALIDA"));
    }

    @Test
    void contractorCanCreateEditSubmitAndAttachInformeContent() throws Exception {
        when(informeService.listarMisInformes(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(informeResumen())));
        when(informeService.crearInforme(any())).thenReturn(informeDetalle());
        when(informeService.actualizarInforme(eq(10L), any())).thenReturn(informeDetalle());
        when(informeEstadoService.enviar(eq(10L), eq(CONTRACTOR_EMAIL))).thenReturn(informeDetalle());
        when(actividadInformeService.crear(eq(10L), any())).thenReturn(actividadDto());
        when(actividadInformeService.actualizar(eq(10L), eq(11L), any())).thenReturn(actividadDto());
        when(soporteAdjuntoService.agregarSoporteUrl(eq(11L), any())).thenReturn(soporteDto());
        when(soporteAdjuntoService.agregarSoporteArchivo(eq(11L), any())).thenReturn(soporteDto());
        when(documentoAdicionalInformeService.agregar(eq(10L), any())).thenReturn(documentoDto());

        mockMvc.perform(get("/api/informes")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(post("/api/informes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInformeJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(put("/api/informes/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validInformeJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/informes/10/enviar")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/informes/10/actividades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validActividadJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(11));

        mockMvc.perform(put("/api/informes/10/actividades/11")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validActividadJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(11));

        mockMvc.perform(post("/api/actividades/11/soportes/url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSoporteUrlJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(12));

        MockMultipartFile file = new MockMultipartFile("file", "evidencia.pdf", "application/pdf", new byte[] { 1 });
        mockMvc.perform(multipart("/api/actividades/11/soportes/archivo")
                .file(file)
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(12));

        mockMvc.perform(post("/api/informes/10/documentos-adicionales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validDocumentoJson())
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(13));
    }

    @Test
    void reviewerAndSupervisorCanExecuteTheirWorkflowTransitions() throws Exception {
        when(informeService.listarParaRevisor(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(informeResumen())));
        when(informeService.listarParaSupervisor(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(informeResumen())));
        when(informeEstadoService.aprobarRevision(eq(10L), eq(REVIEWER_EMAIL), any())).thenReturn(informeDetalle());
        when(informeEstadoService.devolverRevision(eq(10L), eq(REVIEWER_EMAIL), eq("Ajustar soporte")))
            .thenReturn(informeDetalle());
        when(informeEstadoService.aprobar(eq(10L), eq(SUPERVISOR_EMAIL))).thenReturn(informeDetalle());
        when(informeEstadoService.devolver(eq(10L), eq(SUPERVISOR_EMAIL), eq("Ajustar actividad")))
            .thenReturn(informeDetalle());

        mockMvc.perform(get("/api/informes")
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(post("/api/informes/10/aprobar-revision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Listo para supervisión\"}")
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/informes/10/devolver-revision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Ajustar soporte\"}")
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(get("/api/informes")
                .with(httpBasic(SUPERVISOR_EMAIL, "supervisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(post("/api/informes/10/aprobar")
                .with(httpBasic(SUPERVISOR_EMAIL, "supervisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/informes/10/devolver")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Ajustar actividad\"}")
                .with(httpBasic(SUPERVISOR_EMAIL, "supervisor123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void contractorCanDeleteEditableInformeChildren() throws Exception {
        mockMvc.perform(delete("/api/informes/10/actividades/11")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/actividades/11/soportes/12")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/informes/10/documentos-adicionales/13")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isNoContent());
    }

    @Test
    void adminCanReadInformeDetailForHistoricalAccess() throws Exception {
        when(informeService.obtenerDetalle(10L)).thenReturn(informeDetalle());

        mockMvc.perform(get("/api/informes/10")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void reviewerCannotDownloadApprovedPdf() throws Exception {
        mockMvc.perform(get("/api/informes/10/pdf")
                .with(httpBasic(REVIEWER_EMAIL, "revisor123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void contractorCannotDownloadPdfWhenInformeIsNotAssigned() throws Exception {
        when(informeService.obtenerInformeAutorizado(10L))
            .thenThrow(new SigconBusinessException(
                ErrorCode.ACCESO_DENEGADO,
                "Acceso denegado",
                HttpStatus.FORBIDDEN
            ));
        when(informeService.findActiveInforme(10L)).thenReturn(informeEntity());
        when(pdfInformeService.cargarPdf(any(Informe.class))).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));

        mockMvc.perform(get("/api/informes/10/pdf")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void notificationEndpointsUseI3ContractAndRequireAuthentication() throws Exception {
        when(notificacionService.listar(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(notificacionService.contarNoLeidas()).thenReturn(new NotificacionesCountDto(2L));

        mockMvc.perform(get("/api/notificaciones"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notificaciones")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notificaciones/no-leidas/count")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(patch("/api/notificaciones/1/leida")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/notificaciones/leidas")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isNoContent());
    }

    private static InformeResumenDto informeResumen() {
        InformeResumenDto dto = new InformeResumenDto();
        dto.setId(10L);
        dto.setNumero(1);
        dto.setContratoId(20L);
        dto.setContratoNumero("OPS-2026-020");
        dto.setFechaInicio(LocalDate.of(2026, 2, 1));
        dto.setFechaFin(LocalDate.of(2026, 2, 28));
        dto.setEstado(EstadoInforme.BORRADOR);
        return dto;
    }

    private static InformeDetalleDto informeDetalle() {
        InformeDetalleDto dto = new InformeDetalleDto();
        dto.setId(10L);
        dto.setNumero(1);
        dto.setContratoId(20L);
        dto.setContratoNumero("OPS-2026-020");
        dto.setFechaInicio(LocalDate.of(2026, 2, 1));
        dto.setFechaFin(LocalDate.of(2026, 2, 28));
        dto.setEstado(EstadoInforme.BORRADOR);
        return dto;
    }

    private static Informe informeEntity() {
        Contrato contrato = new Contrato();
        contrato.setId(20L);
        contrato.setNumero("OPS-2026-020");

        Informe informe = new Informe();
        informe.setId(10L);
        informe.setNumero(1);
        informe.setContrato(contrato);
        informe.setEstado(EstadoInforme.APROBADO);
        informe.setPdfRuta("pdfs/20/10/informe-1.pdf");
        informe.setActivo(true);
        return informe;
    }

    private static ActividadInformeDto actividadDto() {
        ActividadInformeDto dto = new ActividadInformeDto();
        dto.setId(11L);
        dto.setIdObligacion(21L);
        dto.setDescripcion("Actividad ejecutada");
        return dto;
    }

    private static SoporteAdjuntoDto soporteDto() {
        SoporteAdjuntoDto dto = new SoporteAdjuntoDto();
        dto.setId(12L);
        dto.setNombre("Evidencia");
        dto.setReferencia("https://sed.gov.co/evidencia");
        return dto;
    }

    private static DocumentoAdicionalDto documentoDto() {
        DocumentoAdicionalDto dto = new DocumentoAdicionalDto();
        dto.setId(13L);
        dto.setIdCatalogo(30L);
        dto.setNombreCatalogo("Certificación");
        dto.setReferencia("Radicado 123");
        return dto;
    }

    private static String validInformeJson() {
        return "{"
            + "\"idContrato\":20,"
            + "\"fechaInicio\":\"2026-02-01\","
            + "\"fechaFin\":\"2026-02-28\""
            + "}";
    }

    private static String validActividadJson() {
        return "{"
            + "\"idObligacion\":21,"
            + "\"descripcion\":\"Actividad ejecutada\","
            + "\"porcentaje\":50"
            + "}";
    }

    private static String validSoporteUrlJson() {
        return "{"
            + "\"nombre\":\"Evidencia\","
            + "\"url\":\"https://sed.gov.co/evidencia\""
            + "}";
    }

    private static String validDocumentoJson() {
        return "{"
            + "\"idCatalogo\":30,"
            + "\"referencia\":\"Radicado 123\""
            + "}";
    }
}
