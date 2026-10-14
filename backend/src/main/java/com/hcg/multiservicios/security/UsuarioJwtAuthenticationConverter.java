package com.hcg.multiservicios.security;

import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convierte un JWT valido en la autenticacion de la app. Consulta la BD en cada peticion, asi
 * desactivar un usuario o cambiarle permisos surte efecto de inmediato, sin esperar a que expire el token.
 */
@Component
public class UsuarioJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;
    private final AccesoService accesoService;

    public UsuarioJwtAuthenticationConverter(UsuarioRepository usuarioRepository, AccesoService accesoService) {
        this.usuarioRepository = usuarioRepository;
        this.accesoService = accesoService;
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Token inválido");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new BadCredentialsException("Usuario inexistente o inactivo"));

        List<GrantedAuthority> autoridades = new ArrayList<>();
        accesoService.roles(usuario).forEach(r -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + r)));
        accesoService.permisos(usuario).forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario.getId(), usuario.getUsername(), usuario.getNombreCompleto());
        return new UsernamePasswordAuthenticationToken(principal, jwt, autoridades);
    }
}
