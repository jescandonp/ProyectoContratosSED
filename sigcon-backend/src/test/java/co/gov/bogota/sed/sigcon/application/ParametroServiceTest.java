package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.ParametroService;
import co.gov.bogota.sed.sigcon.domain.entity.SgcnParametro;
import co.gov.bogota.sed.sigcon.domain.repository.InformeRepository;
import co.gov.bogota.sed.sigcon.domain.repository.SgcnParametroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametroServiceTest {

    @Mock private SgcnParametroRepository parametroRepository;
    @Mock private InformeRepository informeRepository;

    private ParametroService service;

    @BeforeEach
    void setUp() {
        service = new ParametroService(parametroRepository, informeRepository);
    }

    @Test
    void isVbActivo_cuandoParametroS_retornaTrue() {
        when(parametroRepository.findById("VB_ACTIVO")).thenReturn(Optional.of(parametro("S")));

        assertThat(service.isVbActivo()).isTrue();
    }

    @Test
    void isVbActivo_cuandoParametroN_retornaFalse() {
        when(parametroRepository.findById("VB_ACTIVO")).thenReturn(Optional.of(parametro("N")));

        assertThat(service.isVbActivo()).isFalse();
    }

    @Test
    void isVbActivo_cuandoParametroNoExiste_retornaFalse() {
        when(parametroRepository.findById("VB_ACTIVO")).thenReturn(Optional.empty());

        assertThat(service.isVbActivo()).isFalse();
    }

    @Test
    void setVbActivo_desactivar_migraInformesEnVistoBueno() {
        when(parametroRepository.findById("VB_ACTIVO")).thenReturn(Optional.of(parametro("S")));

        service.setVbActivo(false);

        ArgumentCaptor<SgcnParametro> captor = ArgumentCaptor.forClass(SgcnParametro.class);
        verify(parametroRepository).save(captor.capture());
        assertThat(captor.getValue().getClave()).isEqualTo("VB_ACTIVO");
        assertThat(captor.getValue().getValor()).isEqualTo("N");
        verify(informeRepository).migrarEnVistoBuenoAEnRevision();
    }

    @Test
    void setVbActivo_activar_noEjecutaMigracion() {
        when(parametroRepository.findById("VB_ACTIVO")).thenReturn(Optional.empty());

        service.setVbActivo(true);

        ArgumentCaptor<SgcnParametro> captor = ArgumentCaptor.forClass(SgcnParametro.class);
        verify(parametroRepository).save(captor.capture());
        assertThat(captor.getValue().getClave()).isEqualTo("VB_ACTIVO");
        assertThat(captor.getValue().getValor()).isEqualTo("S");
        verify(informeRepository, never()).migrarEnVistoBuenoAEnRevision();
    }

    @Test
    void isCargaInformesActiva_cuandoParametroNoExiste_retornaTrue() {
        when(parametroRepository.findById("CARGA_INFORMES_ACTIVA")).thenReturn(Optional.empty());

        assertThat(service.isCargaInformesActiva()).isTrue();
    }

    @Test
    void isCargaInformesActiva_cuandoParametroFalse_retornaFalse() {
        SgcnParametro parametro = new SgcnParametro();
        parametro.setClave("CARGA_INFORMES_ACTIVA");
        parametro.setValor("false");
        when(parametroRepository.findById("CARGA_INFORMES_ACTIVA")).thenReturn(Optional.of(parametro));

        assertThat(service.isCargaInformesActiva()).isFalse();
    }

    @Test
    void setCargaInformesActiva_desactivar_retornaEstadoAnteriorYGuardaFalse() {
        when(parametroRepository.findById("CARGA_INFORMES_ACTIVA")).thenReturn(Optional.empty());

        boolean anterior = service.setCargaInformesActiva(false);

        ArgumentCaptor<SgcnParametro> captor = ArgumentCaptor.forClass(SgcnParametro.class);
        verify(parametroRepository).save(captor.capture());
        assertThat(anterior).isTrue();
        assertThat(captor.getValue().getClave()).isEqualTo("CARGA_INFORMES_ACTIVA");
        assertThat(captor.getValue().getValor()).isEqualTo("false");
    }

    private static SgcnParametro parametro(String valor) {
        SgcnParametro parametro = new SgcnParametro();
        parametro.setClave("VB_ACTIVO");
        parametro.setValor(valor);
        parametro.setDescripcion("Visto Bueno Administrativo activo en el flujo de informes");
        return parametro;
    }
}
