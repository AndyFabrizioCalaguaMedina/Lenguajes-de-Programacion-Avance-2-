package com.hcg.multiservicios.dto.catalogo;

import java.time.LocalDateTime;

public record ClienteDto(Integer id, String codigo, String tipoDocumento, String numeroDocumento, String ruc, String dni,
                         String razonSocial, String nombreComercial, String nombres, String apellidos, String nombre,
                         String direccion, String telefono, String correo, boolean activo, LocalDateTime fechaRegistro) {
}
