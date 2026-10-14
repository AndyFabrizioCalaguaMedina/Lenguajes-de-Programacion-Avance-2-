package com.hcg.multiservicios.exception;

public class SunatException extends RuntimeException {
    private final String codigo;

    public SunatException(String message) {
        super(message);
        this.codigo = null;
    }

    public SunatException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
