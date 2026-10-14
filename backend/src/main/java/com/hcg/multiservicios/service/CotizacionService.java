package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.CotizacionDto;
import com.hcg.multiservicios.dto.CotizacionRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.dto.apisperu.ClienteBoletaDto;
import com.hcg.multiservicios.dto.apisperu.EmitirBoletaRequest;
import com.hcg.multiservicios.dto.apisperu.ItemBoletaDto;
import com.hcg.multiservicios.entity.Cliente;
import com.hcg.multiservicios.entity.Cotizacion;
import com.hcg.multiservicios.entity.CotizacionDetalle;
import com.hcg.multiservicios.entity.Documento;
import com.hcg.multiservicios.entity.Producto;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.exception.FieldValidationException;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.repository.CotizacionRepository;
import com.hcg.multiservicios.repository.ProductoRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cotizaciones. Los precios unitarios incluyen IGV (18%), igual que en las boletas. Solo se edita en BORRADOR.
 * Flujo: BORRADOR -> ENVIADA -> APROBADA | RECHAZADA; una APROBADA se convierte en boleta y pasa a FACTURADA.
 */
@Service
public class CotizacionService {

    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");
    private static final String DOC = "COTIZACION";

    private final CotizacionRepository repository;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final DocumentoService documentos;
    private final ComprobanteService comprobantes;
    private final AuditoriaService auditoria;
    private final TransactionTemplate tx;

    public CotizacionService(CotizacionRepository repository, ClienteRepository clientes, ProductoRepository productos,
                             DocumentoService documentos, ComprobanteService comprobantes, AuditoriaService auditoria,
                             PlatformTransactionManager txManager) {
        this.repository = repository;
        this.clientes = clientes;
        this.productos = productos;
        this.documentos = documentos;
        this.comprobantes = comprobantes;
        this.auditoria = auditoria;
        this.tx = new TransactionTemplate(txManager);
    }

    @Transactional
    public CotizacionDto crear(CotizacionRequest r, UsuarioPrincipal actor) {
        Cotizacion c = new Cotizacion();
        c.setFecha(LocalDate.now(ZONA_PERU));
        c.setUsuarioId(actor == null ? null : actor.id());
        aplicar(c, r);
        c = repository.save(c);
        c.setNumero(String.format("COT-%06d", c.getId()));
        registrarDocumento(c);
        auditoria.registrar(actor, "CREAR_COTIZACION", "cotizacion", c.getId(), null, c.getNumero());
        return toDto(c, true);
    }

    @Transactional
    public CotizacionDto actualizar(Integer id, CotizacionRequest r, UsuarioPrincipal actor) {
        Cotizacion c = buscar(id);
        if (!Cotizacion.BORRADOR.equals(c.getEstado())) {
            throw BusinessException.conflict("COTIZACION_NO_EDITABLE", "Solo se puede editar una cotización en BORRADOR.");
        }
        aplicar(c, r);
        c.setActualizadoEn(LocalDateTime.now());
        c = repository.save(c);
        registrarDocumento(c);
        auditoria.registrar(actor, "EDITAR_COTIZACION", "cotizacion", id, null, c.getNumero());
        return toDto(c, true);
    }

