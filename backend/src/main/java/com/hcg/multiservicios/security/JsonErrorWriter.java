package com.hcg.multiservicios.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Escribe errores de seguridad (401/403) con el mismo formato que el resto de la API. */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String message, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = "{\"success\":false,\"message\":\"" + message + "\",\"errorCode\":\"" + errorCode + "\"}";
        response.getWriter().write(body);
    }
}
