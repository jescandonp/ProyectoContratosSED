package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeRequest;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeResumenDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.ActividadInformeRequest;
import co.gov.bogota.sed.sigcon.application.dto.informe.InformeUpdateDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteAdjuntoDto;
import co.gov.bogota.sed.sigcon.application.dto.informe.SoporteUrlRequest;import co.gov.bogota.sed.sigcon.application.mapper.ActividadInformeMapper;
import co.gov.bogota.sed.sigcon.application.mapper.DocumentoAdicionalMapper;
import co.gov.bogota.sed.sigcon.application.mapper.InformeMapper;
import co.gov.bogota.sed.sigcon.application.mapper.ObservacionMapper;
import co.gov.bogota.sed.sigcon.application.mapper.SoporteAdjuntoMapper;
import co.gov.bogota.sed.sigcon.application.mapper.UsuarioMapper;
import co.gov.bogota.sed.sigcon.application.service.ActividadInformeService;
import co.gov.bogota.sed.sigcon.application.service.CurrentUserService;
import co.gov.bogota.sed.sigcon.application.service.DocumentStorageService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.SoporteAdjuntoService;
import co.gov.bogota.sed.sigcon.domain.entity.ActividadInforme;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Obligacion;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoContrato;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoContrato;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ContratoRepository;
import co.gov.bogota.sed.sigcon.domain.repository.DocumentoAdicionalRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObligacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SoporteAdjuntoRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeServiceTest {

    @Mock private InformeRepository informeRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private SoporteAdjuntoRepository soporteRepository;
    @Mock private DocumentoAdicionalRepository documentoAdicionalRepository;
    @Mock private ObservacionRepository observacionRepository;
    @Mock private ObligacionRepository obligacionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private DocumentStorageService documentStorageService;

    private InformeService informeService;
    private ActividadInformeService actividadService;
    private SoporteAdjuntoService soporteService;

    @BeforeEach
    void setUp() {
        SoporteAdjuntoMapper soporteMapper = new SoporteAdjuntoMapper();
        ActividadInformeMapper actividadMapper = new ActividadInformeMapper(soporteMapper);
        DocumentoAdicionalMapper docMapper = new DocumentoAdicionalMapper();
        ObservacionMapper obsMapper = new ObservacionMapper();
        InformeMapper informeMapper = new InformeMapper(new UsuarioMapper(), actividadMapper, docMapper, obsMapper);
        informeService = new InformeService(
            informeRepository, contratoRepository, actividadRepository, soporteRepository,
            documentoAdicionalRepository, observacionRepository, currentUserService, informeMapper
        );
        actividadService = new ActividadInformeService(
            actividadRepository, obligacionRepository, soporteRepository,
            informeService, currentUserService, actividadMapper
        );
        soporteService = new SoporteAdjuntoService(
            soporteRepository, actividadRepository, informeService,
            currentUserService, documentStorageService, soporteMapper
        );
    }

    @Test
    void contractorCreatesInformeOnOwnActiveContract() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Contrato contrato = contrato(10L, contratista, EstadoContrato.EN_EJECUCION);
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));
        when(informeRepository.countByContratoId(10L)).thenReturn(0);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> {
            Informe i = inv.getArgument(0);
            i.setId(99L);
            return i;
        });
        when(actividadRepository.findByInformeIdAndActivoTrue(99L)).thenReturn(Collections.emptyList());
        when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(99L)).thenReturn(Collections.emptyList());
        when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(99L)).thenReturn(Collections.emptyList());

        InformeRequest request = informeRequest(10L);
        InformeDetalleDto detalle = informeService.crearInforme(request);

        assertThat(detalle.getId()).isEqualTo(99L);
        assertThat(detalle.getEstado()).isEqualTo(EstadoInforme.BORRADOR);
        assertThat(detalle.getNumero()).isEqualTo(1);
        verify(informeRepository).save(any(Informe.class));
    }

    @Test
    void contractorCannotCreateInformeOnForeignContract() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Contrato contrato = contrato(10L, usuario(99L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION);
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> informeService.crearInforme(informeRequest(10L)))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void contractorCannotCreateInformeOnInactiveContract() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Contrato contrato = contrato(10L, contratista, EstadoContrato.LIQUIDADO);
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(contratoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> informeService.crearInforme(informeRequest(10L)))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTRATO_NO_ACTIVO));
    }

    @Test
    void contractorCannotEditApprovedInforme() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.APROBADO);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        assertThatThrownBy(() -> informeService.actualizarInforme(50L, informeRequest(10L)))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INFORME_NO_EDITABLE));
    }

    @Test
    void revisorListsOnlyAssignedEnviados() {
        Usuario revisor = usuario(3L, RolUsuario.REVISOR);
        Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.ENVIADO);
        informe.getContrato().setRevisor(revisor);
        when(currentUserService.getCurrentUser()).thenReturn(revisor);
        when(informeRepository.findByContratoRevisorAndEstadoAndActivoTrue(eq(revisor), eq(EstadoInforme.ENVIADO), any()))
            .thenReturn(new PageImpl<>(Collections.singletonList(informe)));

        Page<InformeResumenDto> page = informeService.listarParaRevisor(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        verify(informeRepository).findByContratoRevisorAndEstadoAndActivoTrue(eq(revisor), eq(EstadoInforme.ENVIADO), any());
    }

    @Test
    void supervisorListsOnlyAssignedEnRevision() {
        Usuario supervisor = usuario(4L, RolUsuario.SUPERVISOR);
        Informe informe = informe(50L, contrato(10L, usuario(2L, RolUsuario.CONTRATISTA), EstadoContrato.EN_EJECUCION), EstadoInforme.EN_REVISION);
        informe.getContrato().setSupervisor(supervisor);
        when(currentUserService.getCurrentUser()).thenReturn(supervisor);
        when(informeRepository.findByContratoSupervisorAndEstadoAndActivoTrue(eq(supervisor), eq(EstadoInforme.EN_REVISION), any()))
            .thenReturn(new PageImpl<>(Collections.singletonList(informe)));

        Page<InformeResumenDto> page = informeService.listarParaSupervisor(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        verify(informeRepository).findByContratoSupervisorAndEstadoAndActivoTrue(eq(supervisor), eq(EstadoInforme.EN_REVISION), any());
    }

    @Test
    void detalleExposesPdfMetadataForApprovedInforme() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.APROBADO);
        LocalDateTime pdfGeneradoAt = LocalDateTime.of(2026, 5, 2, 16, 0);
        informe.setPdfRuta("pdfs/10/50/informe-1.pdf");
        informe.setPdfGeneradoAt(pdfGeneradoAt);
        informe.setPdfHash("abc123");
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(actividadRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(50L)).thenReturn(Collections.emptyList());

        InformeDetalleDto detalle = informeService.obtenerDetalle(50L);

        assertThat(detalle.getPdfRuta()).isEqualTo("pdfs/10/50/informe-1.pdf");
        assertThat(detalle.getPdfGeneradoAt()).isEqualTo(pdfGeneradoAt);
        assertThat(detalle.getPdfHash()).isEqualTo("abc123");
    }

    @Test
    void invalidPorcentajeIsRejected() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        ActividadInformeRequest request = new ActividadInformeRequest();
        request.setIdObligacion(1L);
        request.setDescripcion("avance");
        request.setPorcentaje(new BigDecimal("150"));

        assertThatThrownBy(() -> actividadService.crear(50L, request))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PORCENTAJE_INVALIDO));
    }

    @Test
    void soporteUrlMustBeAbsoluteHttp() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
        ActividadInforme actividad = new ActividadInforme();
        actividad.setId(7L);
        actividad.setInforme(informe);
        actividad.setActivo(true);
        when(actividadRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(actividad));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        SoporteUrlRequest request = new SoporteUrlRequest();
        request.setNombre("Drive");
        request.setUrl("ftp://invalid.example.org/");

        assertThatThrownBy(() -> soporteService.agregarSoporteUrl(7L, request))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SOPORTE_INVALIDO));
    }

    @Test
    void soporteArchivoDelegatesToDocumentStorageWithSubdir() throws Exception {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Contrato contrato = contrato(10L, contratista, EstadoContrato.EN_EJECUCION);
        Informe informe = informe(50L, contrato, EstadoInforme.BORRADOR);
        ActividadInforme actividad = new ActividadInforme();
        actividad.setId(7L);
        actividad.setInforme(informe);
        actividad.setActivo(true);
        when(actividadRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(actividad));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(documentStorageService.storeFile(org.mockito.ArgumentMatchers.eq("soportes/10/50/7"), any()))
            .thenReturn("soportes/10/50/7/abc_documento.pdf");
        when(soporteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
            "file", "documento.pdf", "application/pdf", "x".getBytes()
        );

        SoporteAdjuntoDto dto = soporteService.agregarSoporteArchivo(7L, file);

        verify(documentStorageService).storeFile(org.mockito.ArgumentMatchers.eq("soportes/10/50/7"), any());
        assertThat(dto.getReferencia()).isEqualTo("soportes/10/50/7/abc_documento.pdf");
    }

    // ── T5: actualizar (PATCH periodo) ───────────────────────────────────────

    @Test
    void actualizarInformeBorradorExitoso() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actividadRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(50L)).thenReturn(Collections.emptyList());

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        InformeDetalleDto result = informeService.actualizar(50L, dto);

        assertThat(result).isNotNull();
        verify(informeRepository).save(any(Informe.class));
    }

    @Test
    void actualizarInformeDevueltoExitoso() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.DEVUELTO);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actividadRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(documentoAdicionalRepository.findByInformeIdAndActivoTrue(50L)).thenReturn(Collections.emptyList());
        when(observacionRepository.findByInformeIdAndActivoTrueOrderByFechaAsc(50L)).thenReturn(Collections.emptyList());

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        InformeDetalleDto result = informeService.actualizar(50L, dto);

        assertThat(result).isNotNull();
        verify(informeRepository).save(any(Informe.class));
    }

    @Test
    void actualizarInformeEnviadoFalla() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.ENVIADO);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThatThrownBy(() -> informeService.actualizar(50L, dto))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INFORME_NO_EDITABLE));
    }

    @Test
    void actualizarInformeAprobadoFalla() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.APROBADO);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThatThrownBy(() -> informeService.actualizar(50L, dto))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INFORME_NO_EDITABLE));
    }

    @Test
    void actualizarInformeContratistaIncorrectoFalla() {
        Usuario contratistaPropietario = usuario(2L, RolUsuario.CONTRATISTA);
        Usuario contratistaOtro = usuario(99L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratistaPropietario, EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratistaOtro);

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThatThrownBy(() -> informeService.actualizar(50L, dto))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESO_DENEGADO));
    }

    @Test
    void actualizarInformeFechaFinInvalidaFalla() {
        Usuario contratista = usuario(2L, RolUsuario.CONTRATISTA);
        Informe informe = informe(50L, contrato(10L, contratista, EstadoContrato.EN_EJECUCION), EstadoInforme.BORRADOR);
        when(informeRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(informe));
        when(currentUserService.getCurrentUser()).thenReturn(contratista);

        InformeUpdateDto dto = informeUpdateDto(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1));

        assertThatThrownBy(() -> informeService.actualizar(50L, dto))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FECHA_FIN_INVALIDA);
                assertThat(ex.getStatus().value()).isEqualTo(422);
            });
    }

    private static Usuario usuario(Long id, RolUsuario rol) {        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail("u" + id + "@educacionbogota.edu.co");
        u.setNombre("Usuario " + id);
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private static Contrato contrato(Long id, Usuario contratista, EstadoContrato estado) {
        Contrato c = new Contrato();
        c.setId(id);
        c.setNumero("OPS-2026-" + id);
        c.setObjeto("obj");
        c.setTipo(TipoContrato.OPS);
        c.setValorTotal(BigDecimal.valueOf(1000));
        c.setFechaInicio(LocalDate.of(2026, 1, 1));
        c.setFechaFin(LocalDate.of(2026, 12, 31));
        c.setEstado(estado);
        c.setContratista(contratista);
        c.setActivo(true);
        return c;
    }

    private static Informe informe(Long id, Contrato contrato, EstadoInforme estado) {
        Informe i = new Informe();
        i.setId(id);
        i.setNumero(1);
        i.setContrato(contrato);
        i.setFechaInicio(LocalDate.of(2026, 1, 1));
        i.setFechaFin(LocalDate.of(2026, 1, 31));
        i.setEstado(estado);
        i.setActivo(true);
        return i;
    }

    private static InformeRequest informeRequest(Long contratoId) {
        InformeRequest r = new InformeRequest();
        r.setIdContrato(contratoId);
        r.setFechaInicio(LocalDate.of(2026, 1, 1));
        r.setFechaFin(LocalDate.of(2026, 1, 31));
        return r;
    }

    private static InformeUpdateDto informeUpdateDto(LocalDate inicio, LocalDate fin) {
        InformeUpdateDto dto = new InformeUpdateDto();
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        return dto;
    }
}
