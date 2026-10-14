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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comprobante_detalle")
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(nullable = false)
    private int item;

    @Column(name = "cod_producto", length = 30)
    private String codProducto;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 5)
    private String unidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "valor_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorVenta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;
}
