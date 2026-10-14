package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.EstadisticasDto;
import com.hcg.multiservicios.service.EstadisticasService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class EstadisticasController {

    private final EstadisticasService service;

    public EstadisticasController(EstadisticasService service) {
        this.service = service;
    }

    /** La actividad reciente solo se incluye si el usuario puede ver la auditoria. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DASHBOARD_VER')")
    public EstadisticasDto.Dashboard dashboard(Authentication auth) {
        return service.dashboard(puede(auth, "AUDITORIA_VER"));
    }

    @GetMapping("/reportes")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    public EstadisticasDto.Reportes reportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication auth) {
        return service.reportes(desde, hasta, puede(auth, "AUDITORIA_VER"));
    }

    static boolean puede(Authentication auth, String permiso) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permiso));
    }
}
