package com.hcg.multiservicios.controller;

import jakarta.validation.Valid;
import com.hcg.multiservicios.dto.ComprobanteDto;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.dto.apisperu.EmitirBoletaRequest;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.ComprobanteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/** Boletas electronicas: requieren token y permisos COMPROBANTES_*. Todo comprobante emitido queda guardado. */
@RestController
@RequestMapping("/api/v1/boletas")
public class BoletaController {

    private final ComprobanteService service;

    public BoletaController(ComprobanteService service) {
        this.service = service;
    }

    @PostMapping("/emitir")
    @PreAuthorize("hasAuthority('COMPROBANTES_CREAR')")
    public ResponseEntity<BoletaResponse> emitir(@Valid @RequestBody EmitirBoletaRequest request,
                                                 @AuthenticationPrincipal UsuarioPrincipal actor) {
        BoletaResponse response = service.emitirBoleta(request, actor);
        return ResponseEntity.status(estadoHttp(response)).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COMPROBANTES_VER')")
    public PageResponse<ComprobanteDto> listar(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) String estado,
                                               @RequestParam(required = false) String tipo,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.listar(q, estado, tipo, page, size);
    }

    @GetMapping("/{serie}-{correlativo}")
    @PreAuthorize("hasAuthority('COMPROBANTES_VER')")
    public ComprobanteDto obtener(@PathVariable String serie, @PathVariable int correlativo) {
        return service.obtener(serie, correlativo);
    }

    @GetMapping(value = "/{serie}-{correlativo}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('COMPROBANTES_VER')")
    public String xml(@PathVariable String serie, @PathVariable int correlativo) {
        return service.xml(serie, correlativo);
    }

    @GetMapping(value = "/{serie}-{correlativo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('COMPROBANTES_VER')")
    public ResponseEntity<byte[]> pdf(@PathVariable String serie, @PathVariable int correlativo) {
        byte[] pdf = service.pdf(serie, correlativo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + serie + "-" + correlativo + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{serie}-{correlativo}/reenviar")
    @PreAuthorize("hasAuthority('COMPROBANTES_CREAR')")
    public ResponseEntity<BoletaResponse> reenviar(@PathVariable String serie, @PathVariable int correlativo,
                                                   @AuthenticationPrincipal UsuarioPrincipal actor) {
        BoletaResponse response = service.reenviar(serie, correlativo, actor);
        return ResponseEntity.status(estadoHttp(response)).body(response);
    }

    private static HttpStatus estadoHttp(BoletaResponse r) {
        if (r.isSuccess()) return HttpStatus.OK;
        return "COMMUNICATION_ERROR".equals(r.getCodigoRespuestaSunat()) ? HttpStatus.BAD_GATEWAY : HttpStatus.BAD_REQUEST;
    }
}

