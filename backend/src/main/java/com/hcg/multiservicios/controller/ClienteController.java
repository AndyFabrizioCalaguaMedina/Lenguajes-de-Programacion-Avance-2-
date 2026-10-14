package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.catalogo.ClienteDto;
import com.hcg.multiservicios.dto.catalogo.ClienteRequest;
import com.hcg.multiservicios.dto.security.EstadoRequest;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public PageResponse<ClienteDto> buscar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) Boolean activo,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return service.buscar(q, activo, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public ClienteDto obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTES_CREAR')")
    public ResponseEntity<ClienteDto> crear(@Valid @RequestBody ClienteRequest request,
                                    @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_EDITAR')")
    public ClienteDto actualizar(@PathVariable Integer id, @Valid @RequestBody ClienteRequest request,
                         @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, request, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLIENTES_EDITAR')")
    public ClienteDto cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request,
                            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(id, request.activo(), actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_EDITAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        service.eliminar(id, actor);
        return ResponseEntity.noContent().build();
    }
}
