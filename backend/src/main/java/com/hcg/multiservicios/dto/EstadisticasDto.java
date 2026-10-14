package com.hcg.multiservicios.dto;

import com.hcg.multiservicios.dto.security.AuditoriaDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Respuestas de estadisticas para el dashboard y los reportes. */
public final class EstadisticasDto {

    private EstadisticasDto() {
    }

    public record Punto(LocalDate fecha, BigDecimal total, long cantidad) {
    }

    public record Conteo(String nombre, long cantidad) {
    }

    public record Ranking(String nombre, BigDecimal total, long cantidad) {
    }

    /** Cotizaciones de un mes agrupadas para el grafico de barras. */
    public record MesCotizaciones(String mes, long aprobadas, long pendientes, long rechazadas) {
    }

    public record Dashboard(long clientes, long cotizacionesPendientes, long cotizacionesAprobadas, long documentos,
                            BigDecimal ventasMes, long boletasMes, long boletasAceptadas, long boletasConProblema,
                            long salidasMes, List<Punto> ventasPorDia, List<Conteo> cotizacionesPorEstado,
                            List<AuditoriaDto> actividadReciente, List<MesCotizaciones> cotizacionesPorMes,
                            long operacionesActivas) {
    }

    public record Reportes(LocalDate desde, LocalDate hasta, BigDecimal ventasTotal, long boletasAceptadas,
                           BigDecimal ticketPromedio, List<Punto> ventasPorDia, List<Conteo> boletasPorEstado,
                           List<Ranking> topProductos, List<Ranking> topClientes, List<Conteo> cotizacionesPorEstado,
                           List<Conteo> salidasPorMotivo, List<Conteo> actividadPorUsuario,
                           List<Conteo> actividadPorAccion) {
    }
}
