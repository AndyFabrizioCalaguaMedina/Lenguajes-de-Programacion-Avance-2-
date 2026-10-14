package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.security.PasswordResetRequest;
import com.hcg.multiservicios.dto.security.UsuarioCreateRequest;
import com.hcg.multiservicios.dto.security.UsuarioDto;
import com.hcg.multiservicios.dto.security.UsuarioUpdateRequest;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.repository.UsuarioRepository;
import com.hcg.multiservicios.security.AccesoService;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final AccesoService accesoService;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder,
                          UsuarioMapper mapper, AccesoService accesoService, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.accesoService = accesoService;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return mapper.toDto(buscar(id));
    }

    @Transactional
    public UsuarioDto crear(UsuarioCreateRequest req, UsuarioPrincipal actor) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        String correo = req.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw BusinessException.conflict("USERNAME_TAKEN", "Ya existe un usuario con ese nombre de usuario.");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw BusinessException.conflict("EMAIL_TAKEN", "Ya existe un usuario con ese correo.");
        }
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setCorreo(correo);
        u.setNombreCompleto(req.nombreCompleto().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRoles(resolverRoles(req.roles()));
        u = usuarioRepository.save(u);
        auditoria.registrar(actor, "USUARIO_CREADO", "Usuario", u.getId(), null,
                "username=" + u.getUsername() + ", roles=" + accesoService.roles(u));
        return mapper.toDto(u);
    }

    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioUpdateRequest req, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        String correo = req.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw BusinessException.conflict("EMAIL_TAKEN", "Ya existe un usuario con ese correo.");
        }
        String antes = "nombre=" + u.getNombreCompleto() + ", correo=" + u.getCorreo();
        u.setNombreCompleto(req.nombreCompleto().trim());
        u.setCorreo(correo);
        u.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        auditoria.registrar(actor, "USUARIO_EDITADO", "Usuario", id, antes, "nombre=" + u.getNombreCompleto() + ", correo=" + u.getCorreo());
        return mapper.toDto(u);
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        if (!activo) {
            if (u.getId().equals(actor.id())) {
                throw BusinessException.badRequest("CANNOT_DEACTIVATE_SELF", "No puede desactivar su propio usuario.");
            }
            exigirOtroAdminActivo(u);
        }
        boolean antes = u.isActivo();
        u.setActivo(activo);
        u.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        auditoria.registrar(actor, activo ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", "Usuario", id, "activo=" + antes, "activo=" + activo);
        return mapper.toDto(u);
    }

    @Transactional
    public UsuarioDto asignarRoles(Long id, Set<String> nombres, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        Set<Rol> nuevos = resolverRoles(nombres);
        boolean perderiaAdmin = u.esAdmin() && nuevos.stream().noneMatch(r -> Rol.ADMIN.equals(r.getNombre()));
        if (perderiaAdmin) {
            exigirOtroAdminActivo(u);
        }
        String antes = String.valueOf(accesoService.roles(u));
        u.setRoles(nuevos);
        u.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        auditoria.registrar(actor, "ROLES_ASIGNADOS", "Usuario", id, "roles=" + antes, "roles=" + accesoService.roles(u));
        return mapper.toDto(u);
    }

    @Transactional
    public void restablecerPassword(Long id, PasswordResetRequest req, UsuarioPrincipal actor) {
        Usuario u = buscar(id);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        auditoria.registrar(actor, "PASSWORD_RESTABLECIDA", "Usuario", id, null, null); // sin valores sensibles
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Usuario no encontrado."));
    }

    private Set<Rol> resolverRoles(Set<String> nombres) {
        Set<Rol> roles = new HashSet<>();
        for (String nombre : nombres) {
            String n = nombre == null ? "" : nombre.trim().toUpperCase(Locale.ROOT);
            roles.add(rolRepository.findByNombre(n)
                    .orElseThrow(() -> BusinessException.badRequest("ROLE_NOT_FOUND", "El rol '" + n + "' no existe.")));
        }
        return roles;
    }

    /** Evita dejar el sistema sin ningun administrador activo. */
    private void exigirOtroAdminActivo(Usuario u) {
        if (u.esAdmin() && u.isActivo() && usuarioRepository.contarActivosConRol(Rol.ADMIN) <= 1) {
            throw BusinessException.badRequest("LAST_ADMIN", "Debe existir al menos un administrador activo.");
        }
    }
}
