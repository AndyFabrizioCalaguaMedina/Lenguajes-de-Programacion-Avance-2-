package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.ComprobanteDto;
import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.apisperu.ApisPeruDetailItem;
import com.hcg.multiservicios.dto.apisperu.ApisPeruInvoiceRequest;
import com.hcg.multiservicios.dto.apisperu.BoletaResponse;
import com.hcg.multiservicios.dto.apisperu.EmitirBoletaRequest;
import com.hcg.multiservicios.entity.Cliente;
import com.hcg.multiservicios.entity.Comprobante;
import com.hcg.multiservicios.entity.ComprobanteDetalle;
import com.hcg.multiservicios.entity.Documento;
import com.hcg.multiservicios.entity.SerieComprobante;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.repository.ComprobanteRepository;
import com.hcg.multiservicios.repository.SerieComprobanteRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Emision de boletas con persistencia. Flujo: (1) transaccion corta que reserva el correlativo y guarda la boleta
 * como PENDIENTE; (2) llamada a APIsPERU fuera de transaccion (no retiene conexion a la BD); (3) transaccion que
 * guarda el resultado. Si el paso 2 falla por comunicacion queda en ERROR y se puede reenviar con el mismo numero.
 */
@Service
public class ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);
    private static final String BOLETA = "03";
    private static final String FACTURA = "01";

    private final ComprobanteRepository comprobantes;
    private final SerieComprobanteRepository series;
    private final ClienteRepository clientes;
    private final ApisPeruBoletaService apisPeru;
    private final AuditoriaService auditoria;
    private final DocumentoService documentos;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public ComprobanteService(ComprobanteRepository comprobantes, SerieComprobanteRepository series,
                              ClienteRepository clientes, ApisPeruBoletaService apisPeru, AuditoriaService auditoria,
                              DocumentoService documentos, ObjectMapper mapper, PlatformTransactionManager txManager) {
        this.comprobantes = comprobantes;
        this.series = series;
        this.clientes = clientes;
        this.apisPeru = apisPeru;
        this.auditoria = auditoria;
        this.documentos = documentos;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
    }

    public BoletaResponse emitirBoleta(EmitirBoletaRequest request, UsuarioPrincipal actor) {
        String tipo = tipoDe(request);
        validarReceptor(tipo, request);
        String serie = request.getSerie() != null && !request.getSerie().isBlank() ? request.getSerie().trim()
                : FACTURA.equals(tipo) ? "FFF1" : "BBB1";
        // El correlativo lo asigna siempre el servidor; el que envie el cliente se ignora.
        Long id = tx.execute(status -> reservarYGuardar(request, tipo, serie, actor));
        return enviarYActualizar(id, actor);
    }

    /** Reintenta una boleta en ERROR o PENDIENTE con su mismo serie-correlativo. */
    public BoletaResponse reenviar(String serie, int correlativo, UsuarioPrincipal actor) {
        Comprobante c = buscar(serie, correlativo);
        if (!Comprobante.ERROR.equals(c.getEstado()) && !Comprobante.PENDIENTE.equals(c.getEstado())) {
            throw new BusinessException(HttpStatus.CONFLICT, "COMPROBANTE_NO_REENVIABLE",
                    "Solo se pueden reenviar comprobantes en estado ERROR o PENDIENTE.");
        }
        auditoria.registrar(actor, "REENVIAR_BOLETA", "comprobante", c.getId(), c.getEstado(), null);
        return enviarYActualizar(c.getId(), actor);
    }

    private Long reservarYGuardar(EmitirBoletaRequest request, String tipo, String serie, UsuarioPrincipal actor) {
        SerieComprobante s = series.bloquear(tipo, serie).orElseThrow(() -> new BusinessException(
                HttpStatus.BAD_REQUEST, "SERIE_INVALIDA", "La serie " + serie + " no existe o está inactiva."));
        int correlativo = s.getUltimoCorrelativo() + 1;
        s.setUltimoCorrelativo(correlativo);
        series.save(s);

        request.setTipoDoc(tipo);
        request.setSerie(serie);
        request.setCorrelativo(String.valueOf(correlativo));
        ApisPeruInvoiceRequest invoice = apisPeru.construirInvoiceRequest(request);
        request.setFechaEmision(invoice.getFechaEmision()); // se fija para reenviar con la misma fecha

        Comprobante c = new Comprobante();
        c.setTipoDoc(tipo);
        c.setSerie(serie);
        c.setCorrelativo(correlativo);
        c.setFechaEmision(parsearFecha(invoice.getFechaEmision()));
        c.setMoneda(invoice.getTipoMoneda());
        c.setObservacion(recortar(invoice.getObservacion(), 500));
        c.setMtoOperGravadas(invoice.getMtoOperGravadas());
        c.setMtoIgv(invoice.getMtoIGV());
        c.setMtoImpVenta(invoice.getMtoImpVenta());
        c.setClienteTipoDoc(invoice.getClient().getTipoDoc());
        c.setClienteNumDoc(invoice.getClient().getNumDoc());
        c.setClienteNombre(recortar(invoice.getClient().getRznSocial(), 255));
        if (request.getCliente() != null) {
            c.setClienteDireccion(recortar(request.getCliente().getDireccion(), 255));
        }
        c.setClienteId(buscarClienteId(invoice.getClient().getTipoDoc(), invoice.getClient().getNumDoc()));
        c.setUsuarioId(actor == null ? null : actor.id());
        c.setRequestJson(escribir(request));

        int item = 1;
        for (ApisPeruDetailItem d : invoice.getDetails()) {
            ComprobanteDetalle det = new ComprobanteDetalle();
            det.setItem(item++);
            det.setCodProducto(recortar(d.getCodProducto(), 30));
            det.setDescripcion(recortar(d.getDescripcion(), 500));
            det.setUnidad(recortar(d.getUnidad(), 5));
            det.setCantidad(d.getCantidad());
            det.setValorUnitario(d.getMtoValorUnitario());
            det.setPrecioUnitario(d.getMtoPrecioUnitario());
            det.setValorVenta(d.getMtoValorVenta());
            det.setIgv(d.getIgv());
            c.agregarDetalle(det);
        }
        c = comprobantes.save(c);
        documentos.registrarAutomatico(nombreTipo(tipo), serie + "-" + correlativo, c.getFechaEmision().toLocalDate(),
                Documento.ORIGEN_COMPROBANTE, String.valueOf(c.getId()), c.getClienteNombre(), c.getClienteNumDoc(),
                c.getMtoImpVenta(), c.getMoneda(), Comprobante.PENDIENTE, c.getUsuarioId());
        auditoria.registrar(actor, "EMITIR_BOLETA", "comprobante", c.getId(), null, nombreTipo(tipo) + " serie=" + serie);
        return c.getId();
    }

    private BoletaResponse enviarYActualizar(Long id, UsuarioPrincipal actor) {
        Comprobante guardado = comprobantes.findById(id).orElseThrow();
        EmitirBoletaRequest original = leer(guardado.getRequestJson());
        BoletaResponse respuesta = apisPeru.enviarInvoice(apisPeru.construirInvoiceRequest(original));

        tx.executeWithoutResult(status -> {
            Comprobante c = comprobantes.findById(id).orElseThrow();
            String estadoAnterior = c.getEstado();
            c.setEstado(estadoDe(respuesta));
            c.setCodigoSunat(recortar(respuesta.getCodigoRespuestaSunat(), 60));
            c.setDescripcionSunat(recortar(respuesta.getDescripcionSunat(), 1000));
            if (respuesta.getHash() != null) c.setHash(recortar(respuesta.getHash(), 100));
            if (respuesta.getXml() != null) c.setXml(respuesta.getXml());
            if (respuesta.getCdrZip() != null) c.setCdrZip(respuesta.getCdrZip());
            c.setActualizadoEn(LocalDateTime.now());
            comprobantes.save(c);
            documentos.actualizarEstado(nombreTipo(c.getTipoDoc()), c.getSerie() + "-" + c.getCorrelativo(), c.getEstado());
            auditoria.registrar(actor, "ESTADO_BOLETA", "comprobante", c.getId(), estadoAnterior, c.getEstado());
        });
        log.info("Comprobante {}-{} -> {}", guardado.getSerie(), guardado.getCorrelativo(), estadoDe(respuesta));

        respuesta.setId(id);
        respuesta.setSerie(guardado.getSerie());
        respuesta.setCorrelativo(String.valueOf(guardado.getCorrelativo()));
        respuesta.setPdfUrl(respuesta.isSuccess()
                ? "/api/v1/boletas/" + guardado.getSerie() + "-" + guardado.getCorrelativo() + "/pdf"
                : null);
        return respuesta;
    }

    static String estadoDe(BoletaResponse r) {
        if (r.isSuccess()) return Comprobante.ACEPTADA;
        String cod = r.getCodigoRespuestaSunat();
        return "COMMUNICATION_ERROR".equals(cod) || "EMPTY_RESPONSE".equals(cod) || "APISPERU_SERVER_ERROR".equals(cod) ? Comprobante.ERROR : Comprobante.RECHAZADA;
    }

    @Transactional(readOnly = true)
    public PageResponse<ComprobanteDto> listar(String q, String estado, String tipo, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Comprobante> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null && !estado.isBlank()) {
                ps.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            }
            if (tipo != null && !tipo.isBlank()) {
                ps.add(cb.equal(root.get("tipoDoc"), tipo.trim()));
            }
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.<String>get("serie")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("clienteNombre")), p, '\\'),
                        cb.like(cb.lower(root.<String>get("clienteNumDoc")), p, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("fechaEmision").descending().and(Sort.by("id").descending()));
        return PageResponse.of(comprobantes.findAll(spec, pageable).map(c -> toDto(c, false)));
    }

    @Transactional(readOnly = true)
    public ComprobanteDto obtener(String serie, int correlativo) {
        return toDto(buscar(serie, correlativo), true);
    }

    @Transactional(readOnly = true)
    public String xml(String serie, int correlativo) {
        String xml = buscar(serie, correlativo).getXml();
        if (xml == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "XML_NO_DISPONIBLE", "Este comprobante aún no tiene XML.");
        }
        return xml;
    }

    /** PDF de una boleta emitida (aceptada): se genera en APIsPERU con los mismos datos guardados al emitir. */
    public byte[] pdf(String serie, int correlativo) {
        Comprobante c = buscar(serie, correlativo);
        if (!Comprobante.ACEPTADA.equals(c.getEstado())) {
            throw new BusinessException(HttpStatus.CONFLICT, "PDF_NO_DISPONIBLE",
                    "Solo los comprobantes ACEPTADOS tienen PDF (estado actual: " + c.getEstado() + ").");
        }
        return apisPeru.generarPdf(apisPeru.construirInvoiceRequest(leer(c.getRequestJson())));
    }

    private Comprobante buscar(String serie, int correlativo) {
        return comprobantes.findFirstBySerieAndCorrelativo(serie, correlativo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Comprobante no encontrado."));
    }

    private ComprobanteDto toDto(Comprobante c, boolean conDetalle) {
        List<ComprobanteDto.Detalle> det = conDetalle
                ? c.getDetalles().stream().map(d -> new ComprobanteDto.Detalle(d.getItem(), d.getCodProducto(),
                        d.getDescripcion(), d.getUnidad(), d.getCantidad(), d.getValorUnitario(), d.getPrecioUnitario(),
                        d.getValorVenta(), d.getIgv())).toList()
                : null;
        return new ComprobanteDto(c.getId(), c.getTipoDoc(), c.getSerie(), String.valueOf(c.getCorrelativo()),
                c.getFechaEmision(), c.getMoneda(), c.getClienteId(), c.getClienteTipoDoc(), c.getClienteNumDoc(),
                c.getClienteNombre(), c.getClienteDireccion(), c.getMtoOperGravadas(), c.getMtoIgv(), c.getMtoImpVenta(),
                c.getObservacion(), c.getEstado(), c.getCodigoSunat(), c.getDescripcionSunat(), c.getHash(),
                c.getUsuarioId(), c.getCreadoEn(), det);
    }

    private Integer buscarClienteId(String tipoDoc, String numDoc) {
        if (numDoc == null) return null;
        return ("1".equals(tipoDoc) ? clientes.findFirstByDni(numDoc) : "6".equals(tipoDoc) ? clientes.findFirstByRuc(numDoc)
                : java.util.Optional.<Cliente>empty()).map(Cliente::getId).orElse(null);
    }

    private String escribir(EmitirBoletaRequest r) {
        try {
            return mapper.writeValueAsString(r);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo serializar la boleta", e);
        }
    }

    private EmitirBoletaRequest leer(String json) {
        try {
            return mapper.readValue(json, EmitirBoletaRequest.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo leer la boleta guardada", e);
        }
    }

    private static LocalDateTime parsearFecha(String texto) {
        try {
            return OffsetDateTime.parse(texto).toLocalDateTime();
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(texto);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(texto).atStartOfDay();
                } catch (DateTimeParseException e3) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA",
                            "fechaEmision no es válida. Use el formato 2026-10-07T19:00:00-05:00.");
                }
            }
        }
    }

    private static String tipoDe(EmitirBoletaRequest r) {
        String t = r.getTipoDoc() == null || r.getTipoDoc().isBlank() ? BOLETA : r.getTipoDoc().trim();
        if ("FACTURA".equalsIgnoreCase(t)) t = FACTURA;
        if ("BOLETA".equalsIgnoreCase(t)) t = BOLETA;
        if (!BOLETA.equals(t) && !FACTURA.equals(t)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TIPO_COMPROBANTE_INVALIDO",
                    "El tipo de comprobante debe ser 03 (boleta) o 01 (factura).");
        }
        return t;
    }

    static String nombreTipo(String tipoDoc) {
        return FACTURA.equals(tipoDoc) ? "FACTURA" : "BOLETA";
    }

    /** La factura solo se emite a un cliente con RUC (11 dígitos) y razón social. */
    private static void validarReceptor(String tipo, EmitirBoletaRequest r) {
        if (!FACTURA.equals(tipo)) return;
        var c = r.getCliente();
        String num = c == null || c.getNumDoc() == null ? "" : c.getNumDoc().trim();
        String nombre = c == null || c.getRznSocial() == null ? "" : c.getRznSocial().trim();
        if (!"6".equals(c == null ? null : c.getTipoDoc()) || !num.matches("\\d{11}")) {
            throw BusinessException.badRequest("FACTURA_REQUIERE_RUC",
                    "La factura se emite solo a clientes con RUC (11 dígitos).");
        }
        if (nombre.isEmpty()) {
            throw BusinessException.badRequest("FACTURA_REQUIERE_RAZON_SOCIAL", "Indique la razón social del cliente.");
        }
    }

    private static String recortar(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}


