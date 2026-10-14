package com.hcg.multiservicios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OperacionEventoRequest(
        @NotBlank(message = "Indique el tipo")
        @Pattern(regexp = "SOLICITUD|COTIZACION|ENVIO|ACEPTACION|DESPACHO|COMPROBANTE|DOCUMENTO|NOTA",
                message = "Tipo de evento no válido") String tipo,
        @NotBlank(message = "El título es obligatorio") @Size(max = 150) String titulo,
        @Size(max = 500) String detalle,
        @Size(max = 60) String referencia) {
}
