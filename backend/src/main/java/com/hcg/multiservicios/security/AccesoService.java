package com.hcg.multiservicios.security;

import com.hcg.multiservicios.entity.Permiso;
import com.hcg.multiservicios.entity.Rol;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.repository.PermisoRepository;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calcula roles y permisos efectivos de un usuario. ADMIN siempre tiene todos, incluso los permisos futuros. */
@Service
public class AccesoService {

    private final PermisoRepository permisoRepository;

    public AccesoService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    public Set<String> roles(Usuario usuario) {
        Set<String> nombres = new TreeSet<>();
        for (Rol rol : usuario.getRoles()) {
            nombres.add(rol.getNombre());
        }
        return nombres;
    }

    @Transactional(readOnly = true)
    public Set<String> permisos(Usuario usuario) {
        Set<String> codigos = new TreeSet<>();
        if (usuario.esAdmin()) {
            for (Permiso p : permisoRepository.findAll()) {
                codigos.add(p.getCodigo());
            }
            return codigos;
        }
        for (Rol rol : usuario.getRoles()) {
            for (Permiso p : rol.getPermisos()) {
                codigos.add(p.getCodigo());
            }
        }
        return codigos;
    }
}
