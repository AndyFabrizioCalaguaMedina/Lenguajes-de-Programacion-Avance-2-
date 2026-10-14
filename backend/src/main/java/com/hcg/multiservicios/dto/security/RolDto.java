package com.hcg.multiservicios.dto.security;

import java.util.Set;

public record RolDto(Long id, String nombre, String descripcion, boolean sistema, Set<String> permisos) {
}
