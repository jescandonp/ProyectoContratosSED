package co.gov.bogota.sed.sigcon.web;

import co.gov.bogota.sed.sigcon.SigconBackendApplication;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
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
import co.gov.bogota.sed.sigcon.application.service.ObligacionService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.application.service.ParametroService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
import co.gov.bogota.sed.sigcon.application.service.SoporteAdjuntoService;
import co.gov.bogota.sed.sigcon.application.service.UsuarioService;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T7 — I9: Tests de endpoints VB en InformeController.
 */
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
class InformeControllerVbTest {

    private static final String ADMIN_EMAIL          = "administrativo@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL     = "supervisor1@educacionbogota.edu.co";

    @Autowired private MockMvc mockMvc;

    @MockBean private InformeService informeService;
    @MockBean private InformeEstadoService informeEstadoService;
    @MockBean private ContratoService contratoService;
    @MockBean private UsuarioService usuarioService;
    @MockBean private DocumentoCatalogoService documentoCatalogoService;
    @MockBean private ObligacionService obligacionService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private ObservacionService observacionService;
    @MockBean private DocumentStorageService documentStorageService;
    @MockBean private ActividadInformeService actividadInformeService;
    @MockBean private SoporteAdjuntoService soporteAdjuntoService;
    @MockBean private DocumentoAdicionalInformeService documentoAdicionalInformeService;
    @MockBean private PdfInformeService pdfInformeService;
    @MockBean private InformePdfTemplateService informePdfTemplateService;
    @MockBean private NotificacionService notificacionService;
    @MockBean private EventoInformeService eventoInformeService;
    @MockBean private EmailNotificacionService emailNotificacionService;
    @MockBean private co.gov.bogota.sed.sigcon.application.service.AporteSgssiService aporteSgssiService;
    @MockBean private co.gov.bogota.sed.sigcon.application.service.DocumentoRequeridoInformeService documentoRequeridoInformeService;
    @MockBean private co.gov.bogota.sed.sigcon.application.service.BusquedaAdminService busquedaAdminService;
    @MockBean private co.gov.bogota.sed.sigcon.domain.repository.SgcnParametroRepository sgcnParametroRepository;
    @MockBean private ParametroService parametroService;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void colaVistoBueno_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/informes/cola/visto-bueno"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void colaVistoBueno_rolSupervisor_retorna403() throws Exception {
        mockMvc.perform(get("/api/informes/cola/visto-bueno")
                .with(httpBasic(SUPERVISOR_EMAIL, "supervisor123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void colaVistoBueno_rolAdministrativo_retorna200() throws Exception {
        InformeResumenDto resumen = informeResumen();
        when(informeService.listarColaVistoBueno(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(resumen)));

        mockMvc.perform(get("/api/informes/cola/visto-bueno")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void darVistosBueno_estadoCorrecto_retorna200() throws Exception {
        when(informeEstadoService.darVistosBueno(eq(10L), any()))
            .thenReturn(informeDetalle());

        mockMvc.perform(post("/api/informes/10/dar-visto-bueno")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void escalar_retorna200() throws Exception {
        when(informeEstadoService.escalar(eq(10L), any()))
            .thenReturn(informeDetalle());

        mockMvc.perform(post("/api/informes/10/escalar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Requiere revision del supervisor\"}")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static InformeResumenDto informeResumen() {
        InformeResumenDto dto = new InformeResumenDto();
        dto.setId(10L);
        dto.setNumero(1);
        dto.setContratoId(20L);
        dto.setContratoNumero("OPS-2026-020");
        dto.setFechaInicio(LocalDate.of(2026, 2, 1));
        dto.setFechaFin(LocalDate.of(2026, 2, 28));
        dto.setEstado(EstadoInforme.EN_VISTO_BUENO);
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
        dto.setEstado(EstadoInforme.EN_REVISION);
        return dto;
    }
}
