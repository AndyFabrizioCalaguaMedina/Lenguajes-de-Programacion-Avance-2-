package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Operacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperacionRepository extends JpaRepository<Operacion, Long>, JpaSpecificationExecutor<Operacion> {

    long countByEstado(String estado);

    long countByTipo(String tipo);
}
