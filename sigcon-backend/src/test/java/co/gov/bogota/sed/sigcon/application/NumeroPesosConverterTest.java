package co.gov.bogota.sed.sigcon.application;

import co.gov.bogota.sed.sigcon.application.service.NumeroPesosConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumeroPesosConverterTest {

    @Test
    void nullReturnsZeroPesos() {
        assertThat(NumeroPesosConverter.convertir(null)).isEqualTo("CERO PESOS M/CTE");
    }

    @Test
    void zeroReturnsZeroPesos() {
        assertThat(NumeroPesosConverter.convertir(BigDecimal.ZERO)).isEqualTo("CERO PESOS M/CTE");
    }

    @Test
    void oneHundredTwentyThreeThousandReturnsCorrect() {
        assertThat(NumeroPesosConverter.convertir(new BigDecimal("123000")))
            .isEqualTo("CIENTO VEINTITRÉS MIL PESOS M/CTE");
    }

    @Test
    void oneMillionReturnsUnMillon() {
        assertThat(NumeroPesosConverter.convertir(new BigDecimal("1000000")))
            .isEqualTo("UN MILLÓN PESOS M/CTE");
    }

    @Test
    void exampleValueFromSpecConvertsCorrectly() {
        // 118666667 → CIENTO DIECIOCHO MILLONES SEISCIENTOS SESENTA Y SEIS MIL SEISCIENTOS SESENTA Y SIETE
        assertThat(NumeroPesosConverter.convertir(new BigDecimal("118666667")))
            .isEqualTo("CIENTO DIECIOCHO MILLONES SEISCIENTOS SESENTA Y SEIS MIL SEISCIENTOS SESENTA Y SIETE PESOS M/CTE");
    }
}
