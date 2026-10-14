package com.hcg.multiservicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidator implements ConstraintValidator<Dni, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return esValido(valor.trim());
    }

    public static boolean esValido(String dni) {
        if (dni == null || dni.length() != 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (dni.charAt(i) < '0' || dni.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }
}
