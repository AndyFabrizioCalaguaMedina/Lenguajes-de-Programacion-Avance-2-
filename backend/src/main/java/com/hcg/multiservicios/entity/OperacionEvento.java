package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Paso de la linea de tiempo de una operacion (solicitud, cotizacion, envio, comprobante...). */
@Entity
@Table(name = "operacion_evento")
@Getter
@Setter
@NoArgsConstructor
public class OperacionEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String detalle;

    @Column(length = 60)
    private String referencia;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "usuario_id")
    private Long usuarioId;
}
