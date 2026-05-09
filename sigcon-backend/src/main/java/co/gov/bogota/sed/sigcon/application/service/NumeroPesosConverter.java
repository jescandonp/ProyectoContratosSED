package co.gov.bogota.sed.sigcon.application.service;

import java.math.BigDecimal;

/**
 * Convierte un BigDecimal de pesos colombianos a texto en mayúsculas.
 * Compatible con Java 8. Soporta hasta 999.999.999.999.
 * Ejemplo: 118666667 → "CIENTO DIECIOCHO MILLONES SEISCIENTOS SESENTA Y SEIS MIL
 *                        SEISCIENTOS SESENTA Y SIETE PESOS M/CTE"
 */
public final class NumeroPesosConverter {

    private static final String[] UNIDADES = {
        "", "UN", "DOS", "TRES", "CUATRO", "CINCO",
        "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ",
        "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
        "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"
    };

    private static final String[] DECENAS = {
        "", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
        "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CENTENAS = {
        "", "CIEN", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private static final String[] VEINTITANTOS = {
        "", "VEINTIUN", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO",
        "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };

    private NumeroPesosConverter() {}

    public static String convertir(BigDecimal monto) {
        if (monto == null) {
            return "CERO PESOS M/CTE";
        }
        long entero = monto.toBigInteger().longValue();
        if (entero == 0) {
            return "CERO PESOS M/CTE";
        }
        return convertirLong(entero).trim() + " PESOS M/CTE";
    }

    private static String convertirLong(long n) {
        if (n == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        long miles_millones = n / 1_000_000_000L;
        long resto = n % 1_000_000_000L;
        long millones = resto / 1_000_000L;
        resto = resto % 1_000_000L;
        long miles = resto / 1_000L;
        long unidades = resto % 1_000L;

        if (miles_millones > 0) {
            sb.append(convertirGrupo((int) miles_millones)).append(" MIL");
        }
        if (millones > 0) {
            String grupoMill = convertirGrupo((int) millones);
            if (miles_millones > 0 || millones > 1) {
                sb.append(" ").append(grupoMill).append(" MILLONES");
            } else {
                // millones == 1 y no hay miles_millones
                sb.append(" UN MILLÓN");
            }
        }
        if (miles > 0) {
            if (miles == 1) {
                sb.append(" MIL");
            } else {
                sb.append(" ").append(convertirGrupo((int) miles)).append(" MIL");
            }
        }
        if (unidades > 0) {
            sb.append(" ").append(convertirGrupo((int) unidades));
        }
        return sb.toString();
    }

    private static String convertirGrupo(int n) {
        if (n == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int centenas = n / 100;
        int resto = n % 100;

        if (centenas > 0) {
            if (centenas == 1 && resto == 0) {
                sb.append("CIEN");
            } else if (centenas == 1) {
                sb.append("CIENTO");
            } else {
                sb.append(CENTENAS[centenas]);
            }
        }

        if (resto == 0) {
            return sb.toString();
        }

        if (sb.length() > 0) {
            sb.append(" ");
        }

        if (resto < 20) {
            sb.append(UNIDADES[resto]);
        } else if (resto < 30) {
            int uni = resto - 20;
            if (uni == 0) {
                sb.append("VEINTE");
            } else {
                sb.append(VEINTITANTOS[uni]);
            }
        } else {
            int decena = resto / 10;
            int uni = resto % 10;
            sb.append(DECENAS[decena]);
            if (uni > 0) {
                sb.append(" Y ").append(UNIDADES[uni]);
            }
        }
        return sb.toString();
    }
}
