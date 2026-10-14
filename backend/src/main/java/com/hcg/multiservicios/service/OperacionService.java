package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.OperacionDto;
import com.hcg.multiservicios.dto.OperacionEventoRequest;
import com.hcg.multiservicios.dto.OperacionRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.entity.Cliente;
import com.hcg.multiservicios.entity.Operacion;
import com.hcg.multiservicios.entity.OperacionEvento;
import com.hcg.multiservicios.entity.Proveedor;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.exception.FieldValidationException;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.repository.OperacionRepository;
import com.hcg.multiservicios.repository.ProveedorRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones comerciales (ventas, compras y pedidos) con linea de tiempo.
 * Flujo: ABIERTA -> EN_PROCESO -> COMPLETADA | CANCELADA. Una operacion cerrada ya no se edita ni recibe eventos.
 */
@Service
public class OperacionService {

    private final OperacionRepository repository;
    private final ClienteRepository clientes;
    private final ProveedorRepository proveedores;
    private final AuditoriaService auditoria;

    public OperacionService(OperacionRepository repository, ClienteRepository clientes,
                            ProveedorRepository proveedores, AuditoriaService auditoria) {
        this.repository = repository;
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.auditoria = auditoria;
    }

    @Transactional
    public OperacionDto crear(OperacionRequest r, UsuarioPrincipal actor) {
        Operacion o = new Operacion();
        o.setUsuarioId(actor == null ? null : actor.id());
        aplicar(o, r);
        o = repository.save(o);
        o.setCodigo(String.format("OP-%d-%04d", o.getFechaInicio().getYear(), o.getId()));

        OperacionEvento e = evento("NOTA", "Operación registrada", Texto.limpio(r.descripcion()), null, actor);
        o.agregarEvento(e);
        o = repository.save(o);
        auditoria.registrar(actor, "OPERACION_CREADA", "operacion", o.getId(), null, o.getCodigo() + " " + o.getTipo());
        return toDto(o, true);
    }

    @Transactional
    public OperacionDto actualizar(Long id, OperacionRequest r, UsuarioPrincipal actor) {
        Operacion o = buscar(id);
        exigirAbierta(o);
        aplicar(o, r);
        o.setActualizadoEn(LocalDateTime.now());
        auditoria.registrar(actor, "OPERACION_EDITADA", "operacion", id, null, o.getCodigo());
        return toDto(repository.save(o), true);
    }

    @Transactional
    public OperacionDto agregarEvento(Long id, OperacionEventoRequest r, UsuarioPrincipal actor) {
        Operacion o = buscar(id);
        exigirAbierta(o);
        o.agregarEvento(evento(r.tipo(), r.titulo().trim(), Texto.limpio(r.detalle()), Texto.limpio(r.referencia()), actor));
        if (Operacion.ABIERTA.equals(o.getEstado())) {
            o.setEstado(Operacion.EN_PROCESO); // en cuanto hay actividad, la operacion esta en proceso
        }
        o.setActualizadoEn(LocalDateTime.now());
        auditoria.registrar(actor, "OPERACION_EVENTO", "operacion", id, null, r.tipo() + ": " + r.titulo());
        return toDto(repository.save(o), true);
    }

