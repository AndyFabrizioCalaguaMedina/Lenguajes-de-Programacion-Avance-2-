package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.EstadisticasDto.Conteo;
import com.hcg.multiservicios.dto.EstadisticasDto.Dashboard;
import com.hcg.multiservicios.dto.EstadisticasDto.MesCotizaciones;
import com.hcg.multiservicios.dto.EstadisticasDto.Punto;
import com.hcg.multiservicios.dto.EstadisticasDto.Ranking;
import com.hcg.multiservicios.dto.EstadisticasDto.Reportes;
import com.hcg.multiservicios.dto.security.AuditoriaDto;
import com.hcg.multiservicios.entity.Auditoria;
import com.hcg.multiservicios.entity.Comprobante;
import com.hcg.multiservicios.entity.ComprobanteDetalle;
import com.hcg.multiservicios.entity.Cotizacion;
import com.hcg.multiservicios.entity.SalidaBodega;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.AuditoriaRepository;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.repository.ComprobanteRepository;
import com.hcg.multiservicios.repository.CotizacionRepository;
import com.hcg.multiservicios.repository.DocumentoRepository;
import com.hcg.multiservicios.repository.OperacionRepository;
import com.hcg.multiservicios.repository.SalidaBodegaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estadisticas calculadas en memoria sobre rangos acotados de fechas (volumen de un negocio pequeno), lo que evita
 * SQL especifico de un motor y funciona igual en Supabase (PostgreSQL) y en los tests (H2).
 */
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    private static final int DIAS_DASHBOARD = 14;
    private static final int MAX_DIAS_REPORTE = 366;
    private static final List<String> ESTADOS_COTIZACION = List.of(Cotizacion.BORRADOR, Cotizacion.ENVIADA,
            Cotizacion.APROBADA, Cotizacion.RECHAZADA, Cotizacion.FACTURADA);

    private final ClienteRepository clientes;
    private final CotizacionRepository cotizaciones;
    private final DocumentoRepository documentos;
    private final ComprobanteRepository comprobantes;
    private final SalidaBodegaRepository salidas;
    private final AuditoriaRepository auditoria;
    private final OperacionRepository operaciones;

    public EstadisticasService(ClienteRepository clientes, CotizacionRepository cotizaciones,
                               DocumentoRepository documentos, ComprobanteRepository comprobantes,
                               SalidaBodegaRepository salidas, AuditoriaRepository auditoria,
                               OperacionRepository operaciones) {
        this.clientes = clientes;
        this.cotizaciones = cotizaciones;
        this.documentos = documentos;
        this.comprobantes = comprobantes;
        this.salidas = salidas;
        this.auditoria = auditoria;
        this.operaciones = operaciones;
    }

    public Dashboard dashboard(boolean verActividad) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioSerie = hoy.minusDays(DIAS_DASHBOARD - 1L);

        List<Comprobante> delMes = comprobantes.findByFechaEmisionBetween(inicioMes.atStartOfDay(), fin(hoy));
        List<Comprobante> serie = comprobantes.findByFechaEmisionBetween(inicioSerie.atStartOfDay(), fin(hoy));
        long aceptadasMes = delMes.stream().filter(c -> Comprobante.ACEPTADA.equals(c.getEstado())).count();
        long conProblema = delMes.stream().filter(c -> Comprobante.RECHAZADA.equals(c.getEstado())
                || Comprobante.ERROR.equals(c.getEstado())).count();
        BigDecimal ventasMes = sumar(delMes.stream().filter(c -> Comprobante.ACEPTADA.equals(c.getEstado())));

        long pendientes = cotizaciones.countByEstado(Cotizacion.BORRADOR) + cotizaciones.countByEstado(Cotizacion.ENVIADA);
        List<Conteo> porEstado = new ArrayList<>();
        for (String e : ESTADOS_COTIZACION) {
            porEstado.add(new Conteo(e, cotizaciones.countByEstado(e)));
        }

        List<AuditoriaDto> reciente = verActividad
                ? auditoria.findAllByOrderByFechaDescIdDesc(PageRequest.of(0, 8)).getContent().stream().map(this::aDto).toList()
                : List.of();

        return new Dashboard(clientes.countByActivo(true), pendientes, cotizaciones.countByEstado(Cotizacion.APROBADA),
                documentos.count(), ventasMes, delMes.size(), aceptadasMes, conProblema,
                salidas.findByFechaBetween(inicioMes.atStartOfDay(), fin(hoy)).stream()
                        .filter(s -> !SalidaBodega.ANULADA.equals(s.getEstado())).count(),
                ventasPorDia(serie, inicioSerie, hoy), porEstado, reciente, cotizacionesPorMes(hoy),
                operaciones.countByEstado("ABIERTA") + operaciones.countByEstado("EN_PROCESO"));
    }

    /** Ultimos 6 meses (el actual incluido): aprobadas/facturadas, pendientes y rechazadas. */
    private List<MesCotizaciones> cotizacionesPorMes(LocalDate hoy) {
        LocalDate primero = hoy.withDayOfMonth(1).minusMonths(5);
        List<Cotizacion> lista = cotizaciones.findByFechaBetween(primero, hoy);
        List<MesCotizaciones> salida = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate ini = primero.plusMonths(i);
            long ap = 0;
            long pe = 0;
            long re = 0;
            for (Cotizacion c : lista) {
                if (c.getFecha() == null || c.getFecha().getYear() != ini.getYear()
                        || c.getFecha().getMonth() != ini.getMonth()) {
                    continue;
                }
                String e = c.getEstado();
                if (Cotizacion.APROBADA.equals(e) || Cotizacion.FACTURADA.equals(e)) {
                    ap++;
                } else if (Cotizacion.RECHAZADA.equals(e)) {
                    re++;
                } else {
                    pe++;
                }
            }
            salida.add(new MesCotizaciones(String.format("%d-%02d", ini.getYear(), ini.getMonthValue()), ap, pe, re));
        }
        return salida;
    }

    public Reportes reportes(LocalDate desde, LocalDate hasta, boolean verActividad) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate ini = desde != null ? desde : fin.minusDays(29);
        if (ini.isAfter(fin)) {
            throw BusinessException.badRequest("RANGO_INVALIDO", "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        if (ini.plusDays(MAX_DIAS_REPORTE).isBefore(fin)) {
            throw BusinessException.badRequest("RANGO_MUY_GRANDE", "El rango máximo es de un año.");
        }

        List<Comprobante> todas = comprobantes.findByFechaEmisionBetween(ini.atStartOfDay(), fin(fin));
        List<Comprobante> aceptadas = todas.stream().filter(c -> Comprobante.ACEPTADA.equals(c.getEstado())).toList();
        BigDecimal total = sumar(aceptadas.stream());
        BigDecimal promedio = aceptadas.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(aceptadas.size()), 2, RoundingMode.HALF_UP);

        Map<String, long[]> prodCant = new LinkedHashMap<>();
        Map<String, BigDecimal> prodTotal = new LinkedHashMap<>();
        for (Comprobante c : aceptadas) {
            for (ComprobanteDetalle d : c.getDetalles()) {
                String clave = d.getDescripcion() == null ? "(sin descripción)" : d.getDescripcion();
                BigDecimal monto = nz(d.getValorVenta()).add(nz(d.getIgv()));
                prodTotal.merge(clave, monto, BigDecimal::add);
                prodCant.computeIfAbsent(clave, k -> new long[1])[0] += d.getCantidad() == null ? 0 : d.getCantidad().longValue();
            }
        }
        List<Ranking> topProductos = prodTotal.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).limit(5)
                .map(e -> new Ranking(e.getKey(), e.getValue(), prodCant.get(e.getKey())[0])).toList();

        Map<String, BigDecimal> cliTotal = new LinkedHashMap<>();
        Map<String, long[]> cliCant = new LinkedHashMap<>();
        for (Comprobante c : aceptadas) {
            String n = c.getClienteNombre() == null ? "(sin nombre)" : c.getClienteNombre();
            cliTotal.merge(n, nz(c.getMtoImpVenta()), BigDecimal::add);
            cliCant.computeIfAbsent(n, k -> new long[1])[0]++;
        }
        List<Ranking> topClientes = cliTotal.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()).limit(5)
                .map(e -> new Ranking(e.getKey(), e.getValue(), cliCant.get(e.getKey())[0])).toList();

        List<Conteo> cotPorEstado = contar(cotizaciones.findByFechaBetween(ini, fin).stream().map(Cotizacion::getEstado));
        List<Conteo> salidasPorMotivo = contar(salidas.findByFechaBetween(ini.atStartOfDay(), fin(fin)).stream()
                .filter(s -> !SalidaBodega.ANULADA.equals(s.getEstado())).map(SalidaBodega::getMotivo));

        List<Conteo> porUsuario = List.of();
        List<Conteo> porAccion = List.of();
        if (verActividad) {
            List<Auditoria> acts = auditoria.findByFechaBetween(ini.atStartOfDay(), fin(fin));
            porUsuario = contar(acts.stream().filter(a -> a.getUsername() != null).map(Auditoria::getUsername));
            porAccion = contar(acts.stream().map(Auditoria::getAccion));
        }

        return new Reportes(ini, fin, total, aceptadas.size(), promedio, ventasPorDia(aceptadas, ini, fin),
                contar(todas.stream().map(Comprobante::getEstado)), topProductos, topClientes, cotPorEstado,
                salidasPorMotivo, porUsuario, porAccion);
    }

    // --- utilidades ---

    private static List<Punto> ventasPorDia(List<Comprobante> lista, LocalDate ini, LocalDate fin) {
        Map<LocalDate, BigDecimal> totales = new TreeMap<>();
        Map<LocalDate, Long> cantidades = new TreeMap<>();
        for (LocalDate d = ini; !d.isAfter(fin); d = d.plusDays(1)) {
            totales.put(d, BigDecimal.ZERO);
            cantidades.put(d, 0L);
        }
        for (Comprobante c : lista) {
            if (!Comprobante.ACEPTADA.equals(c.getEstado())) continue;
            LocalDate d = c.getFechaEmision().toLocalDate();
            if (totales.containsKey(d)) {
                totales.merge(d, nz(c.getMtoImpVenta()), BigDecimal::add);
                cantidades.merge(d, 1L, Long::sum);
            }
        }
        List<Punto> puntos = new ArrayList<>();
        totales.forEach((d, t) -> puntos.add(new Punto(d, t, cantidades.get(d))));
        return puntos;
    }

    private static List<Conteo> contar(java.util.stream.Stream<String> valores) {
        Map<String, Long> m = new LinkedHashMap<>();
        valores.forEach(v -> m.merge(v == null ? "—" : v, 1L, Long::sum));
        return m.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .map(e -> new Conteo(e.getKey(), e.getValue())).toList();
    }

    private static BigDecimal sumar(java.util.stream.Stream<Comprobante> s) {
        return s.map(c -> nz(c.getMtoImpVenta())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static LocalDateTime fin(LocalDate d) {
        return d.atTime(LocalTime.MAX);
    }

    private AuditoriaDto aDto(Auditoria a) {
        return new AuditoriaDto(a.getId(), a.getUsuarioId(), a.getUsername(), a.getAccion(), a.getEntidad(),
                a.getEntidadId(), a.getValorAnterior(), a.getValorNuevo(), a.getFecha());
    }
}
