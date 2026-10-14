package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.security.PermisoDto;
import com.hcg.multiservicios.dto.security.RolCreateRequest;
import com.hcg.multiservicios.dto.security.RolDto;
import com.hcg.multiservicios.entity.Permiso;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.PermisoRepository;
import com.hcg.multiservicios.repository.RolRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AuditoriaService auditoria;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository, AuditoriaService auditoria) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<RolDto> listar() {
        return rolRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PermisoDto> listarPermisos() {
        return permisoRepository.findAllByOrderByModuloAscAccionAsc().stream()
                .map(p -> new PermisoDto(p.getCodigo(), p.getModulo(), p.getAccion(), p.getDescripcion()))
                .toList();
    }

    @Transactional
    public RolDto crear(RolCreateRequest req, UsuarioPrincipal actor) {
        if (rolRepository.existsByNombre(req.nombre())) {
            throw BusinessException.conflict("ROLE_EXISTS", "Ya existe un rol con ese nombre.");
        }
        Rol rol = new Rol();
        rol.setNombre(req.nombre());
        rol.setDescripcion(req.descripcion());
        rol = rolRepository.save(rol);
        auditoria.registrar(actor, "ROL_CREADO", "Rol", rol.getId(), null, "nombre=" + rol.getNombre());
        return toDto(rol);
    }

    @Transactional
    public RolDto asignarPermisos(Long id, Set<String> codigos, UsuarioPrincipal actor) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ROLE_NOT_FOUND", "Rol no encontrado."));
        if (Rol.ADMIN.equals(rol.getNombre())) {
            throw BusinessException.badRequest("ADMIN_ROLE_LOCKED", "El rol ADMIN siempre tiene todos los permisos y no se modifica.");
        }
        List<Permiso> encontrados = permisoRepository.findByCodigoIn(codigos);
        if (encontrados.size() != codigos.size()) {
            throw BusinessException.badRequest("PERMISSION_NOT_FOUND", "Alguno de los permisos indicados no existe.");
        }
        String antes = String.valueOf(new TreeSet<>(toDto(rol).permisos()));
        rol.setPermisos(new HashSet<>(encontrados));
        rolRepository.save(rol);
        auditoria.registrar(actor, "PERMISOS_ASIGNADOS", "Rol", id, "permisos=" + antes, "permisos=" + new TreeSet<>(toDto(rol).permisos()));
        return toDto(rol);
    }

    private RolDto toDto(Rol r) {
        Set<String> codigos = new TreeSet<>();
        r.getPermisos().forEach(p -> codigos.add(p.getCodigo()));
        return new RolDto(r.getId(), r.getNombre(), r.getDescripcion(), r.isSistema(), codigos);
    }
}
