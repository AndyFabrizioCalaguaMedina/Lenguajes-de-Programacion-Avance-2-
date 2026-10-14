package com.hcg.multiservicios.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Salida de productos de la bodega (envio a proveedor, devolucion, venta, merma...). */
@Entity
@Table(name = "salida_bodega")
@Getter
@Setter
@NoArgsConstructor
public class SalidaBodega {

    public static final String REGISTRADA = "REGISTRADA";
    public static final String ANULADA = "ANULADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "destino_tipo", nullable = false, length = 20)
    private String destinoTipo;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "destino_nombre", length = 255)
    private String destinoNombre;

    @Column(nullable = false, length = 20)
    private String motivo;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false, length = 20)
    private String estado = REGISTRADA;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @OneToMany(mappedBy = "salida", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("item ASC")
    private List<SalidaBodegaDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(SalidaBodegaDetalle d) {
        d.setSalida(this);
        detalles.add(d);
    }
}
