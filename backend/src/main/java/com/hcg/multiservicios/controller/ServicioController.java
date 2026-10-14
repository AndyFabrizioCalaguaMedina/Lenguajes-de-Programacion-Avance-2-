package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.catalogo.ProductoDto;
import com.hcg.multiservicios.dto.catalogo.ProductoRequest;
import com.hcg.multiservicios.dto.security.EstadoRequest;
import com.hcg.multiservicios.entity.Producto;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.ProductoService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServicioController {

    private final ProductoService service;

    public ServicioController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_VER')")
    public PageResponse<ProductoDto> buscar(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) Boolean activo,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return service.buscar(Producto.TIPO_SERVICIO, q, activo, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_VER')")
    public ProductoDto obtener(@PathVariable Integer id) {
        return service.obtener(Producto.TIPO_SERVICIO, id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_CREAR')")
    public ResponseEntity<ProductoDto> crear(@Valid @RequestBody ProductoRequest request,
                                    @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(Producto.TIPO_SERVICIO, request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTOS_EDITAR')")
    public ProductoDto actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request,
                         @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(Producto.TIPO_SERVICIO, id, request, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PRODUCTOS_EDITAR')")
    public ProductoDto cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request,
                            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(Producto.TIPO_SERVICIO, id, request.activo(), actor);
    }
}
