package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.security.AuditoriaDto;
import com.hcg.multiservicios.entity.Auditoria;
import com.hcg.multiservicios.repository.AuditoriaRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de trazabilidad. Nunca recibir contraseñas ni tokens en los valores. */
@Service
public class AuditoriaService {

    private static final int MAX_TEXTO = 2000;

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(Long usuarioId, String username, String accion, String entidad, Object entidadId,
                          String valorAnterior, String valorNuevo) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(usuarioId);
        a.setUsername(username);
        a.setAccion(accion);
        a.setEntidad(entidad);
        a.setEntidadId(entidadId == null ? null : String.valueOf(entidadId));
        a.setValorAnterior(recortar(valorAnterior));
        a.setValorNuevo(recortar(valorNuevo));
        repository.save(a);
    }

    @Transactional
    public void registrar(UsuarioPrincipal actor, String accion, String entidad, Object entidadId,
                          String valorAnterior, String valorNuevo) {
        registrar(actor == null ? null : actor.id(), actor == null ? null : actor.username(),
                accion, entidad, entidadId, valorAnterior, valorNuevo);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDto> recientes(int pagina, int tamano) {
        int size = Math.min(Math.max(tamano, 1), 200);
        return repository.findAllByOrderByFechaDescIdDesc(PageRequest.of(Math.max(pagina, 0), size)).getContent().stream()
                .map(a -> new AuditoriaDto(a.getId(), a.getUsuarioId(), a.getUsername(), a.getAccion(), a.getEntidad(),
                        a.getEntidadId(), a.getValorAnterior(), a.getValorNuevo(), a.getFecha()))
                .toList();
    }

    /** Historial filtrable: por usuario, accion, entidad y rango de fechas (mas reciente primero). */
    @Transactional(readOnly = true)
    public PageResponse<AuditoriaDto> buscar(String username, String accion, String entidad, LocalDate desde,
                                             LocalDate hasta, int page, int size) {
        String u = Texto.limpio(username);
        String a = Texto.limpio(accion);
        String e = Texto.limpio(entidad);
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (u != null) ps.add(cb.equal(cb.lower(root.<String>get("username")), u.toLowerCase()));
            if (a != null) ps.add(cb.equal(root.get("accion"), a.toUpperCase()));
            if (e != null) ps.add(cb.equal(root.get("entidad"), e));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.<java.time.LocalDateTime>get("fecha"), desde.atStartOfDay()));
            if (hasta != null) ps.add(cb.lessThanOrEqualTo(root.<java.time.LocalDateTime>get("fecha"), hasta.atTime(LocalTime.MAX)));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fecha").descending().and(Sort.by("id").descending()));
        return PageResponse.of(repository.findAll(spec, pageable).map(x -> new AuditoriaDto(x.getId(), x.getUsuarioId(),
                x.getUsername(), x.getAccion(), x.getEntidad(), x.getEntidadId(), x.getValorAnterior(),
                x.getValorNuevo(), x.getFecha())));
    }

    private String recortar(String s) {
        return s != null && s.length() > MAX_TEXTO ? s.substring(0, MAX_TEXTO) : s;
    }
}
