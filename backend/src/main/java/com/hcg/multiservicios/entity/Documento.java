package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro central de documentos de la empresa (boletas, facturas, cotizaciones, ordenes de compra, etc.). */
@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    public static final String ORIGEN_COMPROBANTE = "COMPROBANTE";
    public static final String ORIGEN_COTIZACION = "COTIZACION";
    public static final String ORIGEN_MANUAL = "MANUAL";
    public static final String ANULADO = "ANULADO";
    public static final String REGISTRADO = "REGISTRADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 40)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    /** Quien lo genero: COMPROBANTE / COTIZACION (automatico, sigue el estado de su origen) o MANUAL. */
    @Column(nullable = false, length = 20)
    private String origen = ORIGEN_MANUAL;

    @Column(name = "origen_id", length = 40)
    private String origenId;

    @Column(name = "contraparte_nombre", length = 255)
    private String contraparteNombre;

    @Column(name = "contraparte_doc", length = 20)
    private String contraparteDoc;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(nullable = false, length = 20)
    private String estado = REGISTRADO;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
