package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Integer id);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Integer id);

    Optional<Cliente> findFirstByDni(String dni);

    Optional<Cliente> findFirstByRuc(String ruc);

    long countByActivo(boolean activo);
}
