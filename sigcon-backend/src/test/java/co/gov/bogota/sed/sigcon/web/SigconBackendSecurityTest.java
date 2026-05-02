package co.gov.bogota.sed.sigcon.web;

import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.SigconBackendApplication;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.NotificacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SigconBackendSecurityTest {

    private static final String ADMIN_EMAIL = "admin@educacionbogota.edu.co";
    private static final String CONTRACTOR_EMAIL = "juan.escandon@educacionbogota.edu.co";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ContratoRepository contratoRepository;

    @MockBean
    private ObligacionRepository obligacionRepository;

    @MockBean
    private DocumentoCatalogoRepository documentoCatalogoRepository;

    // I2 repositories — mocked to keep the I1 security context loadable without Oracle.
    @MockBean
    private InformeRepository informeRepository;

    @MockBean
    private ActividadInformeRepository actividadInformeRepository;

    @MockBean
    private SoporteAdjuntoRepository soporteAdjuntoRepository;

    @MockBean
    private DocumentoAdicionalRepository documentoAdicionalRepository;

    @MockBean
    private ObservacionRepository observacionRepository;

    // I3 repositories — mocked to keep context loadable without Oracle.
    @MockBean
    private NotificacionRepository notificacionRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void contractorListsOnlyOwnContracts() throws Exception {
        Usuario contractor = usuario(2L, CONTRACTOR_EMAIL, RolUsuario.CONTRATISTA);
        Contrato contract = contrato(10L, "OPS-2026-010", contractor);
        when(usuarioRepository.findByEmailAndActivoTrue(CONTRACTOR_EMAIL)).thenReturn(Optional.of(contractor));
        when(contratoRepository.findByContratistaAndActivoTrue(eq(contractor), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(contract)));

        mockMvc.perform(get("/api/contratos")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].numero").value("OPS-2026-010"));
    }

    @Test
    void adminListsAllContracts() throws Exception {
        Usuario admin = usuario(1L, ADMIN_EMAIL, RolUsuario.ADMIN);
        Usuario contractor = usuario(2L, CONTRACTOR_EMAIL, RolUsuario.CONTRATISTA);
        when(usuarioRepository.findByEmailAndActivoTrue(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(contratoRepository.findByActivoTrue(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(contrato(20L, "OPS-2026-020", contractor))));

        mockMvc.perform(get("/api/contratos")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].numero").value("OPS-2026-020"));
    }

    @Test
    void nonAdminCannotCreateContract() throws Exception {
        mockMvc.perform(post("/api/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validContractJson("OPS-2026-030"))
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isForbidden());
    }

    @Test
    void duplicateContractNumberReturnsConflict() throws Exception {
        Usuario contractor = usuario(2L, CONTRACTOR_EMAIL, RolUsuario.CONTRATISTA);
        when(contratoRepository.findByNumeroAndActivoTrue("OPS-2026-040"))
            .thenReturn(Optional.of(contrato(40L, "OPS-2026-040", contractor)));

        mockMvc.perform(post("/api/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validContractJson("OPS-2026-040"))
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("NUMERO_CONTRATO_DUPLICADO"));
    }

    @Test
    void contractorCannotOpenForeignContract() throws Exception {
        Usuario contractor = usuario(2L, CONTRACTOR_EMAIL, RolUsuario.CONTRATISTA);
        Usuario foreignContractor = usuario(99L, "otro@educacionbogota.edu.co", RolUsuario.CONTRATISTA);
        when(usuarioRepository.findByEmailAndActivoTrue(CONTRACTOR_EMAIL)).thenReturn(Optional.of(contractor));
        when(contratoRepository.findByIdAndActivoTrue(50L))
            .thenReturn(Optional.of(contrato(50L, "OPS-2026-050", foreignContractor)));

        mockMvc.perform(get("/api/contratos/50")
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    void signatureUploadAcceptsPngAndRejectsPdf() throws Exception {
        Usuario contractor = usuario(2L, CONTRACTOR_EMAIL, RolUsuario.CONTRATISTA);
        when(usuarioRepository.findByEmailAndActivoTrue(CONTRACTOR_EMAIL)).thenReturn(Optional.of(contractor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile png = new MockMultipartFile("file", "firma.png", "image/png", new byte[] { 1, 2, 3 });
        mockMvc.perform(multipart("/api/usuarios/me/firma")
                .file(png)
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firmaImagen").exists());

        MockMultipartFile pdf = new MockMultipartFile("file", "firma.pdf", "application/pdf", new byte[] { 1, 2, 3 });
        mockMvc.perform(multipart("/api/usuarios/me/firma")
                .file(pdf)
                .with(httpBasic(CONTRACTOR_EMAIL, "contratista123")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("FORMATO_IMAGEN_INVALIDO"));
    }

    @Test
    void actuatorHealthAndApiDocsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api-docs"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isLessThan(400));

        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isLessThan(400));
    }

    @Test
    void informesAreExposedInI2ButFutureNotificationsAreNot() throws Exception {
        mockMvc.perform(get("/api/informes")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));

        mockMvc.perform(get("/api/notificaciones")
                .with(httpBasic(ADMIN_EMAIL, "admin123")))
            .andExpect(status().isNotFound());
    }

    private static Usuario usuario(Long id, String email, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNombre("Usuario " + id);
        usuario.setCargo("Cargo " + id);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private static Contrato contrato(Long id, String numero, Usuario contratista) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumero(numero);
        contrato.setObjeto("Objeto contractual " + id);
        contrato.setTipo(TipoContrato.OPS);
        contrato.setEstado(EstadoContrato.EN_EJECUCION);
        contrato.setFechaInicio(LocalDate.of(2026, 1, 15));
        contrato.setFechaFin(LocalDate.of(2026, 12, 31));
        contrato.setValorTotal(BigDecimal.valueOf(18000000));
        contrato.setContratista(contratista);
        contrato.setActivo(true);
        return contrato;
    }

    private static String validContractJson(String numero) {
        return "{"
            + "\"numero\":\"" + numero + "\","
            + "\"objeto\":\"Objeto contractual\","
            + "\"tipo\":\"OPS\","
            + "\"valorTotal\":18000000,"
            + "\"fechaInicio\":\"2026-01-15\","
            + "\"fechaFin\":\"2026-12-31\","
            + "\"idContratista\":2,"
            + "\"idRevisor\":3,"
            + "\"idSupervisor\":4"
            + "}";
    }
}
