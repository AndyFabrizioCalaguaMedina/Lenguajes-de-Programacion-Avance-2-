package com.hcg.multiservicios.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle solo en la consulta individual. */
public record CotizacionDto(Integer id, String numero, Integer clienteId, String clienteNombre, String telefono,
                            LocalDate fecha, LocalDate vigenciaHasta, String estado, String moneda,
                            BigDecimal subtotal, BigDecimal igv, BigDecimal total, String observacion,
                            Long comprobanteId, Long usuarioId, LocalDateTime creadoEn, List<Item> items) {

    public record Item(int item, Integer productoId, String descripcion, String unidad, BigDecimal cantidad,
                       BigDecimal precioUnitario, BigDecimal subtotal) {
    }
}
