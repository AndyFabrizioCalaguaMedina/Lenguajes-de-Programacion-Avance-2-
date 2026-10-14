package com.hcg.multiservicios.dto.catalogo;

import java.util.List;

public record ProductoImportResultado(int total, int creados, int actualizados, List<Error> errores) {

    public record Error(Integer fila, String codigo, String mensaje) {
    }
}