    @Transactional
    public CotizacionDto cambiarEstado(Integer id, String nuevo, UsuarioPrincipal actor) {
        Cotizacion c = buscar(id);
        String actual = c.getEstado();
        boolean valido = switch (actual) {
            case Cotizacion.BORRADOR -> Cotizacion.ENVIADA.equals(nuevo);
            case Cotizacion.ENVIADA -> Cotizacion.APROBADA.equals(nuevo) || Cotizacion.RECHAZADA.equals(nuevo);
            case Cotizacion.APROBADA -> Cotizacion.RECHAZADA.equals(nuevo);
            default -> false;
        };
        if (!valido) {
            throw BusinessException.conflict("TRANSICION_INVALIDA", "No se puede pasar de " + actual + " a " + nuevo + ".");
        }
        c.setEstado(nuevo);
        c.setActualizadoEn(LocalDateTime.now());
        repository.save(c);
        documentos.actualizarEstado(DOC, c.getNumero(), nuevo);
        auditoria.registrar(actor, "ESTADO_COTIZACION", "cotizacion", id, actual, nuevo);
        return toDto(c, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<CotizacionDto> buscar(String q, String estado, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Cotizacion> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null && !estado.isBlank()) ps.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.<String>get("numero")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("clienteNombre")), p, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("id").descending());
        return PageResponse.of(repository.findAll(spec, pageable).map(c -> toDto(c, false)));
    }

    @Transactional(readOnly = true)
    public CotizacionDto obtener(Integer id) {
        return toDto(buscar(id), true);
    }

    /** Emite una boleta con los datos de la cotizacion APROBADA. Si SUNAT/APIsPERU falla, la cotizacion sigue APROBADA. */
    public BoletaResponse convertirEnBoleta(Integer id, UsuarioPrincipal actor) {
        EmitirBoletaRequest request = tx.execute(status -> {
            Cotizacion c = buscar(id);
            if (Cotizacion.FACTURADA.equals(c.getEstado())) {
                throw BusinessException.conflict("COTIZACION_FACTURADA", "La cotización ya fue convertida en boleta.");
            }
            if (!Cotizacion.APROBADA.equals(c.getEstado())) {
                throw BusinessException.conflict("COTIZACION_NO_APROBADA", "Solo se puede facturar una cotización APROBADA.");
            }
            if (c.getComprobanteId() != null) {
                throw BusinessException.conflict("BOLETA_PENDIENTE",
                        "Ya se generó una boleta que no fue aceptada; reenvíela desde Boletas en vez de crear otra.");
            }
            return construirBoleta(c);
        });

        BoletaResponse respuesta = comprobantes.emitirBoleta(request, actor);

        tx.executeWithoutResult(status -> {
            Cotizacion c = buscar(id);
            if (respuesta.getId() != null) c.setComprobanteId(respuesta.getId());
            if (respuesta.isSuccess()) c.setEstado(Cotizacion.FACTURADA);
            c.setActualizadoEn(LocalDateTime.now());
            repository.save(c);
            if (respuesta.isSuccess()) documentos.actualizarEstado(DOC, c.getNumero(), Cotizacion.FACTURADA);
            auditoria.registrar(actor, "COTIZACION_A_BOLETA", "cotizacion", id, null,
                respuesta.getSerie() + "-" + respuesta.getCorrelativo() + " " + respuesta.getEstado());
        });
        return respuesta;
    }

    private EmitirBoletaRequest construirBoleta(Cotizacion c) {
        EmitirBoletaRequest r = new EmitirBoletaRequest();
        ClienteBoletaDto cb = new ClienteBoletaDto("1", "00000000", c.getClienteNombre() != null ? c.getClienteNombre() : "CLIENTE VARIOS");
        if (c.getClienteId() != null) {
            clientes.findById(c.getClienteId()).ifPresent(cli -> {
                if (cli.getDni() != null && !cli.getDni().isBlank()) cb.setNumDoc(cli.getDni());
                cb.setDireccion(cli.getDireccion());
                cb.setEmail(cli.getCorreo());
                cb.setTelefono(cli.getTelefono());
            });
        }
        r.setCliente(cb);
        r.setTipoMoneda(c.getMoneda());
        r.setObservacion("Cotización " + c.getNumero());
        List<ItemBoletaDto> items = new ArrayList<>();
        for (CotizacionDetalle d : c.getDetalles()) {
            ItemBoletaDto it = new ItemBoletaDto();
            String codigo = null;
            if (d.getProductoId() != null) {
                codigo = productos.findById(d.getProductoId()).map(Producto::getCodigo).orElse(null);
            }
            it.setCodProducto(codigo != null ? codigo : "ITEM-" + d.getItem());
            it.setDescripcion(d.getDescripcion());
            it.setUnidad(d.getUnidad());
            it.setCantidad(d.getCantidad());
            it.setPrecioUnitario(d.getPrecioUnitario());
            items.add(it);
        }
        r.setItems(items);
        return r;
    }

    /** Valida, resuelve cliente y productos, y recalcula montos y detalle. */
    private void aplicar(Cotizacion c, CotizacionRequest r) {
        Map<String, String> errores = new java.util.LinkedHashMap<>();
        if (r.clienteId() != null) {
            Cliente cli = clientes.findById(r.clienteId())
                    .orElseThrow(() -> BusinessException.notFound("CLIENTE_NO_ENCONTRADO", "El cliente indicado no existe."));
            c.setClienteId(cli.getId());
            c.setClienteNombre(cli.getNombre());
            c.setTelefono(Texto.limpio(r.telefono()) != null ? Texto.limpio(r.telefono()) : cli.getTelefono());
        } else {
            String nombre = Texto.limpio(r.clienteNombre());
            if (nombre == null) errores.put("clienteNombre", "Indique el cliente (clienteId o clienteNombre)");
            c.setClienteId(null);
            c.setClienteNombre(nombre);
            c.setTelefono(Texto.limpio(r.telefono()));
        }
        c.setVigenciaHasta(r.vigenciaHasta());
        c.setObservacion(Texto.limpio(r.observacion()));

        List<CotizacionDetalle> nuevos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int n = 1;
        for (CotizacionRequest.Item i : r.items()) {
            Producto p = i.productoId() == null ? null : productos.findById(i.productoId()).orElse(null);
            String campo = "items[" + (n - 1) + "]";
            if (i.productoId() != null && p == null) errores.put(campo + ".productoId", "El producto no existe");
            String desc = Texto.limpio(i.descripcion()) != null ? Texto.limpio(i.descripcion()) : (p != null ? p.getNombre() : null);
            BigDecimal precio = i.precioUnitario() != null ? i.precioUnitario() : (p != null ? p.getPrecioVenta() : null);
            if (desc == null) errores.put(campo + ".descripcion", "Indique la descripción o un producto");
            if (precio == null) errores.put(campo + ".precioUnitario", "Indique el precio o un producto con precio de venta");
            if (desc != null && precio != null) {
                String unidad = Texto.limpio(i.unidad());
                if (unidad == null) unidad = p != null && p.getUnidadMedida() != null && p.getUnidadMedida().length() <= 5 ? p.getUnidadMedida() : "NIU";
                BigDecimal linea = precio.multiply(i.cantidad()).setScale(2, RoundingMode.HALF_UP);
                CotizacionDetalle d = new CotizacionDetalle();
                d.setItem(n);
                d.setProductoId(p == null ? null : p.getId());
                d.setDescripcion(desc.length() > 255 ? desc.substring(0, 255) : desc);
                d.setUnidad(unidad);
                d.setCantidad(i.cantidad());
                d.setPrecioUnitario(precio.setScale(2, RoundingMode.HALF_UP));
                d.setSubtotal(linea);
                nuevos.add(d);
                total = total.add(linea);
            }
            n++;
        }
        if (!errores.isEmpty()) throw new FieldValidationException(errores);

        if (total.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw BusinessException.badRequest("MONTO_FUERA_DE_RANGO", "El total supera el límite NUMERIC(12,2).");
        }
        c.getDetalles().clear();
        nuevos.forEach(c::agregarDetalle);
        BigDecimal subtotal = total.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
        c.setTotal(total);
        c.setSubtotal(subtotal);
        c.setIgv(total.subtract(subtotal));
    }

    private void registrarDocumento(Cotizacion c) {
        documentos.registrarAutomatico(DOC, c.getNumero(), c.getFecha(), Documento.ORIGEN_COTIZACION,
                String.valueOf(c.getId()), c.getClienteNombre(), null, c.getTotal(), c.getMoneda(), c.getEstado(),
                c.getUsuarioId());
    }

    private Cotizacion buscar(Integer id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("NOT_FOUND", "Cotización no encontrada."));
    }

    private CotizacionDto toDto(Cotizacion c, boolean conItems) {
        List<CotizacionDto.Item> items = conItems
                ? c.getDetalles().stream().map(d -> new CotizacionDto.Item(d.getItem() == null ? 0 : d.getItem(),
                        d.getProductoId(), d.getDescripcion(), d.getUnidad(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getSubtotal())).toList()
                : null;
        return new CotizacionDto(c.getId(), c.getNumero(), c.getClienteId(), c.getClienteNombre(), c.getTelefono(),
                c.getFecha(), c.getVigenciaHasta(), c.getEstado(), c.getMoneda(), c.getSubtotal(), c.getIgv(),
                c.getTotal(), c.getObservacion(), c.getComprobanteId(), c.getUsuarioId(), c.getCreadoEn(), items);
    }
}




