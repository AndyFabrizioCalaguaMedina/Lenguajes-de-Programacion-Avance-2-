package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Documento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    Optional<Documento> findByTipoAndNumero(String tipo, String numero);
}
