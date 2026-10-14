package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.catalogo.ProveedorDto;
import com.hcg.multiservicios.dto.catalogo.ProveedorRequest;
import com.hcg.multiservicios.entity.Proveedor;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.ProveedorRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

    private final ProveedorRepository repository;
    private final AuditoriaService auditoria;

    public ProveedorService(ProveedorRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProveedorDto> buscar(String q, Boolean activo, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Proveedor> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                String[] campos = {"nombre", "ruc", "razonSocial", "contacto"};
                Predicate[] ors = new Predicate[campos.length];
                for (int i = 0; i < campos.length; i++) {
                    ors[i] = cb.like(cb.lower(root.<String>get(campos[i])), p, '\\');
                }
                ps.add(cb.or(ors));
            }
            if (activo != null) {
                ps.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("nombre").ascending().and(Sort.by("id")));
        return PageResponse.of(repository.findAll(spec, pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ProveedorDto obtener(Integer id) {
        return toDto(buscarPorId(id));
    }

    @Transactional
    public ProveedorDto crear(ProveedorRequest req, UsuarioPrincipal actor) {
        Proveedor p = new Proveedor();
        aplicar(p, req);
        if (repository.existsByRuc(p.getRuc())) {
            throw BusinessException.conflict("DUPLICATE_RUC", "Ya existe un proveedor con ese RUC.");
        }
        p = repository.save(p);
        auditoria.registrar(actor, "PROVEEDOR_CREADO", "Proveedor", p.getId(), null, resumen(p));
        return toDto(p);
    }

    @Transactional
    public ProveedorDto actualizar(Integer id, ProveedorRequest req, UsuarioPrincipal actor) {
        Proveedor p = buscarPorId(id);
        String antes = resumen(p);
        aplicar(p, req);
        if (repository.existsByRucAndIdNot(p.getRuc(), id)) {
            throw BusinessException.conflict("DUPLICATE_RUC", "Ya existe un proveedor con ese RUC.");
        }
        repository.save(p);
        auditoria.registrar(actor, "PROVEEDOR_EDITADO", "Proveedor", id, antes, resumen(p));
        return toDto(p);
    }

    @Transactional
    public ProveedorDto cambiarEstado(Integer id, boolean activo, UsuarioPrincipal actor) {
        Proveedor p = buscarPorId(id);
        boolean antes = p.isActivo();
        p.setActivo(activo);
        repository.save(p);
        auditoria.registrar(actor, activo ? "PROVEEDOR_ACTIVADO" : "PROVEEDOR_DESACTIVADO", "Proveedor", id,
                "activo=" + antes, "activo=" + activo);
        return toDto(p);
    }

    private Proveedor buscarPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PROVIDER_NOT_FOUND", "Proveedor no encontrado."));
    }

    private void aplicar(Proveedor p, ProveedorRequest r) {
        p.setRuc(r.ruc().trim());
        p.setRazonSocial(Texto.limpio(r.razonSocial()));
        String comercial = Texto.limpio(r.nombreComercial());
        p.setNombre(comercial != null ? comercial : p.getRazonSocial());
        p.setDireccion(Texto.limpio(r.direccion()));
        p.setTelefono1(Texto.limpio(r.telefono1()));
        p.setTelefono2(Texto.limpio(r.telefono2()));
        String correo = Texto.limpio(r.correo());
        p.setCorreo(correo == null ? null : correo.toLowerCase(Locale.ROOT));
        p.setContacto(Texto.limpio(r.contacto()));
    }

    private String resumen(Proveedor p) {
        return "ruc=" + p.getRuc() + ", razonSocial=" + p.getRazonSocial();
    }

    private ProveedorDto toDto(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getRuc(), p.getRazonSocial(), p.getNombre(), p.getNombre(),
                p.getDireccion(), p.getTelefono1(), p.getTelefono2(), p.getCorreo(), p.getContacto(),
                p.isActivo(), p.getFechaRegistro());
    }
}
