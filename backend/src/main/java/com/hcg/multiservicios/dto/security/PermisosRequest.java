package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record PermisosRequest(@NotNull(message = "Indique los permisos") Set<String> permisos) {
}
