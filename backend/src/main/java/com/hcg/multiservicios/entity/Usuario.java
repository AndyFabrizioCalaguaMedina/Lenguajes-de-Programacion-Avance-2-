package com.hcg.multiservicios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en minusculas. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    /** Siempre en minusculas. */
    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    public boolean esAdmin() {
        return roles.stream().anyMatch(r -> Rol.ADMIN.equals(r.getNombre()));
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }
}
