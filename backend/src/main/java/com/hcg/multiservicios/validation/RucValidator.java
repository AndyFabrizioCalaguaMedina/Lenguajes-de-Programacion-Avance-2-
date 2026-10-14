package com.hcg.multiservicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RucValidator implements ConstraintValidator<Ruc, String> {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return esValido(valor.trim());
    }

    public static boolean esValido(String ruc) {
        if (ruc == null || ruc.length() != 11) {
            return false;
        }
        for (int i = 0; i < 11; i++) {
            if (ruc.charAt(i) < '0' || ruc.charAt(i) > '9') {
                return false;
            }
        }
        String prefijo = ruc.substring(0, 2);
        if (!(prefijo.equals("10") || prefijo.equals("15") || prefijo.equals("16")
                || prefijo.equals("17") || prefijo.equals("20"))) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS[i];
        }
        int digito = 11 - (suma % 11);
        if (digito == 10) {
            digito = 0;
        } else if (digito == 11) {
            digito = 1;
        }
        return digito == (ruc.charAt(10) - '0');
    }
}
