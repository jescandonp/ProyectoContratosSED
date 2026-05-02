package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.dto.informe.InformeDetalleDto;
import co.gov.bogota.sed.sigcon.application.service.EventoInformeService;
import co.gov.bogota.sed.sigcon.application.service.InformeEstadoService;
import co.gov.bogota.sed.sigcon.application.service.InformeService;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.application.service.PdfInformeService;
import co.gov.bogota.sed.sigcon.domain.entity.Contrato;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Usuario;
import co.gov.bogota.sed.sigcon.domain.enums.EstadoInforme;
import co.gov.bogota.sed.sigcon.domain.enums.RolUsuario;
import co.gov.bogota.sed.sigcon.domain.enums.TipoEvento;
import co.gov.bogota.sed.sigcon.domain.repository.ActividadInformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * I3-specific tests for InformeEstadoService:
 * PDF generation injection point and event publication wiring.
 */
@ExtendWith(MockitoExtension.class)
class InformeEstadoServiceI3Test {

    private static final String CONTRATISTA_EMAIL = "contratista@educacionbogota.edu.co";
    private static final String REVISOR_EMAIL    = "revisor@educacionbogota.edu.co";
    private static final String SUPERVISOR_EMAIL = "supervisor@educacionbogota.edu.co";

    @Mock private InformeRepository informeRepository;
    @Mock private ActividadInformeRepository actividadRepository;
    @Mock private InformeService informeService;
    @Mock private ObservacionService observacionService;
    @Mock private PdfInformeService pdfInformeService;
    @Mock private EventoInformeService eventoInformeService;

    private InformeEstadoService service;

    @BeforeEach
    void setUp() {
        service = new InformeEstadoService(
            informeRepository, actividadRepository, informeService, observacionService,
            pdfInformeService, eventoInformeService
        );
    }

    // -------------------------------------------------------------------------
    // aprobar(): PDF service wiring
    // -------------------------------------------------------------------------

    @Test
    void aprobarConFirmasLlamaAlPdfService() throws Exception {
        Informe informe = informe(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobar(50L, SUPERVISOR_EMAIL);

        verify(pdfInformeService).generarYPersistir(informe);
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.APROBADO);
    }

    @Test
    void aprobarConFirmaAusentePropagaFirmaRequerida() throws Exception {
        // If PdfInformeService throws FIRMA_REQUERIDA, aprobar() must propagate it.
        Informe informe = informe(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        SigconBusinessException firmaRequerida = new SigconBusinessException(
            ErrorCode.FIRMA_REQUERIDA, "Firma ausente", HttpStatus.UNPROCESSABLE_ENTITY
        );
        doThrow(firmaRequerida).when(pdfInformeService).generarYPersistir(informe);

        assertThatThrownBy(() -> service.aprobar(50L, SUPERVISOR_EMAIL))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FIRMA_REQUERIDA));
    }

    @Test
    void aprobarConPdfFallidoNoPersiste() throws Exception {
        // If PDF generation fails, state must NOT change to APROBADO and repository must not save.
        Informe informe = informe(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        SigconBusinessException pdfFallido = new SigconBusinessException(
            ErrorCode.PDF_GENERACION_FALLIDA, "Disco lleno", HttpStatus.INTERNAL_SERVER_ERROR
        );
        doThrow(pdfFallido).when(pdfInformeService).generarYPersistir(informe);

        assertThatThrownBy(() -> service.aprobar(50L, SUPERVISOR_EMAIL))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PDF_GENERACION_FALLIDA));

        // Estado must remain EN_REVISION — not persisted as APROBADO
        assertThat(informe.getEstado()).isEqualTo(EstadoInforme.EN_REVISION);
        verify(informeRepository, never()).save(any(Informe.class));
    }

    @Test
    void aprobarExitosoCreaNotificacion() throws Exception {
        Informe informe = informe(EstadoInforme.EN_REVISION);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.aprobar(50L, SUPERVISOR_EMAIL);

        verify(eventoInformeService).publicar(eq(TipoEvento.INFORME_APROBADO), eq(informe), isNull());
    }

    // -------------------------------------------------------------------------
    // enviar(): event publication wiring
    // -------------------------------------------------------------------------

    @Test
    void enviarCreaNotificacionParaRevisor() {
        Informe informe = informe(EstadoInforme.BORRADOR);
        when(informeService.findActiveInforme(50L)).thenReturn(informe);
        when(actividadRepository.countByInformeIdAndActivoTrue(50L)).thenReturn(1);
        when(informeRepository.save(any(Informe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(informeService.buildDetalle(informe)).thenReturn(new InformeDetalleDto());

        service.enviar(50L, CONTRATISTA_EMAIL);

        verify(eventoInformeService).publicar(eq(TipoEvento.INFORME_ENVIADO), eq(informe), isNull());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Informe informe(EstadoInforme estado) {
        Usuario contratista = usuario(1L, CONTRATISTA_EMAIL, RolUsuario.CONTRATISTA);
        Usuario revisor     = usuario(2L, REVISOR_EMAIL,    RolUsuario.REVISOR);
        Usuario supervisor  = usuario(3L, SUPERVISOR_EMAIL, RolUsuario.SUPERVISOR);

        Contrato contrato = new Contrato();
        contrato.setId(10L);
        contrato.setContratista(contratista);
        contrato.setRevisor(revisor);
        contrato.setSupervisor(supervisor);
        contrato.setActivo(true);

        Informe informe = new Informe();
        informe.setId(50L);
        informe.setContrato(contrato);
        informe.setEstado(estado);
        informe.setActivo(true);
        return informe;
    }

    private static Usuario usuario(Long id, String email, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNombre(email);
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}
