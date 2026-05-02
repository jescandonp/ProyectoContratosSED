package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.mapper.ObservacionMapper;
import co.gov.bogota.sed.sigcon.application.service.ObservacionService;
import co.gov.bogota.sed.sigcon.domain.entity.Informe;
import co.gov.bogota.sed.sigcon.domain.entity.Observacion;
import co.gov.bogota.sed.sigcon.domain.enums.RolObservacion;
import co.gov.bogota.sed.sigcon.domain.repository.ObservacionRepository;
import co.gov.bogota.sed.sigcon.web.exception.ErrorCode;
import co.gov.bogota.sed.sigcon.web.exception.SigconBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservacionServiceTest {

    @Mock private ObservacionRepository observacionRepository;

    private ObservacionService service;

    @BeforeEach
    void setUp() {
        service = new ObservacionService(observacionRepository, new ObservacionMapper());
    }

    @Test
    void registersTrimmedObservation() {
        when(observacionRepository.save(any(Observacion.class))).thenAnswer(inv -> {
            Observacion observacion = inv.getArgument(0);
            observacion.setId(8L);
            return observacion;
        });

        Observacion observacion = service.registrar(new Informe(), RolObservacion.REVISOR, "  corregir soporte  ");

        assertThat(observacion.getId()).isEqualTo(8L);
        assertThat(observacion.getTexto()).isEqualTo("corregir soporte");
        assertThat(observacion.getAutorRol()).isEqualTo(RolObservacion.REVISOR);
    }

    @Test
    void rejectsEmptyObservation() {
        assertThatThrownBy(() -> service.registrar(new Informe(), RolObservacion.SUPERVISOR, " "))
            .isInstanceOfSatisfying(SigconBusinessException.class, ex ->
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OBSERVACION_REQUERIDA));
    }
}
