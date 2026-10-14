package com.hcg.multiservicios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Comprobante guardado. El XML y el CDR no se incluyen (se descargan aparte); detalles solo en la consulta individual. */
public record ComprobanteDto(Long id, String tipoDoc, String serie, String correlativo, LocalDateTime fechaEmision,
                             String moneda, Integer clienteId, String clienteTipoDoc, String clienteNumDoc,
                             String clienteNombre, String clienteDireccion, BigDecimal mtoOperGravadas,
                             BigDecimal mtoIgv, BigDecimal mtoImpVenta, String observacion, String estado,
                             String codigoSunat, String descripcionSunat, String hash, Long usuarioId,
                             LocalDateTime creadoEn, List<Detalle> detalles) {

    public record Detalle(int item, String codProducto, String descripcion, String unidad, BigDecimal cantidad,
                          BigDecimal valorUnitario, BigDecimal precioUnitario, BigDecimal valorVenta, BigDecimal igv) {
    }
}
