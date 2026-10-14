package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.SerieComprobante;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Integer> {

    /** Bloqueo de escritura: dos emisiones simultaneas nunca obtienen el mismo correlativo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SerieComprobante s where s.tipoDoc = :tipoDoc and s.serie = :serie and s.activo = true")
    Optional<SerieComprobante> bloquear(@Param("tipoDoc") String tipoDoc, @Param("serie") String serie);
}
