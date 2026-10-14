package com.hcg.multiservicios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OperacionEstadoRequest(
        @NotBlank(message = "Indique el estado")
        @Pattern(regexp = "EN_PROCESO|COMPLETADA|CANCELADA", message = "Estado no válido (EN_PROCESO, COMPLETADA o CANCELADA)")
        String estado) {
}
