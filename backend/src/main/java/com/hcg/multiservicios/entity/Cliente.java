package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla existente del diagrama (cliente) + columnas de V3. Las columnas no mapeadas (pedidos_anuales) se conservan intactas. */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    public static final String TIPO_RUC = "RUC";
    public static final String TIPO_DNI = "DNI";
    public static final String TIPO_SIN_DOCUMENTO = "SIN_DOCUMENTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 20)
    private String codigo;

    /** Nombre para mostrar: razon social, o nombres + apellidos, o nombre comercial. */
    @Column(length = 255)
    private String nombre;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(length = 20)
    private String ruc;

    @Column(length = 8)
    private String dni;

    @Column(name = "razon_social", length = 255)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 255)
    private String nombreComercial;

    @Column(length = 150)
    private String nombres;

    @Column(length = 150)
    private String apellidos;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
