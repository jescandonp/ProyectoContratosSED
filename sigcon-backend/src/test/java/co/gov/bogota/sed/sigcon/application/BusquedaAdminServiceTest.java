package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.busqueda.BusquedaAdminResponse;
import co.gov.bogota.sed.sigcon.application.service.BusquedaAdminService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaAdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private InformeRepository informeRepository;

    private BusquedaAdminService service;

    @BeforeEach
    void setUp() {
        service = new BusquedaAdminService(usuarioRepository, contratoRepository, informeRepository);
    }

    @Test
    void buscarRetornaGruposVaciosCuandoNoHayResultados() {
        when(usuarioRepository.buscarContratistas(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(contratoRepository.buscarContratos(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(informeRepository.buscarInformes(any(), any(), any(), any(Pageable.class))).thenReturn(Collections.emptyList());

        BusquedaAdminResponse resp = service.buscar("inexistente", null, null);

        assertThat(resp.getContratistas()).isEmpty();
        assertThat(resp.getContratos()).isEmpty();
        assertThat(resp.getInformes()).isEmpty();
    }

    @Test
    void buscarRetornaContratistasCuandoCoincideNombre() {
        Usuario contratista = contratista(1L, "Ana García", "ana@sed.gov.co");
        when(usuarioRepository.buscarContratistas(eq("Ana"), any(Pageable.class)))
            .thenReturn(Collections.singletonList(contratista));
        when(contratoRepository.buscarContratos(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(informeRepository.buscarInformes(any(), any(), any(), any(Pageable.class))).thenReturn(Collections.emptyList());

        BusquedaAdminResponse resp = service.buscar("Ana", null, null);

        assertThat(resp.getContratistas()).hasSize(1);
        assertThat(resp.getContratistas().get(0).getNombre()).isEqualTo("Ana García");
        assertThat(resp.getContratistas().get(0).getEmail()).isEqualTo("ana@sed.gov.co");
    }

    @Test
    void buscarRetornaContratosConNombreContratista() {
        Contrato contrato = contrato(10L, "OPS-2026-001", contratista(1L, "Ana García", "ana@sed.gov.co"));
        when(usuarioRepository.buscarContratistas(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(contratoRepository.buscarContratos(eq("OPS"), any(Pageable.class)))
            .thenReturn(Collections.singletonList(contrato));
        when(informeRepository.buscarInformes(any(), any(), any(), any(Pageable.class))).thenReturn(Collections.emptyList());

        BusquedaAdminResponse resp = service.buscar("OPS", null, null);

        assertThat(resp.getContratos()).hasSize(1);
        assertThat(resp.getContratos().get(0).getNumero()).isEqualTo("OPS-2026-001");
        assertThat(resp.getContratos().get(0).getContratistaNombre()).isEqualTo("Ana García");
    }

    @Test
    void buscarRetornaInformesConDatosCompletos() {
        Informe informe = informe(50L, 3, EstadoInforme.APROBADO,
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
            contrato(10L, "OPS-2026-001", contratista(1L, "Ana García", "ana@sed.gov.co")));
        when(usuarioRepository.buscarContratistas(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(contratoRepository.buscarContratos(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(informeRepository.buscarInformes(eq("APROBADO"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Collections.singletonList(informe));

        BusquedaAdminResponse resp = service.buscar("APROBADO", null, null);

        assertThat(resp.getInformes()).hasSize(1);
        assertThat(resp.getInformes().get(0).getEstado()).isEqualTo("APROBADO");
        assertThat(resp.getInformes().get(0).getContratoNumero()).isEqualTo("OPS-2026-001");
        assertThat(resp.getInformes().get(0).getContratistaNombre()).isEqualTo("Ana García");
    }

    @Test
    void buscarPasaRangoDeFechasAlRepositorioDeInformes() {
        LocalDate inicio = LocalDate.of(2026, 5, 1);
        LocalDate fin = LocalDate.of(2026, 5, 31);
        when(usuarioRepository.buscarContratistas(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(contratoRepository.buscarContratos(any(), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(informeRepository.buscarInformes(eq(""), eq(inicio), eq(fin), any(Pageable.class)))
            .thenReturn(Collections.emptyList());

        service.buscar("", inicio, fin);

        // La verificación implícita es que el mock fue llamado con los parámetros correctos
        // (Mockito lanzaría si no coincide)
    }

    @Test
    void buscarNormalizaTerminoNullComoVacio() {
        when(usuarioRepository.buscarContratistas(eq(""), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(contratoRepository.buscarContratos(eq(""), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(informeRepository.buscarInformes(eq(""), any(), any(), any(Pageable.class))).thenReturn(Collections.emptyList());

        BusquedaAdminResponse resp = service.buscar(null, null, null);

        assertThat(resp.getContratistas()).isEmpty();
        assertThat(resp.getContratos()).isEmpty();
        assertThat(resp.getInformes()).isEmpty();
    }

    @Test
    void buscarRetornaMultiplesGruposSimultaneamente() {
        Usuario c1 = contratista(1L, "Ana García", "ana@sed.gov.co");
        Usuario c2 = contratista(2L, "Carlos García", "carlos@sed.gov.co");
        Contrato contrato = contrato(10L, "OPS-2026-001", c1);
        Informe informe = informe(50L, 1, EstadoInforme.BORRADOR,
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), contrato);

        when(usuarioRepository.buscarContratistas(eq("García"), any(Pageable.class)))
            .thenReturn(Arrays.asList(c1, c2));
        when(contratoRepository.buscarContratos(eq("García"), any(Pageable.class)))
            .thenReturn(Collections.singletonList(contrato));
        when(informeRepository.buscarInformes(eq("García"), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Collections.singletonList(informe));

        BusquedaAdminResponse resp = service.buscar("García", null, null);

        assertThat(resp.getContratistas()).hasSize(2);
        assertThat(resp.getContratos()).hasSize(1);
        assertThat(resp.getInformes()).hasSize(1);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Usuario contratista(Long id, String nombre, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setEmail(email);
        u.setCargo("Profesional");
        u.setRol(RolUsuario.CONTRATISTA);
        u.setActivo(true);
        return u;
    }

    private static Contrato contrato(Long id, String numero, Usuario contratista) {
        Contrato c = new Contrato();
        c.setId(id);
        c.setNumero(numero);
        c.setObjeto("Objeto del contrato " + numero);
        c.setTipo(TipoContrato.OPS);
        c.setEstado(EstadoContrato.EN_EJECUCION);
        c.setValorTotal(BigDecimal.valueOf(18000000));
        c.setFechaInicio(LocalDate.of(2026, 1, 1));
        c.setFechaFin(LocalDate.of(2026, 12, 31));
        c.setContratista(contratista);
        c.setActivo(true);
        return c;
    }

    private static Informe informe(Long id, Integer numero, EstadoInforme estado,
                                    LocalDate fechaInicio, LocalDate fechaFin, Contrato contrato) {
        Informe i = new Informe();
        i.setId(id);
        i.setNumero(numero);
        i.setEstado(estado);
        i.setFechaInicio(fechaInicio);
        i.setFechaFin(fechaFin);
        i.setContrato(contrato);
        i.setActivo(true);
        return i;
    }
}