    @Transactional
    public OperacionDto cambiarEstado(Long id, String nuevo, UsuarioPrincipal actor) {
        Operacion o = buscar(id);
        exigirAbierta(o);
        String actual = o.getEstado();
        if (actual.equals(nuevo)) {
            throw BusinessException.conflict("TRANSICION_INVALIDA", "La operación ya está en " + actual + ".");
        }
        o.setEstado(nuevo);
        if (o.cerrada()) o.setFechaCierre(LocalDate.now());
        o.setActualizadoEn(LocalDateTime.now());
        String titulo = switch (nuevo) {
            case Operacion.COMPLETADA -> "Operación completada";
            case Operacion.CANCELADA -> "Operación cancelada";
            default -> "Operación en proceso";
        };
        o.agregarEvento(evento("NOTA", titulo, null, null, actor));
        auditoria.registrar(actor, "OPERACION_ESTADO", "operacion", id, actual, nuevo);
        return toDto(repository.save(o), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<OperacionDto> buscar(String q, String tipo, String estado, Integer proveedorId,
                                             Integer clienteId, LocalDate desde, LocalDate hasta, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Operacion> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.<String>get("codigo")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("titulo")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("contraparteNombre")), p, '\\')));
            }
            if (tipo != null && !tipo.isBlank()) ps.add(cb.equal(root.get("tipo"), tipo.trim().toUpperCase(Locale.ROOT)));
            if (estado != null && !estado.isBlank()) ps.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            if (proveedorId != null) ps.add(cb.equal(root.get("proveedorId"), proveedorId));
            if (clienteId != null) ps.add(cb.equal(root.get("clienteId"), clienteId));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaInicio"), desde));
            if (hasta != null) ps.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaInicio"), hasta));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fechaInicio").descending().and(Sort.by("id").descending()));
        return PageResponse.of(repository.findAll(spec, pageable).map(o -> toDto(o, false)));
    }

    @Transactional(readOnly = true)
    public OperacionDto obtener(Long id) {
        return toDto(buscar(id), true);
    }

    @Transactional(readOnly = true)
    public OperacionDto.Resumen resumen() {
        return new OperacionDto.Resumen(repository.count(), repository.countByEstado(Operacion.ABIERTA),
                repository.countByEstado(Operacion.EN_PROCESO), repository.countByEstado(Operacion.COMPLETADA),
                repository.countByEstado(Operacion.CANCELADA), repository.countByTipo(Operacion.VENTA),
                repository.countByTipo(Operacion.COMPRA), repository.countByTipo(Operacion.PEDIDO));
    }

    // --- utilidades ---

    private void aplicar(Operacion o, OperacionRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        o.setTipo(r.tipo());
        o.setTitulo(r.titulo().trim());
        o.setDescripcion(Texto.limpio(r.descripcion()));
        o.setMonto(r.monto() == null ? BigDecimal.ZERO : r.monto());
        if (r.fechaInicio() != null) o.setFechaInicio(r.fechaInicio());
        o.setClienteId(null);
        o.setProveedorId(null);
        String nombre = Texto.limpio(r.contraparteNombre());

        if (r.clienteId() != null) {
            Cliente c = clientes.findById(r.clienteId()).orElse(null);
            if (c == null) errores.put("clienteId", "El cliente no existe");
            else { o.setClienteId(c.getId()); nombre = c.getNombre(); }
        }
        if (r.proveedorId() != null) {
            Proveedor p = proveedores.findById(r.proveedorId()).orElse(null);
            if (p == null) errores.put("proveedorId", "El proveedor no existe");
            else {
                o.setProveedorId(p.getId());
                if (o.getClienteId() == null) nombre = p.getRazonSocial() != null ? p.getRazonSocial() : p.getNombre();
            }
        }
        if (nombre == null) errores.put("contraparteNombre", "Indique el cliente, el proveedor o el nombre de la contraparte");
        if (!errores.isEmpty()) throw new FieldValidationException(errores);
        o.setContraparteNombre(nombre.length() > 255 ? nombre.substring(0, 255) : nombre);
    }

    private static OperacionEvento evento(String tipo, String titulo, String detalle, String referencia, UsuarioPrincipal actor) {
        OperacionEvento e = new OperacionEvento();
        e.setTipo(tipo);
        e.setTitulo(titulo);
        e.setDetalle(detalle);
        e.setReferencia(referencia);
        e.setUsuarioId(actor == null ? null : actor.id());
        return e;
    }

    private static void exigirAbierta(Operacion o) {
        if (o.cerrada()) {
            throw BusinessException.conflict("OPERACION_CERRADA", "La operación está " + o.getEstado() + " y ya no admite cambios.");
        }
    }

    private Operacion buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("NOT_FOUND", "Operación no encontrada."));
    }

    private OperacionDto toDto(Operacion o, boolean conEventos) {
        List<OperacionDto.Evento> eventos = conEventos
                ? o.getEventos().stream().map(e -> new OperacionDto.Evento(e.getId(), e.getTipo(), e.getTitulo(),
                        e.getDetalle(), e.getReferencia(), e.getFecha(), e.getUsuarioId())).toList()
                : null;
        return new OperacionDto(o.getId(), o.getCodigo(), o.getTipo(), o.getTitulo(), o.getDescripcion(), o.getClienteId(),
                o.getProveedorId(), o.getContraparteNombre(), o.getEstado(), o.getMonto(), o.getFechaInicio(),
                o.getFechaCierre(), o.getUsuarioId(), o.getCreadoEn(), o.getActualizadoEn(), eventos);
    }
}
