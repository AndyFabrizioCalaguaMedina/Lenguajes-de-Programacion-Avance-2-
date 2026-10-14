package com.hcg.multiservicios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Formato unico de error: {"success":false,"message":"...","errorCode":"..."} (+ errors por campo si aplica). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(boolean success, String message, String errorCode, Map<String, String> errors) {

    public static ApiError of(String message, String errorCode) {
        return new ApiError(false, message, errorCode, null);
    }

    public static ApiError of(String message, String errorCode, Map<String, String> errors) {
        return new ApiError(false, message, errorCode, errors);
    }
}
