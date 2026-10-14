package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameOrCorreo(String username, String correo);

    boolean existsByUsername(String username);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    @Query("select count(distinct u) from Usuario u join u.roles r where r.nombre = :rol and u.activo = true")
    long contarActivosConRol(@Param("rol") String rol);
}
