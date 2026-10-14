package com.hcg.multiservicios.controller;

import com.hcg.multiservicios.dto.BusquedaGlobalDto;
import com.hcg.multiservicios.entity.Producto;
import com.hcg.multiservicios.service.ClienteService;
import com.hcg.multiservicios.service.ComprobanteService;
import com.hcg.multiservicios.service.CotizacionService;
import com.hcg.multiservicios.service.DocumentoService;
import com.hcg.multiservicios.service.ProductoService;
import com.hcg.multiservicios.service.ProveedorService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Busqueda global: cada seccion respeta los permisos *_VER del usuario. */
@RestController
@RequestMapping("/api/search")
public class BusquedaController {

    private static final int POR_SECCION = 5;

    private final ClienteService clientes;
    private final ProveedorService proveedores;
    private final ProductoService productos;
    private final CotizacionService cotizaciones;
    private final DocumentoService documentos;
    private final ComprobanteService comprobantes;

    public BusquedaController(ClienteService clientes, ProveedorService proveedores, ProductoService productos,
                              CotizacionService cotizaciones, DocumentoService documentos,
                              ComprobanteService comprobantes) {
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.productos = productos;
        this.cotizaciones = cotizaciones;
        this.documentos = documentos;
        this.comprobantes = comprobantes;
    }

    @GetMapping
    public BusquedaGlobalDto buscar(@RequestParam String q, Authentication auth) {
        String texto = q == null ? "" : q.trim();
        if (texto.length() < 2) {
            return new BusquedaGlobalDto(texto, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
        return new BusquedaGlobalDto(texto,
                EstadisticasController.puede(auth, "CLIENTES_VER")
                        ? clientes.buscar(texto, null, 0, POR_SECCION).content() : List.of(),
                EstadisticasController.puede(auth, "PROVEEDORES_VER")
                        ? proveedores.buscar(texto, null, 0, POR_SECCION).content() : List.of(),
                EstadisticasController.puede(auth, "PRODUCTOS_VER")
                        ? productos.buscar(Producto.TIPO_PRODUCTO, texto, null, 0, POR_SECCION).content() : List.of(),
                EstadisticasController.puede(auth, "COTIZACIONES_VER")
                        ? cotizaciones.buscar(texto, null, 0, POR_SECCION).content() : List.of(),
                EstadisticasController.puede(auth, "DOCUMENTOS_VER")
                        ? documentos.buscar(texto, null, null, null, null, 0, POR_SECCION).content() : List.of(),
                EstadisticasController.puede(auth, "COMPROBANTES_VER")
                        ? comprobantes.listar(texto, null, null, 0, POR_SECCION).content() : List.of());
    }
}
