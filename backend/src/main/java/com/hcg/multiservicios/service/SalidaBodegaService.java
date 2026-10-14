package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.SalidaBodegaDto;
import com.hcg.multiservicios.dto.SalidaBodegaRequest;
import com.hcg.multiservicios.entity.Cliente;
import com.hcg.multiservicios.entity.Producto;
import com.hcg.multiservicios.entity.Proveedor;
import com.hcg.multiservicios.entity.SalidaBodega;
import com.hcg.multiservicios.entity.SalidaBodegaDetalle;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.exception.FieldValidationException;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.repository.ProductoRepository;
import com.hcg.multiservicios.repository.ProveedorRepository;
import com.hcg.multiservicios.repository.SalidaBodegaRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salidas de bodega: descuentan stock, guardan el stock anterior y posterior de cada producto y se registran en
 * Documentos. Anular una salida devuelve el stock. Todo ocurre en una sola transaccion con bloqueo de los productos.
 */
@Service
public class SalidaBodegaService {

    private static final String DOC = "SALIDA_BODEGA";
    private static final String ORIGEN = "SALIDA";

    private final SalidaBodegaRepository repository;
    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final ClienteRepository clientes;
    private final DocumentoService documentos;
    private final AuditoriaService auditoria;

    public SalidaBodegaService(SalidaBodegaRepository repository, ProductoRepository productos,
                               ProveedorRepository proveedores, ClienteRepository clientes,
                               DocumentoService documentos, AuditoriaService auditoria) {
        this.repository = repository;
        this.productos = productos;
        this.proveedores = proveedores;
        this.clientes = clientes;
        this.documentos = documentos;
        this.auditoria = auditoria;
    }

    @Transactional
    public SalidaBodegaDto registrar(SalidaBodegaRequest r, UsuarioPrincipal actor) {
        Map<String, String> errores = new LinkedHashMap<>();
        SalidaBodega s = new SalidaBodega();
        s.setDestinoTipo(r.destinoTipo());
        s.setMotivo(r.motivo());
        s.setObservacion(Texto.limpio(r.observacion()));
        s.setUsuarioId(actor == null ? null : actor.id());
        resolverDestino(s, r, errores);

        // 1) resolver cada item a un producto y sumar cantidades repetidas del mismo producto
        Map<Integer, Integer> porProducto = new TreeMap<>(); // ordenado por id: bloqueos siempre en el mismo orden
        int n = 0;
        for (SalidaBodegaRequest.Item i : r.items()) {
            String campo = "items[" + n++ + "]";
            Producto p = null;
            if (i.productoId() != null) {
                p = productos.findById(i.productoId()).orElse(null);
            } else if (Texto.limpio(i.codigo()) != null) {
                p = productos.findByCodigo(Texto.limpio(i.codigo())).orElse(null);
            } else {
                errores.put(campo, "Indique productoId o codigo");
                continue;
            }
            if (p == null) {
                errores.put(campo, "El producto no existe");
                continue;
            }
            porProducto.merge(p.getId(), i.cantidad(), Integer::sum);
        }
        if (!errores.isEmpty()) throw new FieldValidationException(errores);

        // 2) bloquear, validar stock y descontar
        int item = 1;
        for (Map.Entry<Integer, Integer> e : porProducto.entrySet()) {
            Producto p = productos.bloquear(e.getKey()).orElseThrow();
            String etiqueta = "producto " + (p.getCodigo() != null ? p.getCodigo() : p.getId());
            if (!Producto.TIPO_PRODUCTO.equals(p.getTipo())) {
                errores.put(etiqueta, "Es un servicio: no tiene stock");
                continue;
            }
            if (!p.esActivo()) {
                errores.put(etiqueta, "El producto está inactivo");
                continue;
            }
            int actual = p.getStock() == null ? 0 : p.getStock();
            if (actual < e.getValue()) {
                errores.put(etiqueta, "Stock insuficiente: hay " + actual + " y se piden " + e.getValue());
                continue;
            }
            SalidaBodegaDetalle d = new SalidaBodegaDetalle();
            d.setItem(item++);
            d.setProductoId(p.getId());
            d.setCodigo(p.getCodigo());
            d.setDescripcion(p.getNombre() != null && p.getNombre().length() > 255 ? p.getNombre().substring(0, 255) : p.getNombre());
            d.setCantidad(e.getValue());
            d.setStockAnterior(actual);
            d.setStockPosterior(actual - e.getValue());
            p.setStock(actual - e.getValue());
            s.agregarDetalle(d);
        }
        // si algo falla se lanza el error y la transaccion se revierte completa (ningun stock queda descontado)
        if (!errores.isEmpty()) throw new FieldValidationException(errores);

        s = repository.save(s);
        s.setNumero(String.format("SAL-%06d", s.getId()));
        documentos.registrarAutomatico(DOC, s.getNumero(), s.getFecha().toLocalDate(), ORIGEN, String.valueOf(s.getId()),
                s.getDestinoNombre(), null, BigDecimal.ZERO, "PEN", SalidaBodega.REGISTRADA, s.getUsuarioId());
        auditoria.registrar(actor, "REGISTRAR_SALIDA_BODEGA", "salida_bodega", s.getId(), null,
                s.getNumero() + " " + s.getDestinoTipo() + " " + s.getMotivo());
        return toDto(s, true);
    }

