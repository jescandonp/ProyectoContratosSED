package co.gov.bogota.sed.sigcon.web;

import co.gov.bogota.sed.sigcon.SigconBackendApplication;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T7 — I9: Tests de ParametroController.
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
class ParametroControllerTest {

    private static final String ADMIN_EMAIL          = "admin@educacionbogota.edu.co";
    private static final String ADMINISTRATIVO_EMAIL = "administrativo@educacionbogota.edu.co";

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
    void getParametros_rolAdmin_retorna200() throws Exception {
        when(parametroService.isVbActivo()).thenReturn(true);

        mockMvc.perform(get("/api/admin/parametros")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void getParametros_rolAdministrativo_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/parametros")
                .with(httpBasic(ADMINISTRATIVO_EMAIL, "admin123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void putVbActivo_desactivar_retorna200YMigraEstados() throws Exception {
        when(parametroService.isVbActivo()).thenReturn(false);

        mockMvc.perform(put("/api/admin/parametros/vb-activo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        verify(parametroService).setVbActivo(false);
    }

    @Test
    void getCargaInformes_rolAdmin_retorna200() throws Exception {
        when(parametroService.isCargaInformesActiva()).thenReturn(true);

        mockMvc.perform(get("/api/admin/parametros/carga-informes")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void putCargaInformes_desactivar_notificaBloqueoMasivo() throws Exception {
        when(parametroService.setCargaInformesActiva(false)).thenReturn(true);
        when(parametroService.isCargaInformesActiva()).thenReturn(false);

        mockMvc.perform(put("/api/admin/parametros/carga-informes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        verify(notificacionService).notificarBloqueoMasivo();
    }

    @Test
    void putCargaInformes_activar_noNotificaBloqueoMasivo() throws Exception {
        when(parametroService.setCargaInformesActiva(true)).thenReturn(false);
        when(parametroService.isCargaInformesActiva()).thenReturn(true);

        mockMvc.perform(put("/api/admin/parametros/carga-informes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));

        verify(notificacionService, never()).notificarBloqueoMasivo();
    }
}
