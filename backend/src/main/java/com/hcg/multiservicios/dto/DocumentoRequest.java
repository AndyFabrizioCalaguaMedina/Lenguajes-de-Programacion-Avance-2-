package com.hcg.multiservicios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro manual de un documento (factura, orden de compra, guia, otro...). */
public record DocumentoRequest(
        @NotBlank(message = "Indique el tipo de documento")
        @Pattern(regexp = "BOLETA|FACTURA|COTIZACION|ORDEN_COMPRA|GUIA|OTRO",
                message = "Tipo no válido (BOLETA, FACTURA, COTIZACION, ORDEN_COMPRA, GUIA u OTRO)") String tipo,
        @NotBlank(message = "El número es obligatorio") @Size(max = 40) String numero,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @Size(max = 255) String contraparteNombre,
        @Size(max = 20) String contraparteDoc,
        @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales") BigDecimal monto,
        @Pattern(regexp = "^$|^[A-Z]{3}$", message = "Moneda no válida (PEN, USD...)") String moneda,
        @Size(max = 500) String descripcion,
        @Size(max = 500) String archivoUrl) {
}
