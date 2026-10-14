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

/** Tabla existente del diagrama (cotizacion) + columnas de V5. Las columnas antiguas (presupuesto...) no se mapean ni se tocan. */
@Entity
@Table(name = "cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class Cotizacion {

    public static final String BORRADOR = "BORRADOR";
    public static final String ENVIADA = "ENVIADA";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String FACTURADA = "FACTURADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 20)
    private String numero;

    @Column(name = "id_cliente")
    private Integer clienteId;

    @Column(name = "cliente", length = 255)
    private String clienteNombre;

    private LocalDate fecha;

    @Column(length = 30)
    private String telefono;

    @Column(length = 30)
    private String estado = BORRADOR;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(length = 500)
    private String observacion;

    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Boleta generada a partir de esta cotizacion (tabla comprobante). */
    @Column(name = "comprobante_id")
    private Long comprobanteId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("item ASC")
    private List<CotizacionDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(CotizacionDetalle d) {
        d.setCotizacion(this);
        detalles.add(d);
    }
}
