package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.security.UsuarioDto;
import com.hcg.multiservicios.entity.Usuario;
import com.hcg.multiservicios.security.AccesoService;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    private final AccesoService accesoService;

    public UsuarioMapper(AccesoService accesoService) {
        this.accesoService = accesoService;
    }

    public UsuarioDto toDto(Usuario u) {
        return new UsuarioDto(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getCorreo(), u.isActivo(),
                accesoService.roles(u), accesoService.permisos(u), u.getCreadoEn(), u.getUltimoLogin());
    }
}
