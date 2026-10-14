package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "Indique el estado") Boolean activo) {
}
