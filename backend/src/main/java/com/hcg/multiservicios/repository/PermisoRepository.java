package com.hcg.multiservicios.repository;

import com.hcg.multiservicios.entity.Permiso;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findAllByOrderByModuloAscAccionAsc();
}
