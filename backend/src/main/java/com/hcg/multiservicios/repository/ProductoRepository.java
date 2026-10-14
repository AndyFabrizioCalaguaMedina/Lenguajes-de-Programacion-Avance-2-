package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Producto;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Integer id);

    Optional<Producto> findByIdAndTipo(Integer id, String tipo);

    Optional<Producto> findByCodigo(String codigo);

    /** Bloqueo de escritura para descontar o devolver stock sin carreras entre dos salidas simultaneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> bloquear(@Param("id") Integer id);
}
