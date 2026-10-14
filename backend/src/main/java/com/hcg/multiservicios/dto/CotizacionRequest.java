package com.hcg.multiservicios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Indique clienteId (cliente registrado) o clienteNombre. En cada item puede indicar productoId (toma nombre, unidad
 * y precio del catalogo si no los envia) o descripcion + precioUnitario. precioUnitario incluye IGV (18%).
 */
public record CotizacionRequest(
        Integer clienteId,
        @Size(max = 255) String clienteNombre,
        @Size(max = 30) String telefono,
        LocalDate vigenciaHasta,
        @Size(max = 500) String observacion,
        @NotEmpty(message = "Agregue al menos un ítem") List<@NotNull @Valid Item> items) {

    public record Item(
            Integer productoId,
            @Size(max = 255) String descripcion,
            @Size(max = 5) String unidad,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal cantidad,
            @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal precioUnitario) {
    }
}

