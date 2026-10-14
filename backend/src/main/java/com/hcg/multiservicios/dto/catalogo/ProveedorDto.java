package com.hcg.multiservicios.dto.catalogo;

import java.time.LocalDateTime;

public record ProveedorDto(Integer id, String ruc, String razonSocial, String nombreComercial, String nombre,
                           String direccion, String telefono1, String telefono2, String correo, String contacto,
                           boolean activo, LocalDateTime fechaRegistro) {
}
