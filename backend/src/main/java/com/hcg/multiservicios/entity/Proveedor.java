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

/** Tabla existente del diagrama (proveedores) + columnas de V3. */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nombre comercial (o razon social si no se indica). */
    @Column(length = 255)
    private String nombre;

    @Column(length = 20)
    private String ruc;

    @Column(name = "razon_social", length = 255)
    private String razonSocial;

    @Column(length = 255)
    private String direccion;

    @Column(name = "telefono_1", length = 30)
    private String telefono1;

    @Column(name = "telefono_2", length = 30)
    private String telefono2;

    @Column(length = 150)
    private String correo;

    @Column(length = 150)
    private String contacto;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
