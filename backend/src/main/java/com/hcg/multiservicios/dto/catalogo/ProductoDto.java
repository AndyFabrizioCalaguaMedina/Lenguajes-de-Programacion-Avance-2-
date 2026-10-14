package com.hcg.multiservicios.dto.catalogo;

import java.math.BigDecimal;

public record ProductoDto(Integer id, String codigo, String tipo, String nombre, String descripcion, String categoria,
                          BigDecimal precioVenta, BigDecimal precioCompra, Integer stock, Integer stockMinimo,
                          String unidadMedida, boolean activo, boolean stockBajo) {
}
