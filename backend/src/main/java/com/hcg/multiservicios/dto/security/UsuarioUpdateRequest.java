package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 150) String correo) {
}
