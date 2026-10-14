package com.hcg.multiservicios.dto.catalogo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Carga masiva de productos (el frontend lee el Excel y envía las filas). Cada fila lleva su número en el archivo
 * para poder informar el error exacto. Si actualizarExistentes es true, un código que ya existe se actualiza.
 */
public record ProductoImportRequest(
        @NotEmpty(message = "El archivo no tiene filas") @Size(max = 1000, message = "Máximo 1000 filas por carga")
        List<Fila> filas,
        boolean actualizarExistentes) {

    public record Fila(Integer fila, ProductoRequest producto) {
    }
}