    @Transactional
    public SalidaBodegaDto anular(Long id, UsuarioPrincipal actor) {
        SalidaBodega s = buscar(id);
        if (SalidaBodega.ANULADA.equals(s.getEstado())) {
            throw BusinessException.conflict("SALIDA_ANULADA", "La salida ya está anulada.");
        }
        List<SalidaBodegaDetalle> ordenados = new ArrayList<>(s.getDetalles());
        ordenados.sort((a, b) -> Integer.compare(a.getProductoId(), b.getProductoId()));
        for (SalidaBodegaDetalle d : ordenados) {
            productos.bloquear(d.getProductoId()).ifPresent(p -> {
                int actual = p.getStock() == null ? 0 : p.getStock();
                p.setStock(actual + d.getCantidad());
            });
        }
        s.setEstado(SalidaBodega.ANULADA);
        s.setAnuladoEn(LocalDateTime.now());
        documentos.actualizarEstado(DOC, s.getNumero(), SalidaBodega.ANULADA);
        auditoria.registrar(actor, "ANULAR_SALIDA_BODEGA", "salida_bodega", id, SalidaBodega.REGISTRADA, SalidaBodega.ANULADA);
        return toDto(repository.save(s), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<SalidaBodegaDto> buscar(String q, String estado, String destinoTipo, LocalDate desde,
                                                LocalDate hasta, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<SalidaBodega> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.<String>get("numero")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("destinoNombre")), p, '\\')));
            }
            if (estado != null && !estado.isBlank()) ps.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            if (destinoTipo != null && !destinoTipo.isBlank()) ps.add(cb.equal(root.get("destinoTipo"), destinoTipo.trim().toUpperCase(Locale.ROOT)));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fecha"), desde.atStartOfDay()));
            if (hasta != null) ps.add(cb.lessThan(root.<LocalDateTime>get("fecha"), hasta.plusDays(1).atStartOfDay()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fecha").descending().and(Sort.by("id").descending()));
        return PageResponse.of(repository.findAll(spec, pageable).map(s -> toDto(s, false)));
    }

    @Transactional(readOnly = true)
    public SalidaBodegaDto obtener(Long id) {
        return toDto(buscar(id), true);
    }

    private void resolverDestino(SalidaBodega s, SalidaBodegaRequest r, Map<String, String> errores) {
        switch (r.destinoTipo()) {
            case "PROVEEDOR" -> {
                if (r.proveedorId() == null) {
                    errores.put("proveedorId", "Indique el proveedor");
                } else {
                    Proveedor p = proveedores.findById(r.proveedorId()).orElse(null);
                    if (p == null) {
                        errores.put("proveedorId", "El proveedor no existe");
                    } else {
                        s.setProveedorId(p.getId());
                        s.setDestinoNombre(p.getRazonSocial() != null ? p.getRazonSocial() : p.getNombre());
                    }
                }
            }
            case "CLIENTE" -> {
                if (r.clienteId() == null) {
                    errores.put("clienteId", "Indique el cliente");
                } else {
                    Cliente c = clientes.findById(r.clienteId()).orElse(null);
                    if (c == null) {
                        errores.put("clienteId", "El cliente no existe");
                    } else {
                        s.setClienteId(c.getId());
                        s.setDestinoNombre(c.getNombre());
                    }
                }
            }
            default -> {
                String nombre = Texto.limpio(r.destinoNombre());
                if (nombre == null) errores.put("destinoNombre", "Indique el nombre del destino");
                s.setDestinoNombre(nombre);
            }
        }
    }

    private SalidaBodega buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("NOT_FOUND", "Salida de bodega no encontrada."));
    }

    private SalidaBodegaDto toDto(SalidaBodega s, boolean conItems) {
        List<SalidaBodegaDto.Item> items = conItems
                ? s.getDetalles().stream().map(d -> new SalidaBodegaDto.Item(d.getItem(), d.getProductoId(), d.getCodigo(),
                        d.getDescripcion(), d.getCantidad(), d.getStockAnterior(), d.getStockPosterior())).toList()
                : null;
        return new SalidaBodegaDto(s.getId(), s.getNumero(), s.getFecha(), s.getDestinoTipo(), s.getProveedorId(),
                s.getClienteId(), s.getDestinoNombre(), s.getMotivo(), s.getObservacion(), s.getEstado(),
                s.getUsuarioId(), s.getCreadoEn(), s.getAnuladoEn(), items);
    }
}
