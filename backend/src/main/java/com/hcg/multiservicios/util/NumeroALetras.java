package com.hcg.multiservicios.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte montos numéricos a su representación en letras según el estándar SUNAT.
 * Ejemplo: 118.00 -> "SON CIENTO DIECIOCHO CON 00/100 SOLES"
 */
public final class NumeroALetras {

    private static final String[] UNIDADES = {
            "", "UN ", "DOS ", "TRES ", "CUATRO ", "CINCO ", "SEIS ", "SIETE ", "OCHO ", "NUEVE "
    };

    private static final String[] DECENAS = {
            "DIEZ ", "ONCE ", "DOCE ", "TRECE ", "CATORCE ", "QUINCE ", "DIECISEIS ", "DIECISIETE ", "DIECIOCHO ", "DIECINUEVE ",
            "VEINTE ", "VEINTIUN ", "VEINTIDOS ", "VEINTITRES ", "VEINTICUATRO ", "VEINTICINCO ", "VEINTISEIS ", "VEINTISIETE ", "VEINTIOCHO ", "VEINTINUEVE "
    };

    private static final String[] DIEZ_DECENAS = {
            "", "DIEZ ", "VEINTE ", "TREINTA ", "CUARENTA ", "CINCUENTA ", "SESENTA ", "SETENTA ", "OCHENTA ", "NOVENTA "
    };

    private static final String[] CENTENAS = {
            "", "CIENTO ", "DOSCIENTOS ", "TRESCIENTOS ", "CUATROCIENTOS ", "QUINIENTOS ", "SEISCIENTOS ", "SETECIENTOS ", "OCHOCIENTOS ", "NOVECIENTOS "
    };

    private NumeroALetras() {}

    public static String convertir(BigDecimal cantidad, String moneda) {
        if (cantidad == null) {
            cantidad = BigDecimal.ZERO;
        }

        cantidad = cantidad.setScale(2, RoundingMode.HALF_UP);
        long parteEntera = cantidad.longValue();
        int centavos = cantidad.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();

        String letrasParteEntera = convertirNumero(parteEntera);
        if (letrasParteEntera.trim().isEmpty()) {
            letrasParteEntera = "CERO";
        }

        String nombreMoneda = "SOLES";
        if (moneda != null && moneda.equalsIgnoreCase("USD")) {
            nombreMoneda = "DÓLARES AMERICANOS";
        }

        String centavosFormateados = String.format("%02d/100", Math.abs(centavos));
        return ("SON " + letrasParteEntera.trim() + " CON " + centavosFormateados + " " + nombreMoneda).toUpperCase();
    }

    private static String convertirNumero(long n) {
        if (n == 0) return "CERO ";
        if (n == 100) return "CIEN ";

        StringBuilder sb = new StringBuilder();

        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            if (millones == 1) {
                sb.append("UN MILLON ");
            } else {
                sb.append(convertirNumero(millones)).append("MILLONES ");
            }
            n %= 1_000_000;
        }

        if (n >= 1_000) {
            long miles = n / 1_000;
            if (miles == 1) {
                sb.append("MIL ");
            } else {
                sb.append(convertirNumero(miles)).append("MIL ");
            }
            n %= 1_000;
        }

        if (n >= 100) {
            int centenas = (int) (n / 100);
            if (n == 100) {
                sb.append("CIEN ");
            } else {
                sb.append(CENTENAS[centenas]);
            }
            n %= 100;
        }

        if (n >= 10 && n <= 29) {
            sb.append(DECENAS[(int) n - 10]);
            n = 0;
        } else if (n >= 30) {
            int dec = (int) (n / 10);
            int uni = (int) (n % 10);
            sb.append(DIEZ_DECENAS[dec]);
            if (uni > 0) {
                sb.append("Y ").append(UNIDADES[uni]);
            }
            n = 0;
        }

        if (n > 0) {
            sb.append(UNIDADES[(int) n]);
        }

        return sb.toString();
    }
}
