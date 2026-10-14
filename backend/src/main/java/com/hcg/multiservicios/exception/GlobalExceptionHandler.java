package com.hcg.multiservicios.exception;

import com.hcg.multiservicios.dto.ApiError;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Manejo global: respuestas consistentes y sin stack traces ni detalles internos. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> negocio(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(FieldValidationException.class)
    ResponseEntity<Object> campos(FieldValidationException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("Hay datos inválidos en el formulario.", "VALIDATION_ERROR", ex.getErrors()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> prohibido(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("No tiene permisos para realizar esta acción.", "FORBIDDEN"));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> noAutenticado(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("Sesión no válida o expirada. Inicie sesión nuevamente.", "UNAUTHORIZED"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("La operación viola una restricción de datos (duplicado o referencia inexistente).",
                        "DATA_INTEGRITY_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> inesperado(Exception ex) {
        log.error("Error inesperado", ex); // el detalle va solo al log del servidor
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("Ocurrió un error inesperado. Intente nuevamente.", "INTERNAL_ERROR"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.of("Hay datos inválidos en el formulario.", "VALIDATION_ERROR", errores));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        String codigo = switch (statusCode.value()) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "REQUEST_ERROR";
        };
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ApiError.of("No se pudo procesar la solicitud.", codigo));
    }
}
