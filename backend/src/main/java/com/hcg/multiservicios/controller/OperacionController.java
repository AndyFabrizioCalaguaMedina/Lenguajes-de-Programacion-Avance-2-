package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.OperacionDto;
import com.hcg.multiservicios.dto.OperacionEstadoRequest;
import com.hcg.multiservicios.dto.OperacionEventoRequest;
import com.hcg.multiservicios.dto.OperacionRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.OperacionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Operaciones comerciales: ventas, compras y pedidos con su linea de tiempo. */
@RestController
@RequestMapping("/api/operations")
public class OperacionController {

    private final OperacionService service;

    public OperacionController(OperacionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPERACIONES_VER')")
    public PageResponse<OperacionDto> buscar(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String tipo,
                                             @RequestParam(required = false) String estado,
                                             @RequestParam(required = false) Integer proveedorId,
                                             @RequestParam(required = false) Integer clienteId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.buscar(q, tipo, estado, proveedorId, clienteId, desde, hasta, page, size);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('OPERACIONES_VER')")
    public OperacionDto.Resumen resumen() {
        return service.resumen();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACIONES_VER')")
    public OperacionDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPERACIONES_CREAR')")
    public ResponseEntity<OperacionDto> crear(@Valid @RequestBody OperacionRequest request,
                                              @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPERACIONES_EDITAR')")
    public OperacionDto actualizar(@PathVariable Long id, @Valid @RequestBody OperacionRequest request,
                                   @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, request, actor);
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("hasAuthority('OPERACIONES_EDITAR')")
    public ResponseEntity<OperacionDto> agregarEvento(@PathVariable Long id,
                                                      @Valid @RequestBody OperacionEventoRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarEvento(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('OPERACIONES_EDITAR')")
    public OperacionDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody OperacionEstadoRequest request,
                                      @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(id, request.estado(), actor);
    }
}
