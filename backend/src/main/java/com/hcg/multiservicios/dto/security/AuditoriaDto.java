package com.hcg.multiservicios.dto.security;

import java.time.LocalDateTime;

public record AuditoriaDto(Long id, Long usuarioId, String username, String accion, String entidad,
                           String entidadId, String valorAnterior, String valorNuevo, LocalDateTime fecha) {
}
