package com.hcg.multiservicios.repository;

import java.util.List;
import java.time.LocalDateTime;
import com.hcg.multiservicios.entity.SalidaBodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalidaBodegaRepository extends JpaRepository<SalidaBodega, Long>, JpaSpecificationExecutor<SalidaBodega> {

    List<SalidaBodega> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
