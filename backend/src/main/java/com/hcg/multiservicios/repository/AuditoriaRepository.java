package com.hcg.multiservicios.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.time.LocalDateTime;
import com.hcg.multiservicios.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    Page<Auditoria> findAllByOrderByFechaDescIdDesc(Pageable pageable);

    List<Auditoria> findByFechaBetween(LocalDateTime desde, LocalDateTime hasta);
}
