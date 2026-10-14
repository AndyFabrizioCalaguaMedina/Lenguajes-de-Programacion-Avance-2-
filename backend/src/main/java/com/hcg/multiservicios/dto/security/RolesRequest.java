package com.hcg.multiservicios.dto.security;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record RolesRequest(@NotEmpty(message = "Asigne al menos un rol") Set<String> roles) {
}
