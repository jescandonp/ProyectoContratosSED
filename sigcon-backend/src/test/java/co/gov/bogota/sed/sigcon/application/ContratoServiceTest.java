package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.contrato.ContratoRequest;
import co.gov.bogota.sed.sigcon.application.mapper.ContratoMapper;
import co.gov.bogota.sed.sigcon.application.mapper.DocumentoCatalogoMapper;
import co.gov.bogota.sed.sigcon.application.mapper.ObligacionMapper;
import co.gov.bogota.sed.sigcon.application.mapper.UsuarioMapper;
import co.gov.bogota.sed.sigcon.application.service.ContratoService;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoCatalogoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.UsuarioRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ObligacionRepository obligacionRepository;

    @Mock
    private DocumentoCatalogoRepository documentoCatalogoRepository;

    @Mock
    private CurrentUserService currentUserService;

    private ContratoService contratoService;

    @BeforeEach
    void setUp() {
        UsuarioMapper usuarioMapper = new UsuarioMapper();
        ObligacionMapper obligacionMapper = new ObligacionMapper();
        DocumentoCatalogoMapper documentoCatalogoMapper = new DocumentoCatalogoMapper();
        ContratoMapper contratoMapper = new ContratoMapper(usuarioMapper, obligacionMapper, documentoCatalogoMapper);
        contratoService = new ContratoService(
            contratoRepository,
            usuarioRepository,
            obligacionRepository,
            documentoCatalogoRepository,
            currentUserService,
            contratoMapper
        );
    }

    @Test
    void adminListsAllActiveContracts() {
        Usuario admin = usuario(1L, RolUsuario.ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(contratoRepository.findByActivoTrue(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(contrato(10L, usuario(2L, RolUsuario.CONTRATISTA)))));

        contratoService.listarContratos(PageRequest.of(0, 10));

        verify(contratoRepository).findByActivoTrue(PageRequest.of(0, 10));
    }

    @Test
    void contractorListsOnlyOwnActiveContracts() {
        Usuario contractor = usuario(2L, RolUsuario.CONTRATISTA);
        when(currentUserService.getCurrentUser()).thenReturn(contractor);
        when(contratoRepository.findByContratistaAndActivoTrue(any(Usuario.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        contratoService.listarContratos(PageRequest.of(0, 10));

        verify(contratoRepository).findByContratistaAndActivoTrue(contractor, PageRequest.of(0, 10));
    }

    @Test
    void supervisorListsSupervisedActiveContracts() {
        Usuario supervisor = usuario(4L, RolUsuario.SUPERVISOR);
        when(currentUserService.getCurrentUser()).thenReturn(supervisor);
        when(contratoRepository.findBySupervisorAndActivoTrue(any(Usuario.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        contratoService.listarContratos(PageRequest.of(0, 10));

        verify(contratoRepository).findBySupervisorAndActivoTrue(supervisor, PageRequest.of(0, 10));
    }

    @Test
    void reviewerListsAssignedActiveContracts() {
        Usuario reviewer = usuario(3L, RolUsuario.REVISOR);
        when(currentUserService.getCurrentUser()).thenReturn(reviewer);
        when(contratoRepository.findByRevisorAndActivoTrue(any(Usuario.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        contratoService.listarContratos(PageRequest.of(0, 10));

        verify(contratoRepository).findByRevisorAndActivoTrue(reviewer, PageRequest.of(0, 10));
    }

    @Test
    void duplicateContractNumberThrowsConflict() {
        ContratoRequest request = contratoRequest("OPS-2026-001");
        when(contratoRepository.findByNumeroAndActivoTrue("OPS-2026-001"))
            .thenReturn(Optional.of(contrato(10L, usuario(2L, RolUsuario.CONTRATISTA))));

        assertThatThrownBy(() -> contratoService.crearContrato(request))
            .isInstanceOfSatisfying(SigconBusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NUMERO_CONTRATO_DUPLICADO));
    }

    @Test
    void contractorCannotAccessForeignContractDetail() {
        Usuario contractor = usuario(2L, RolUsuario.CONTRATISTA);
        Contrato foreignContract = contrato(10L, usuario(99L, RolUsuario.CONTRATISTA));
        when(currentUserService.getCurrentUser()).thenReturn(contractor);
        when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(foreignContract));

        assertThatThrownBy(() -> contratoService.obtenerDetalle(10L))
            .isInstanceOfSatisfying(SigconBusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void softDeleteMarksContractInactive() {
        Contrato contrato = contrato(10L, usuario(2L, RolUsuario.CONTRATISTA));
        when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));

        contratoService.eliminarContrato(10L);

        assertThat(contrato.getActivo()).isFalse();
        verify(contratoRepository).save(contrato);
    }

    private static Usuario usuario(Long id, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("usuario" + id + "@educacionbogota.edu.co");
        usuario.setNombre("Usuario " + id);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private static Contrato contrato(Long id, Usuario contratista) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setNumero("OPS-2026-" + id);
        contrato.setObjeto("Objeto contractual");
        contrato.setTipo(TipoContrato.OPS);
        contrato.setEstado(EstadoContrato.EN_EJECUCION);
        contrato.setFechaInicio(LocalDate.of(2026, 1, 15));
        contrato.setFechaFin(LocalDate.of(2026, 12, 31));
        contrato.setValorTotal(BigDecimal.valueOf(18000000));
        contrato.setContratista(contratista);
        contrato.setActivo(true);
        return contrato;
    }

    private static ContratoRequest contratoRequest(String numero) {
        ContratoRequest request = new ContratoRequest();
        request.setNumero(numero);
        request.setObjeto("Objeto contractual");
        request.setTipo(TipoContrato.OPS);
        request.setValorTotal(BigDecimal.valueOf(18000000));
        request.setFechaInicio(LocalDate.of(2026, 1, 15));
        request.setFechaFin(LocalDate.of(2026, 12, 31));
        request.setIdContratista(2L);
        request.setIdRevisor(3L);
        request.setIdSupervisor(4L);
        return request;
    }
}
