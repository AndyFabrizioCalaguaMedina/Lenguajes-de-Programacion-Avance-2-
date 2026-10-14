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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comprobante")
@Getter
@Setter
@NoArgsConstructor
public class Comprobante {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";
    /** Fallo de comunicacion: se puede reenviar con el mismo numero. */
    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_doc", nullable = false, length = 2)
    private String tipoDoc;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private int correlativo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "cliente_tipo_doc", length = 2)
    private String clienteTipoDoc;

    @Column(name = "cliente_num_doc", length = 20)
    private String clienteNumDoc;

    @Column(name = "cliente_nombre", length = 255)
    private String clienteNombre;

    @Column(name = "cliente_direccion", length = 255)
    private String clienteDireccion;

    @Column(name = "mto_oper_gravadas", nullable = false, precision = 12, scale = 2)
    private BigDecimal mtoOperGravadas;

    @Column(name = "mto_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal mtoIgv;

    @Column(name = "mto_imp_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal mtoImpVenta;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(name = "codigo_sunat", length = 60)
    private String codigoSunat;

    @Column(name = "descripcion_sunat", length = 1000)
    private String descripcionSunat;

    @Column(length = 100)
    private String hash;

    @Column(length = 1000000)
    private String xml;

    @Column(name = "cdr_zip", length = 1000000)
    private String cdrZip;

    /** Solicitud original (con serie, correlativo y fecha ya asignados) para poder reenviar identica. */
    @Column(name = "request_json", length = 100000)
    private String requestJson;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("item ASC")
    private List<ComprobanteDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(ComprobanteDetalle d) {
        d.setComprobante(this);
        detalles.add(d);
    }
}
