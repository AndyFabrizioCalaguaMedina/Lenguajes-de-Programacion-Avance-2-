package com.hcg.multiservicios.dto.catalogo;

import com.hcg.multiservicios.validation.Ruc;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank(message = "El RUC es obligatorio") @Ruc String ruc,
        @NotBlank(message = "La razón social es obligatoria") @Size(max = 255) String razonSocial,
        @Size(max = 255) String nombreComercial,
        @Size(max = 255) String direccion,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,30}$", message = "Teléfono no válido") String telefono1,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,30}$", message = "Teléfono no válido") String telefono2,
        @Email(message = "Correo no válido") @Size(max = 150) String correo,
        @Size(max = 150) String contacto) {
}
