package com.hcg.multiservicios.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoDto(Long id, String tipo, String numero, LocalDate fecha, String origen, String origenId,
                           String contraparteNombre, String contraparteDoc, BigDecimal monto, String moneda,
                           String estado, String descripcion, String archivoUrl, Long usuarioId,
                           LocalDateTime creadoEn) {
}
