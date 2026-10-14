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
@Table(name = "detalle_cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class CotizacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cotizacion", nullable = false)
    private Cotizacion cotizacion;

    private Integer item;

    @Column(name = "id_producto")
    private Integer productoId;

    @Column(name = "producto", length = 255)
    private String descripcion;

    /** Codigo de unidad SUNAT (NIU, ZZ...). Reutiliza la columna antigua "unidades". */
    @Column(name = "unidades", length = 30)
    private String unidad;

    @Column(name = "cantidad_num", precision = 12, scale = 2)
    private BigDecimal cantidad;

    /** Precio unitario con IGV. */
    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;
}
