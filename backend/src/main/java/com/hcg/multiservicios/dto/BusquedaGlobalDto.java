package com.hcg.multiservicios.dto;

import com.hcg.multiservicios.dto.catalogo.ClienteDto;
import com.hcg.multiservicios.dto.catalogo.ProductoDto;
import com.hcg.multiservicios.dto.catalogo.ProveedorDto;
import java.util.List;

/** Resultados agrupados; una seccion viene vacia si el usuario no tiene permiso para verla. */
public record BusquedaGlobalDto(String q, List<ClienteDto> clientes, List<ProveedorDto> proveedores,
                                List<ProductoDto> productos, List<CotizacionDto> cotizaciones,
                                List<DocumentoDto> documentos, List<ComprobanteDto> boletas) {
}
