package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiDto;
import co.gov.bogota.sed.sigcon.application.dto.sgssi.AporteSgssiRequest;
import co.gov.bogota.sed.sigcon.application.mapper.AporteSgssiMapper;
import co.gov.bogota.sed.sigcon.application.service.AporteSgssiService;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.domain.entity.AporteSgssi;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.ItemSgssi;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.repository.AporteSgssiRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AporteSgssiServiceTest {

    @Mock private AporteSgssiRepository repository;
    @Mock private InformeService informeService;
    @Mock private CurrentUserService currentUserService;

    private AporteSgssiService service;

    @BeforeEach
    void setUp() {
        service = new AporteSgssiService(repository, informeService, currentUserService, new AporteSgssiMapper());
    }

    @Test
    void listarReturnsActivoAportesForInforme() {
        Informe informe = informe(10L, contratista(2L));
        AporteSgssi salud = aporte(1L, ItemSgssi.SALUD, informe);

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(2L));
        when(repository.findByInformeIdAndActivoTrue(10L)).thenReturn(Collections.singletonList(salud));

        List<AporteSgssiDto> result = service.listar(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItem()).isEqualTo(ItemSgssi.SALUD);
    }

    @Test
    void guardarTodosSoftDeletesExistingAndCreatesNew() {
        Informe informe = informe(10L, contratista(2L));
        AporteSgssi existente = aporte(1L, ItemSgssi.SALUD, informe);

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(2L));
        when(repository.findByInformeIdAndActivoTrue(10L)).thenReturn(Collections.singletonList(existente));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        AporteSgssiRequest req = request(ItemSgssi.ARL);
        List<AporteSgssiDto> result = service.guardarTodos(10L, Collections.singletonList(req));

        assertThat(existente.getActivo()).isFalse();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItem()).isEqualTo(ItemSgssi.ARL);
    }

    @Test
    void guardarTodosWithEmptyListSoftDeletesAll() {
        Informe informe = informe(10L, contratista(2L));
        AporteSgssi existente = aporte(1L, ItemSgssi.PENSION, informe);

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(2L));
        when(repository.findByInformeIdAndActivoTrue(10L)).thenReturn(Collections.singletonList(existente));
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AporteSgssiDto> result = service.guardarTodos(10L, Collections.emptyList());

        assertThat(existente.getActivo()).isFalse();
        assertThat(result).isEmpty();
    }

    @Test
    void guardarTodosRejectsNonContratista() {
        Informe informe = informe(10L, contratista(2L));
        Usuario revisor = new Usuario();
        revisor.setId(5L);
        revisor.setRol(RolUsuario.REVISOR);

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(revisor);
        // assertCanEditInforme throws for non-contratista
        org.mockito.Mockito.doThrow(new SigconBusinessException(
            ErrorCode.ACCESO_DENEGADO, "Acceso denegado", org.springframework.http.HttpStatus.FORBIDDEN))
            .when(informeService).assertCanEditInforme(revisor, informe);

        assertThatThrownBy(() -> service.guardarTodos(10L, Collections.emptyList()))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void guardarTodosCreatesMultipleAportes() {
        Informe informe = informe(10L, contratista(2L));

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(2L));
        when(repository.findByInformeIdAndActivoTrue(10L)).thenReturn(Collections.emptyList());
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<AporteSgssiRequest> requests = Arrays.asList(
            request(ItemSgssi.SALUD), request(ItemSgssi.PENSION), request(ItemSgssi.ARL)
        );
        List<AporteSgssiDto> result = service.guardarTodos(10L, requests);

        assertThat(result).hasSize(3);
    }

    @Test
    void listarDelegatesToRepositoryAfterAuthCheck() {
        Informe informe = informe(10L, contratista(2L));

        when(informeService.findActiveInforme(10L)).thenReturn(informe);
        when(currentUserService.getCurrentUser()).thenReturn(contratista(2L));
        when(repository.findByInformeIdAndActivoTrue(10L)).thenReturn(Collections.emptyList());

        assertThat(service.listar(10L)).isEmpty();
    }

    // --- helpers ---

    private static Informe informe(Long id, Usuario contratista) {
        Contrato contrato = new Contrato();
        contrato.setId(1L);
        contrato.setContratista(contratista);
        Informe informe = new Informe();
        informe.setId(id);
        informe.setContrato(contrato);
        informe.setEstado(EstadoInforme.BORRADOR);
        informe.setActivo(true);
        return informe;
    }

    private static Usuario contratista(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(RolUsuario.CONTRATISTA);
        return u;
    }

    private static AporteSgssi aporte(Long id, ItemSgssi item, Informe informe) {
        AporteSgssi a = new AporteSgssi();
        a.setId(id);
        a.setItem(item);
        a.setInforme(informe);
        a.setFechaPago(LocalDate.of(2026, 3, 1));
        a.setValorAportado(new BigDecimal("100000"));
        a.setEntidad("EPS Sanitas");
        a.setActivo(true);
        return a;
    }

    private static AporteSgssiRequest request(ItemSgssi item) {
        AporteSgssiRequest r = new AporteSgssiRequest();
        r.setItem(item);
        r.setFechaPago(LocalDate.of(2026, 3, 1));
        r.setValorAportado(new BigDecimal("100000"));
        r.setEntidad("EPS Sanitas");
        return r;
    }
}
