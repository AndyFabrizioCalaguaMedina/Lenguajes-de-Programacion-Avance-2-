package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UsuarioCreateRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "Use 3 a 50 caracteres: letras, números, punto, guion o guion bajo")
        String username,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombreCompleto,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido") @Size(max = 150) String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @NotEmpty(message = "Asigne al menos un rol") Set<String> roles) {
}
