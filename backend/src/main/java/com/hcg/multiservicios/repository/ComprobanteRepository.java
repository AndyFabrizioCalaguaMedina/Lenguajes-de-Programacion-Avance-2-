package com.hcg.multiservicios.repository;

import java.util.List;
import java.time.LocalDateTime;
import com.hcg.multiservicios.entity.Comprobante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long>, JpaSpecificationExecutor<Comprobante> {

    Optional<Comprobante> findByTipoDocAndSerieAndCorrelativo(String tipoDoc, String serie, int correlativo);

    Optional<Comprobante> findFirstBySerieAndCorrelativo(String serie, int correlativo);

    List<Comprobante> findByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta);
}
