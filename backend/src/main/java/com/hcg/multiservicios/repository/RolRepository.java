package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Rol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
