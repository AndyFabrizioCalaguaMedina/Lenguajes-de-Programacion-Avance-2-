package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.DocumentoDto;
import com.hcg.multiservicios.dto.DocumentoRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.DocumentoService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Registro central de documentos. Las boletas y cotizaciones aparecen solas; el resto se registra aquí. */
@RestController
@RequestMapping("/api/documents")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public PageResponse<DocumentoDto> buscar(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String tipo,
                                             @RequestParam(required = false) String estado,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.buscar(q, tipo, estado, desde, hasta, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public DocumentoDto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENTOS_CREAR')")
    public ResponseEntity<DocumentoDto> crear(@Valid @RequestBody DocumentoRequest request,
                                              @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_EDITAR')")
    public DocumentoDto actualizar(@PathVariable Long id, @Valid @RequestBody DocumentoRequest request,
                                   @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.actualizar(id, request, actor);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('DOCUMENTOS_ANULAR')")
    public DocumentoDto anular(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal actor) {
        return service.anular(id, actor);
    }
}
