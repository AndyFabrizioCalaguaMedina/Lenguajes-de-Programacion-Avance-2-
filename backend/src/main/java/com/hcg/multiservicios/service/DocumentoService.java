package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.DocumentoDto;
import com.hcg.multiservicios.dto.DocumentoRequest;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.entity.Documento;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.DocumentoRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro central de documentos. Las boletas y cotizaciones se registran solas (y siguen el estado de su origen);
 * facturas, ordenes de compra, guias y otros se registran a mano y solo esos se pueden editar o anular desde aqui.
 */
@Service
public class DocumentoService {

    private final DocumentoRepository repository;
    private final AuditoriaService auditoria;

    public DocumentoService(DocumentoRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    /** Crea o actualiza el documento de un origen automatico (idempotente por tipo + numero). */
    @Transactional
    public void registrarAutomatico(String tipo, String numero, LocalDate fecha, String origen, String origenId,
                                    String contraparteNombre, String contraparteDoc, BigDecimal monto, String moneda,
                                    String estado, Long usuarioId) {
        Documento d = repository.findByTipoAndNumero(tipo, numero).orElseGet(Documento::new);
        if (d.getId() != null && (!origen.equals(d.getOrigen()) || !java.util.Objects.equals(origenId, d.getOrigenId()))) {
            throw BusinessException.conflict("DOCUMENTO_ORIGEN_CONFLICTO",
                    "El tipo y número ya pertenecen a otro origen documental.");
        }
        d.setTipo(tipo);
        d.setNumero(numero);
        d.setFecha(fecha);
        d.setOrigen(origen);
        d.setOrigenId(origenId);
        d.setContraparteNombre(recortar(contraparteNombre, 255));
        d.setContraparteDoc(recortar(contraparteDoc, 20));
        d.setMonto(monto == null ? BigDecimal.ZERO : monto);
        d.setMoneda(moneda == null ? "PEN" : moneda);
        d.setEstado(recortar(estado, 20));
        if (d.getUsuarioId() == null) d.setUsuarioId(usuarioId);
        d.setActualizadoEn(LocalDateTime.now());
        repository.save(d);
    }

    @Transactional
    public void actualizarEstado(String tipo, String numero, String estado) {
        repository.findByTipoAndNumero(tipo, numero).ifPresent(d -> {
            d.setEstado(recortar(estado, 20));
            d.setActualizadoEn(LocalDateTime.now());
            repository.save(d);
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentoDto> buscar(String q, String tipo, String estado, LocalDate desde, LocalDate hasta,
                                             int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Documento> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.<String>get("numero")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("contraparteNombre")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("contraparteDoc")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("descripcion")), p, '\\')));
            }
            if (tipo != null && !tipo.isBlank()) ps.add(cb.equal(root.get("tipo"), tipo.trim().toUpperCase(Locale.ROOT)));
            if (estado != null && !estado.isBlank()) ps.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fecha"), desde));
            if (hasta != null) ps.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fecha"), hasta));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fecha").descending().and(Sort.by("id").descending()));
        return PageResponse.of(repository.findAll(spec, pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public DocumentoDto obtener(Long id) {
        return toDto(buscar(id));
    }

    @Transactional
    public DocumentoDto crear(DocumentoRequest r, UsuarioPrincipal actor) {
        String tipo = r.tipo().trim().toUpperCase(Locale.ROOT);
        String numero = r.numero().trim();
        if (repository.findByTipoAndNumero(tipo, numero).isPresent()) {
            throw BusinessException.conflict("DOCUMENTO_DUPLICADO", "Ya existe un documento " + tipo + " con el número " + numero + ".");
        }
        Documento d = new Documento();
        aplicar(d, r);
        d.setTipo(tipo);
        d.setNumero(numero);
        d.setOrigen(Documento.ORIGEN_MANUAL);
        d.setUsuarioId(actor == null ? null : actor.id());
        d = repository.save(d);
        auditoria.registrar(actor, "CREAR_DOCUMENTO", "documento", d.getId(), null, tipo + " " + numero);
        return toDto(d);
    }

    @Transactional
    public DocumentoDto actualizar(Long id, DocumentoRequest r, UsuarioPrincipal actor) {
        Documento d = buscar(id);
        soloManual(d);
        String tipo = r.tipo().trim().toUpperCase(Locale.ROOT);
        String numero = r.numero().trim();
        repository.findByTipoAndNumero(tipo, numero).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
            throw BusinessException.conflict("DOCUMENTO_DUPLICADO", "Ya existe un documento " + tipo + " con el número " + numero + ".");
        });
        String antes = d.getTipo() + " " + d.getNumero();
        aplicar(d, r);
        d.setTipo(tipo);
        d.setNumero(numero);
        d.setActualizadoEn(LocalDateTime.now());
        d = repository.save(d);
        auditoria.registrar(actor, "EDITAR_DOCUMENTO", "documento", id, antes, tipo + " " + numero);
        return toDto(d);
    }

    @Transactional
    public DocumentoDto anular(Long id, UsuarioPrincipal actor) {
        Documento d = buscar(id);
        soloManual(d);
        String antes = d.getEstado();
        d.setEstado(Documento.ANULADO);
        d.setActualizadoEn(LocalDateTime.now());
        d = repository.save(d);
        auditoria.registrar(actor, "ANULAR_DOCUMENTO", "documento", id, antes, Documento.ANULADO);
        return toDto(d);
    }

    private void aplicar(Documento d, DocumentoRequest r) {
        d.setFecha(r.fecha());
        d.setContraparteNombre(Texto.limpio(r.contraparteNombre()));
        d.setContraparteDoc(Texto.limpio(r.contraparteDoc()));
        d.setMonto(r.monto() == null ? BigDecimal.ZERO : r.monto());
        d.setMoneda(r.moneda() == null || r.moneda().isBlank() ? "PEN" : r.moneda());
        d.setDescripcion(Texto.limpio(r.descripcion()));
        d.setArchivoUrl(Texto.limpio(r.archivoUrl()));
    }

    private void soloManual(Documento d) {
        if (!Documento.ORIGEN_MANUAL.equals(d.getOrigen())) {
            throw BusinessException.conflict("DOCUMENTO_AUTOMATICO",
                    "Este documento se genera automáticamente; modifíquelo desde su " + d.getOrigen().toLowerCase(Locale.ROOT) + ".");
        }
        if (Documento.ANULADO.equals(d.getEstado())) {
            throw BusinessException.conflict("DOCUMENTO_ANULADO", "El documento ya está anulado.");
        }
    }

    private Documento buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("NOT_FOUND", "Documento no encontrado."));
    }

    private DocumentoDto toDto(Documento d) {
        return new DocumentoDto(d.getId(), d.getTipo(), d.getNumero(), d.getFecha(), d.getOrigen(), d.getOrigenId(),
                d.getContraparteNombre(), d.getContraparteDoc(), d.getMonto(), d.getMoneda(), d.getEstado(),
                d.getDescripcion(), d.getArchivoUrl(), d.getUsuarioId(), d.getCreadoEn());
    }

    private static String recortar(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}

