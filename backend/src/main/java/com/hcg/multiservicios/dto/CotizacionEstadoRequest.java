package com.hcg.multiservicios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** FACTURADA no se puede fijar a mano: la asigna la conversión a boleta. */
public record CotizacionEstadoRequest(
        @NotBlank(message = "Indique el estado")
        @Pattern(regexp = "BORRADOR|ENVIADA|APROBADA|RECHAZADA", message = "Estado no válido (BORRADOR, ENVIADA, APROBADA o RECHAZADA)")
        String estado) {
}
