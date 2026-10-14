package com.hcg.multiservicios.security;

/** Usuario autenticado disponible en controllers con @AuthenticationPrincipal. */
public record UsuarioPrincipal(Long id, String username, String nombreCompleto) {
}
