package com.hcg.multiservicios.security;

import com.hcg.multiservicios.entity.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite tokens. El token solo identifica al usuario; roles y permisos se leen de la BD en cada peticion. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder encoder, @Value("${hcg.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("hcg-multiservicios")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(String.valueOf(usuario.getId()))
                .claim("username", usuario.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiracionEnSegundos() {
        return expirationMinutes * 60;
    }
}
