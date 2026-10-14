package com.hcg.multiservicios.exception;

import java.util.Map;

/** Errores de validacion por campo detectados en la capa de servicio (reglas entre campos). */
public class FieldValidationException extends RuntimeException {

    private final transient Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("Datos inválidos");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
