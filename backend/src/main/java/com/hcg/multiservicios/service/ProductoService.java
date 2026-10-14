package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.catalogo.ProductoDto;
import com.hcg.multiservicios.dto.catalogo.ProductoRequest;
import com.hcg.multiservicios.entity.Producto;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.exception.FieldValidationException;
import com.hcg.multiservicios.repository.ProductoRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import com.hcg.multiservicios.dto.catalogo.ProductoImportRequest;
import com.hcg.multiservicios.dto.catalogo.ProductoImportResultado;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Catalogo unico: la misma tabla guarda productos y servicios, siempre diferenciados por el tipo. */
@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final AuditoriaService auditoria;
    private final Validator validator;
    private final TransactionTemplate tx;

    public ProductoService(ProductoRepository repository, AuditoriaService auditoria, Validator validator,
                           PlatformTransactionManager txManager) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.validator = validator;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * Carga masiva: cada fila se guarda en su propia transacción, así una fila con error no frena a las demás.
     * Con actualizarExistentes, un código ya registrado actualiza ese producto (útil para cambiar precios o stock).
     */
    public ProductoImportResultado importar(ProductoImportRequest req, UsuarioPrincipal actor) {
        int creados = 0;
        int actualizados = 0;
        List<ProductoImportResultado.Error> errores = new ArrayList<>();
        java.util.Set<String> codigosVistos = new java.util.HashSet<>();
        for (ProductoImportRequest.Fila f : req.filas()) {
            int fila = f.fila() != null ? f.fila() : 0;
            ProductoRequest p = f.producto();
            String codigo = p == null ? null : Texto.limpio(p.codigo());
            try {
                if (p == null) {
                    throw new IllegalArgumentException("Fila vacía");
                }
                var violaciones = validator.validate(p);
                if (!violaciones.isEmpty()) {
                    throw new IllegalArgumentException(violaciones.stream().map(ConstraintViolation::getMessage)
                            .sorted().collect(java.util.stream.Collectors.joining("; ")));
                }
                if (codigo != null && !codigosVistos.add(codigo.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("El código " + codigo + " está repetido en el archivo");
                }
                Boolean fueActualizado = tx.execute(st -> guardarFila(p, codigo, req.actualizarExistentes(), actor));
                if (Boolean.TRUE.equals(fueActualizado)) {
                    actualizados++;
                } else {
                    creados++;
                }
            } catch (BusinessException | IllegalArgumentException e) {
                errores.add(new ProductoImportResultado.Error(fila, codigo, e.getMessage()));
            } catch (RuntimeException e) {
                errores.add(new ProductoImportResultado.Error(fila, codigo, "No se pudo guardar esta fila"));
            }
        }
        auditoria.registrar(actor, "PRODUCTOS_IMPORTADOS", "Producto", null, null,
                "creados=" + creados + ", actualizados=" + actualizados + ", errores=" + errores.size());
        return new ProductoImportResultado(req.filas().size(), creados, actualizados, errores);
    }

    /** true si actualizó un producto existente, false si creó uno nuevo. */
    private boolean guardarFila(ProductoRequest p, String codigo, boolean actualizar, UsuarioPrincipal actor) {
        if (codigo != null) {
            var existente = repository.findByCodigo(codigo);
            if (existente.isPresent()) {
                if (!actualizar) {
                    throw BusinessException.conflict("DUPLICATE_CODE", "Ya existe un registro con ese código.");
                }
                if (!Producto.TIPO_PRODUCTO.equals(existente.get().getTipo())) {
                    throw BusinessException.conflict("DUPLICATE_CODE", "Ese código pertenece a un servicio.");
                }
                actualizar(Producto.TIPO_PRODUCTO, existente.get().getId(), p, actor);
                return true;
            }
        }
        crear(Producto.TIPO_PRODUCTO, p, actor);
        return false;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoDto> buscar(String tipo, String q, Boolean activo, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Producto> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tipo"), tipo));
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                String[] campos = {"codigo", "nombre", "categoria"};
                Predicate[] ors = new Predicate[campos.length];
                for (int i = 0; i < campos.length; i++) {
                    ors[i] = cb.like(cb.lower(root.<String>get(campos[i])), p, '\\');
                }
                ps.add(cb.or(ors));
            }
            if (activo != null) {
                if (activo) {
                    ps.add(cb.equal(cb.upper(root.<String>get("estado")), Producto.ESTADO_ACTIVO));
                } else {
                    ps.add(cb.or(cb.isNull(root.<String>get("estado")),
                            cb.notEqual(cb.upper(root.<String>get("estado")), Producto.ESTADO_ACTIVO)));
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("nombre").ascending().and(Sort.by("id")));
        return PageResponse.of(repository.findAll(spec, pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(String tipo, Integer id) {
        return toDto(buscarPorId(tipo, id));
    }

    @Transactional
    public ProductoDto crear(String tipo, ProductoRequest req, UsuarioPrincipal actor) {
        validarReglas(tipo, req);
        Producto p = new Producto();
        p.setTipo(tipo);
        p.setEstado(Producto.ESTADO_ACTIVO);
        aplicar(p, tipo, req);
        String codigo = Texto.limpio(req.codigo());
        if (codigo != null && repository.existsByCodigo(codigo)) {
            throw BusinessException.conflict("DUPLICATE_CODE", "Ya existe un registro con ese código.");
        }
        p.setCodigo(codigo);
        p = repository.save(p);
        if (p.getCodigo() == null) {
            p.setCodigo(String.format("%s-%06d", prefijo(tipo), p.getId()));
            repository.save(p);
        }
        auditoria.registrar(actor, etiqueta(tipo) + "_CREADO", "Producto", p.getId(), null, resumen(p));
        return toDto(p);
    }

    @Transactional
    public ProductoDto actualizar(String tipo, Integer id, ProductoRequest req, UsuarioPrincipal actor) {
        validarReglas(tipo, req);
        Producto p = buscarPorId(tipo, id);
        String antes = resumen(p);
        aplicar(p, tipo, req);
        String codigo = Texto.limpio(req.codigo());
        if (codigo != null) {
            if (repository.existsByCodigoAndIdNot(codigo, id)) {
                throw BusinessException.conflict("DUPLICATE_CODE", "Ya existe un registro con ese código.");
            }
            p.setCodigo(codigo);
        }
        repository.save(p);
        auditoria.registrar(actor, etiqueta(tipo) + "_EDITADO", "Producto", id, antes, resumen(p));
        return toDto(p);
    }

    @Transactional
    public ProductoDto cambiarEstado(String tipo, Integer id, boolean activo, UsuarioPrincipal actor) {
        Producto p = buscarPorId(tipo, id);
        String antes = p.getEstado();
        p.setEstado(activo ? Producto.ESTADO_ACTIVO : Producto.ESTADO_INACTIVO);
        repository.save(p);
        auditoria.registrar(actor, activo ? etiqueta(tipo) + "_ACTIVADO" : etiqueta(tipo) + "_DESACTIVADO", "Producto", id,
                "estado=" + antes, "estado=" + p.getEstado());
        return toDto(p);
    }

    /** El tipo de la URL debe coincidir con el del registro: un servicio no se ve como producto ni al reves. */
    private Producto buscarPorId(String tipo, Integer id) {
        return repository.findByIdAndTipo(id, tipo).orElseThrow(() -> BusinessException.notFound(
                Producto.TIPO_SERVICIO.equals(tipo) ? "SERVICE_NOT_FOUND" : "PRODUCT_NOT_FOUND",
                Producto.TIPO_SERVICIO.equals(tipo) ? "Servicio no encontrado." : "Producto no encontrado."));
    }

    private void validarReglas(String tipo, ProductoRequest r) {
        if (Producto.TIPO_SERVICIO.equals(tipo)) {
            Map<String, String> errores = new LinkedHashMap<>();
            if (r.stock() != null) {
                errores.put("stock", "El stock no aplica a servicios");
            }
            if (r.stockMinimo() != null) {
                errores.put("stockMinimo", "El stock mínimo no aplica a servicios");
            }
            if (r.precioCompra() != null) {
                errores.put("precioCompra", "El precio de compra no aplica a servicios");
            }
            if (!errores.isEmpty()) {
                throw new FieldValidationException(errores);
            }
        }
    }

    private void aplicar(Producto p, String tipo, ProductoRequest r) {
        p.setNombre(r.nombre().trim());
        p.setDescripcion(Texto.limpio(r.descripcion()));
        p.setCategoria(Texto.limpio(r.categoria()));
        p.setPrecioVenta(r.precioVenta());
        if (Producto.TIPO_SERVICIO.equals(tipo)) {
            p.setPrecioCompra(null);
            p.setStock(null);
            p.setStockMinimo(null);
            String u = Texto.limpio(r.unidadMedida());
            p.setUnidadMedida(u != null ? u : "SERV");
        } else {
            p.setPrecioCompra(r.precioCompra());
            p.setStock(r.stock() != null ? r.stock() : 0);
            p.setStockMinimo(r.stockMinimo() != null ? r.stockMinimo() : 0);
            String u = Texto.limpio(r.unidadMedida());
            p.setUnidadMedida(u != null ? u : "UND");
        }
    }

    private String prefijo(String tipo) {
        return Producto.TIPO_SERVICIO.equals(tipo) ? "SRV" : "PRD";
    }

    private String etiqueta(String tipo) {
        return Producto.TIPO_SERVICIO.equals(tipo) ? "SERVICIO" : "PRODUCTO";
    }

    private String resumen(Producto p) {
        return "codigo=" + p.getCodigo() + ", nombre=" + p.getNombre() + ", precioVenta=" + p.getPrecioVenta()
                + ", stock=" + p.getStock();
    }

    private ProductoDto toDto(Producto p) {
        boolean stockBajo = Producto.TIPO_PRODUCTO.equals(p.getTipo()) && p.getStock() != null
                && p.getStockMinimo() != null && p.getStock() <= p.getStockMinimo();
        return new ProductoDto(p.getId(), p.getCodigo(), p.getTipo(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                p.getPrecioVenta(), p.getPrecioCompra(), p.getStock(), p.getStockMinimo(), p.getUnidadMedida(),
                p.esActivo(), stockBajo);
    }
}
