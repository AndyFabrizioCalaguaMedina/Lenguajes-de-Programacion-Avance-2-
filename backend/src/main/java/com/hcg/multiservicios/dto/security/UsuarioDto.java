package com.hcg.multiservicios.dto.security;

import java.time.LocalDateTime;
import java.util.Set;

/** Nunca incluye el hash de la contraseña. permisos = permisos efectivos (ADMIN: todos). */
public record UsuarioDto(Long id, String username, String nombreCompleto, String correo, boolean activo,
                         Set<String> roles, Set<String> permisos, LocalDateTime creadoEn, LocalDateTime ultimoLogin) {
}
