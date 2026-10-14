package com.hcg.multiservicios.dto.catalogo;

import com.hcg.multiservicios.validation.Dni;
import com.hcg.multiservicios.validation.Ruc;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** tipoDocumento: RUC (requiere ruc y razon social), DNI (requiere dni, nombres y apellidos) o SIN_DOCUMENTO. */
public record ClienteRequest(
        @NotBlank(message = "Indique el tipo de documento")
        @Pattern(regexp = "RUC|DNI|SIN_DOCUMENTO", message = "Tipo de documento no válido (RUC, DNI o SIN_DOCUMENTO)")
        String tipoDocumento,
        @Ruc String ruc,
        @Dni String dni,
        @Size(max = 255) String razonSocial,
        @Size(max = 255) String nombreComercial,
        @Size(max = 150) String nombres,
        @Size(max = 150) String apellidos,
        @Size(max = 255) String direccion,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,30}$", message = "Teléfono no válido") String telefono,
        @Email(message = "Correo no válido") @Size(max = 150) String correo) {
}
