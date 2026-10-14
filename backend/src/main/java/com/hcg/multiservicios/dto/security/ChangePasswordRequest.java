package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Ingrese su contraseña actual") String actual,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String nueva) {
}
