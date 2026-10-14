package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.security.PermisoDto;
import com.hcg.multiservicios.dto.security.PermisosRequest;
import com.hcg.multiservicios.dto.security.RolCreateRequest;
import com.hcg.multiservicios.dto.security.RolDto;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.RolService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('USUARIOS_VER')")
    public List<RolDto> roles() {
        return rolService.listar();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('USUARIOS_VER')")
    public List<PermisoDto> permisos() {
        return rolService.listarPermisos();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolDto> crear(@Valid @RequestBody RolCreateRequest request,
                                        @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request, actor));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public RolDto asignarPermisos(@PathVariable Long id, @Valid @RequestBody PermisosRequest request,
                                  @AuthenticationPrincipal UsuarioPrincipal actor) {
        return rolService.asignarPermisos(id, request.permisos(), actor);
    }
}
