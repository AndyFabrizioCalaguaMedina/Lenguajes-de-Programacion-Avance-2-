package com.hcg.multiservicios.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Eventos solo en la consulta individual. */
public record OperacionDto(Long id, String codigo, String tipo, String titulo, String descripcion, Integer clienteId,
                           Integer proveedorId, String contraparteNombre, String estado, BigDecimal monto,
                           LocalDate fechaInicio, LocalDate fechaCierre, Long usuarioId, LocalDateTime creadoEn,
                           LocalDateTime actualizadoEn, List<Evento> eventos) {

    public record Evento(Long id, String tipo, String titulo, String detalle, String referencia, LocalDateTime fecha,
                         Long usuarioId) {
    }

    /** Totales para las tarjetas de la pantalla de operaciones. */
    public record Resumen(long total, long abiertas, long enProceso, long completadas, long canceladas, long ventas,
                          long compras, long pedidos) {
    }
}
