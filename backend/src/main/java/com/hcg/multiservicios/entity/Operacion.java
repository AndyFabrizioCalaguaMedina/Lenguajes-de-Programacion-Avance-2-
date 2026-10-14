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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Operacion comercial (venta, compra o pedido) con su linea de tiempo de eventos. */
@Entity
@Table(name = "operacion")
@Getter
@Setter
@NoArgsConstructor
public class Operacion {

    public static final String VENTA = "VENTA";
    public static final String COMPRA = "COMPRA";
    public static final String PEDIDO = "PEDIDO";

    public static final String ABIERTA = "ABIERTA";
    public static final String EN_PROCESO = "EN_PROCESO";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String codigo;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @Column(name = "contraparte_nombre", nullable = false, length = 255)
    private String contraparteNombre;

    @Column(nullable = false, length = 20)
    private String estado = ABIERTA;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio = LocalDate.now();

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "operacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fecha ASC, id ASC")
    private List<OperacionEvento> eventos = new ArrayList<>();

    public void agregarEvento(OperacionEvento e) {
        e.setOperacion(this);
        eventos.add(e);
    }

    public boolean cerrada() {
        return COMPLETADA.equals(estado) || CANCELADA.equals(estado);
    }
}
