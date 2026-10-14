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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "salida_bodega_detalle")
@Getter
@Setter
@NoArgsConstructor
public class SalidaBodegaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salida_id", nullable = false)
    private SalidaBodega salida;

    @Column(nullable = false)
    private int item;

    @Column(name = "producto_id", nullable = false)
    private Integer productoId;

    @Column(length = 30)
    private String codigo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private int stockAnterior;

    @Column(name = "stock_posterior", nullable = false)
    private int stockPosterior;
}
