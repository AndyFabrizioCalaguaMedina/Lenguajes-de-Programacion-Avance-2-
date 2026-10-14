package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** identificador = username o correo. */
public record LoginRequest(
        @NotBlank(message = "Ingrese su usuario o correo") @Size(max = 150) String identificador,
        @NotBlank(message = "Ingrese su contraseña") @Size(max = 100) String password) {
}
