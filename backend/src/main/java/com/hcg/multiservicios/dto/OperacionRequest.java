package com.hcg.multiservicios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indique clienteId o proveedorId (toma el nombre del registro) o, si la contraparte no esta registrada,
 * contraparteNombre. fechaInicio es opcional (por defecto hoy).
 */
public record OperacionRequest(
        @NotBlank(message = "Indique el tipo")
        @Pattern(regexp = "VENTA|COMPRA|PEDIDO", message = "Tipo no válido (VENTA, COMPRA o PEDIDO)") String tipo,
        @NotBlank(message = "El título es obligatorio") @Size(max = 255) String titulo,
        @Size(max = 500) String descripcion,
        Integer clienteId,
        Integer proveedorId,
        @Size(max = 255) String contraparteNombre,
        @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal monto,
        LocalDate fechaInicio) {
}
