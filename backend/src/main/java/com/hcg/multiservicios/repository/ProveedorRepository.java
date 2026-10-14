package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer>, JpaSpecificationExecutor<Proveedor> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Integer id);
}
