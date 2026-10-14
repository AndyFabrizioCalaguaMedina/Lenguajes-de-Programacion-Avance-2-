package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RolCreateRequest(
        @NotBlank(message = "El nombre del rol es obligatorio")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,39}$", message = "Use mayúsculas, números o guion bajo (3 a 40 caracteres)")
        String nombre,
        @Size(max = 200) String descripcion) {
}
