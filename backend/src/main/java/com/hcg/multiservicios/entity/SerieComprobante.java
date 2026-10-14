package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Serie autorizada y ultimo correlativo emitido. Se lee con bloqueo de fila para no repetir numeros. */
@Entity
@Table(name = "serie_comprobante")
@Getter
@Setter
@NoArgsConstructor
public class SerieComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tipo_doc", nullable = false, length = 2)
    private String tipoDoc;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(name = "ultimo_correlativo", nullable = false)
    private int ultimoCorrelativo;

    @Column(nullable = false)
    private boolean activo = true;
}
