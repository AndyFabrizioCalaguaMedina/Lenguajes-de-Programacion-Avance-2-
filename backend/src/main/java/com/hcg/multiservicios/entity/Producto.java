package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catalogo unico: productos y servicios (columna tipo). Tabla existente del diagrama (producto) + columnas de V3.
 * La columna legacy "precio" (FLOAT) no se mapea; el codigo nuevo usa precio_venta NUMERIC.
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    public static final String TIPO_PRODUCTO = "PRODUCTO";
    public static final String TIPO_SERVICIO = "SERVICIO";
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_INACTIVO = "INACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 10)
    private String tipo = TIPO_PRODUCTO;

    @Column(length = 255)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 80)
    private String categoria;

    @Column(length = 30)
    private String estado = ESTADO_ACTIVO;

    @Column(name = "precio_venta", precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "precio_compra", precision = 12, scale = 2)
    private BigDecimal precioCompra;

    private Integer stock;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    public boolean esActivo() {
        return ESTADO_ACTIVO.equalsIgnoreCase(estado);
    }
}
