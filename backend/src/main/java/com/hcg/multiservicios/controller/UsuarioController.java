package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.security.EstadoRequest;
import com.hcg.multiservicios.dto.security.PasswordResetRequest;
import com.hcg.multiservicios.dto.security.RolesRequest;
import com.hcg.multiservicios.dto.security.UsuarioCreateRequest;
import com.hcg.multiservicios.dto.security.UsuarioDto;
import com.hcg.multiservicios.dto.security.UsuarioUpdateRequest;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS_VER')")
    public List<UsuarioDto> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_VER')")
    public UsuarioDto obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioCreateRequest request,
                                            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request,
                                 @AuthenticationPrincipal UsuarioPrincipal actor) {
        return usuarioService.actualizar(id, request, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
    public UsuarioDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request,
                                    @AuthenticationPrincipal UsuarioPrincipal actor) {
        return usuarioService.cambiarEstado(id, request.activo(), actor);
    }

    /** Asignar roles y restablecer contraseñas: solo ADMIN. */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioDto asignarRoles(@PathVariable Long id, @Valid @RequestBody RolesRequest request,
                                   @AuthenticationPrincipal UsuarioPrincipal actor) {
        return usuarioService.asignarRoles(id, request.roles(), actor);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restablecerPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request,
                                                    @AuthenticationPrincipal UsuarioPrincipal actor) {
        usuarioService.restablecerPassword(id, request, actor);
        return ResponseEntity.noContent().build();
    }
}
