package com.hcg.multiservicios.security;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Fabrica publica de los handlers de 401/403 en formato JSON. */
public final class JsonErrorWriterAccess {

    private JsonErrorWriterAccess() {
    }

    public static AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> JsonErrorWriter.write(response, 401,
                "Sesión no válida o expirada. Inicie sesión nuevamente.", "UNAUTHORIZED");
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> JsonErrorWriter.write(response, 403,
                "No tiene permisos para realizar esta acción.", "FORBIDDEN");
    }
}
