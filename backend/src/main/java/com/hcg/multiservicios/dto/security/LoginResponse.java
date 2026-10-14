package com.hcg.multiservicios.dto.security;

public record LoginResponse(String token, String tipo, long expiraEnSegundos, UsuarioDto usuario) {
}
