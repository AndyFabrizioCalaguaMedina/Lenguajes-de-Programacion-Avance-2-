package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.SalidaBodegaDto;
import com.hcg.multiservicios.dto.SalidaBodegaRequest;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.SalidaBodegaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Salidas de bodega: pensado para registrarse desde el celular (productos por código escaneado). */
@RestController
@RequestMapping("/api/warehouse-exits")
public class SalidaBodegaController {

    private final SalidaBodegaService service;

    public SalidaBodegaController(SalidaBodegaService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BODEGA_CREAR')")
    public ResponseEntity<SalidaBodegaDto> registrar(@Valid @RequestBody SalidaBodegaRequest request,
                                                     @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request, actor));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BODEGA_VER')")
    public PageResponse<SalidaBodegaDto> buscar(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String estado,
                                                @RequestParam(required = false) String destinoTipo,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.buscar(q, estado, destinoTipo, desde, hasta, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BODEGA_VER')")
    public SalidaBodegaDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('BODEGA_ANULAR')")
    public SalidaBodegaDto anular(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.anular(id, actor);
    }
}
