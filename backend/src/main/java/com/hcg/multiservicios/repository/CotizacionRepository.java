package com.hcg.multiservicios.repository;

import java.util.List;
import java.time.LocalDate;
import com.hcg.multiservicios.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer>, JpaSpecificationExecutor<Cotizacion> {

    List<Cotizacion> findByFechaBetween(LocalDate desde, LocalDate hasta);

    long countByEstado(String estado);
}
