package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.CotizacionDto;
import com.hcg.multiservicios.dto.CotizacionEstadoRequest;
import com.hcg.multiservicios.dto.CotizacionRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.CotizacionService;
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
@RequestMapping("/api/quotations")
public class CotizacionController {

    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COTIZACIONES_VER')")
    public PageResponse<CotizacionDto> buscar(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String estado,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.buscar(q, estado, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COTIZACIONES_VER')")
    public CotizacionDto obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COTIZACIONES_CREAR')")
    public ResponseEntity<CotizacionDto> crear(@Valid @RequestBody CotizacionRequest request,
                                               @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COTIZACIONES_EDITAR')")
    public CotizacionDto actualizar(@PathVariable Integer id, @Valid @RequestBody CotizacionRequest request,
                                    @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, request, actor);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('COTIZACIONES_EDITAR')")
    public CotizacionDto cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CotizacionEstadoRequest request,
                                       @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.cambiarEstado(id, request.estado(), actor);
    }

    /** Emite una boleta con los datos de la cotización aprobada (usa APIsPERU). */
    @PostMapping("/{id}/convertir-boleta")
    @PreAuthorize("hasAuthority('COTIZACIONES_EDITAR') and hasAuthority('COMPROBANTES_CREAR')")
    public ResponseEntity<BoletaResponse> convertirEnBoleta(@PathVariable Integer id,
                                                            @AuthenticationPrincipal UsuarioPrincipal actor) {
        BoletaResponse r = service.convertirEnBoleta(id, actor);
        HttpStatus status = r.isSuccess() ? HttpStatus.OK
                : "COMMUNICATION_ERROR".equals(r.getCodigoRespuestaSunat()) ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(r);
    }
}
