package com.hcg.multiservicios.dto.catalogo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Sirve para productos y servicios. En servicios, stock, stockMinimo y precioCompra deben ir vacios.
 * codigo es opcional: si no se envia se genera (PRD-000001 / SRV-000001).
 */
public record ProductoRequest(
        @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "El código solo admite letras, números, punto, guion y guion bajo")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 255) String nombre,
        @Size(max = 500) String descripcion,
        @Size(max = 80) String categoria,
        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal precioVenta,
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal precioCompra,
        @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
        @Size(max = 20) String unidadMedida) {
}
