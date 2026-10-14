package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.security.AuditoriaDto;
import com.hcg.multiservicios.service.AuditoriaService;
import com.hcg.multiservicios.dto.PageResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDITORIA_VER')")
    public List<AuditoriaDto> recientes(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return auditoriaService.recientes(page, size);
    }

    /** Historial de actividad filtrable (por ejemplo, todo lo que hizo un empleado en un rango de fechas). */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('AUDITORIA_VER')")
    public PageResponse<AuditoriaDto> buscar(@RequestParam(required = false) String username,
                                             @RequestParam(required = false) String accion,
                                             @RequestParam(required = false) String entidad,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "30") int size) {
        return auditoriaService.buscar(username, accion, entidad, desde, hasta, page, size);
    }
}
