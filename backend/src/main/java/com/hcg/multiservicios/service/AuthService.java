package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.security.ChangePasswordRequest;
import com.hcg.multiservicios.dto.security.LoginRequest;
import com.hcg.multiservicios.dto.security.LoginResponse;
import com.hcg.multiservicios.dto.security.UsuarioDto;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.UsuarioRepository;
import com.hcg.multiservicios.security.JwtService;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioMapper mapper;
    private final AuditoriaService auditoria;
    private final int maxIntentos;
    private final int minutosBloqueo;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       UsuarioMapper mapper, AuditoriaService auditoria,
                       @Value("${hcg.security.login.max-attempts}") int maxIntentos,
                       @Value("${hcg.security.login.lock-minutes}") int minutosBloqueo) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.auditoria = auditoria;
        this.maxIntentos = maxIntentos;
        this.minutosBloqueo = minutosBloqueo;
    }

    /** noRollbackFor: el contador de intentos fallidos y la auditoria deben guardarse aunque el login falle. */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request) {
        String id = request.identificador().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByUsernameOrCorreo(id, id).orElse(null);

        if (usuario == null) {
            auditoria.registrar((Long) null, id.length() > 50 ? id.substring(0, 50) : id, "LOGIN_FALLIDO", "Usuario", null, null, "usuario inexistente");
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", MSG_CREDENCIALES);
        }
        if (usuario.estaBloqueado()) {
            auditoria.registrar(usuario.getId(), usuario.getUsername(), "LOGIN_BLOQUEADO", "Usuario", usuario.getId(), null, null);
            throw new BusinessException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Cuenta bloqueada temporalmente por intentos fallidos. Intente más tarde.");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= maxIntentos) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(minutosBloqueo));
                usuario.setIntentosFallidos(0);
            }
            usuarioRepository.save(usuario);
            auditoria.registrar(usuario.getId(), usuario.getUsername(), "LOGIN_FALLIDO", "Usuario", usuario.getId(), null, "contraseña incorrecta");
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", MSG_CREDENCIALES);
        }
        if (!usuario.isActivo()) {
            auditoria.registrar(usuario.getId(), usuario.getUsername(), "LOGIN_USUARIO_INACTIVO", "Usuario", usuario.getId(), null, null);
            throw new BusinessException(HttpStatus.FORBIDDEN, "USER_INACTIVE", "Su usuario está desactivado. Contacte al administrador.");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);
        auditoria.registrar(usuario.getId(), usuario.getUsername(), "LOGIN_OK", "Usuario", usuario.getId(), null, null);

        return new LoginResponse(jwtService.generar(usuario), "Bearer", jwtService.expiracionEnSegundos(), mapper.toDto(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto me(UsuarioPrincipal principal) {
        Usuario u = usuarioRepository.findById(principal.id())
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado."));
        return mapper.toDto(u);
    }

    @Transactional
    public void cambiarPassword(UsuarioPrincipal principal, ChangePasswordRequest request) {
        Usuario u = usuarioRepository.findById(principal.id())
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado."));
        if (!passwordEncoder.matches(request.actual(), u.getPasswordHash())) {
            throw BusinessException.badRequest("WRONG_CURRENT_PASSWORD", "La contraseña actual no es correcta.");
        }
        u.setPasswordHash(passwordEncoder.encode(request.nueva()));
        u.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        auditoria.registrar(principal, "PASSWORD_CAMBIADA", "Usuario", u.getId(), null, null);
    }
}
