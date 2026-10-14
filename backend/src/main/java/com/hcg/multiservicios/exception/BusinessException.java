package com.hcg.multiservicios.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con codigo estable para que el frontend pueda reaccionar sin parsear mensajes. */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException notFound(String errorCode, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, errorCode, message);
    }

    public static BusinessException conflict(String errorCode, String message) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static BusinessException badRequest(String errorCode, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, errorCode, message);
    }
}
