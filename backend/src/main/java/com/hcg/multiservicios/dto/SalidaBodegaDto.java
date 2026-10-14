package com.hcg.multiservicios.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Items solo en la consulta individual y al registrar. */
public record SalidaBodegaDto(Long id, String numero, LocalDateTime fecha, String destinoTipo, Integer proveedorId,
                              Integer clienteId, String destinoNombre, String motivo, String observacion,
                              String estado, Long usuarioId, LocalDateTime creadoEn, LocalDateTime anuladoEn,
                              List<Item> items) {

    public record Item(int item, Integer productoId, String codigo, String descripcion, int cantidad,
                       int stockAnterior, int stockPosterior) {
    }
}
