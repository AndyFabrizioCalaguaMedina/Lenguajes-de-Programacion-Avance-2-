package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(name = "valor_anterior", length = 2000)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 2000)
    private String valorNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
