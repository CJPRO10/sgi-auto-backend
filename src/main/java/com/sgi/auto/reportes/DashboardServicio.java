package com.sgi.auto.reportes;

import com.sgi.auto.caja.SesionCajaRepositorio;
import com.sgi.auto.clientes.CreditoRepositorio;
import com.sgi.auto.inventario.ProductoRepositorio;
import com.sgi.auto.pos.EstadoVenta;
import com.sgi.auto.pos.VentaRepositorio;
import com.sgi.auto.reportes.dto.DashboardDTO;
import com.sgi.auto.taller.EstadoOT;
import com.sgi.auto.taller.OrdenDeTrabajoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Servicio de dashboards y métricas.
 * RF-096 al RF-102
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServicio {

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final OrdenDeTrabajoRepositorio otRepositorio;
    private final CreditoRepositorio creditoRepositorio;
    private final SesionCajaRepositorio sesionCajaRepositorio;

    private static final ZoneOffset ZONA_CO = ZoneOffset.of("-05:00");

    /**
     * Dashboard principal con todos los indicadores.
     * RF-096
     */
    @Transactional(readOnly = true)
    public DashboardDTO obtenerDashboard() {
        return new DashboardDTO(
                calcularResumenDia(),
                calcularIndicadoresInventario(),
                calcularEstadoTaller(),
                calcularCarteraCredito(),
                calcularResumenCaja()
        );
    }

    // ── RF-096 — Resumen del día ──────────────────────

    private DashboardDTO.ResumenDiaDTO calcularResumenDia() {
        OffsetDateTime inicioDia = LocalDate.now()
                .atStartOfDay().atOffset(ZONA_CO);

        var ventasHoy = ventaRepositorio.findAll().stream()
                .filter(v -> v.getEstado() == EstadoVenta.COMPLETADA
                        && v.getCreadoEn() != null
                        && !v.getCreadoEn().isBefore(inicioDia))
                .toList();

        BigDecimal ingresos = ventasHoy.stream()
                .map(v -> v.getTotalCop() != null ? v.getTotalCop() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int puntosOtorgados = ventasHoy.stream()
                .mapToInt(v -> v.getPuntosGanados())
                .sum();

        long clientesAtendidos = ventasHoy.stream()
                .filter(v -> v.getCliente() != null)
                .map(v -> v.getCliente().getId())
                .distinct()
                .count();

        return new DashboardDTO.ResumenDiaDTO(
                LocalDate.now(),
                ventasHoy.size(),
                ingresos,
                puntosOtorgados,
                (int) clientesAtendidos);
    }

    // ── RF-097 — Indicadores de inventario ───────────

    private DashboardDTO.IndicadoresInventarioDTO calcularIndicadoresInventario() {
        var productos = productoRepositorio.findAll().stream()
                .filter(p -> p.getEliminadoEn() == null && p.isEstaActivo())
                .toList();

        int stockBajo = (int) productos.stream()
                .filter(p -> p.getStockActual() > 0
                        && p.getStockActual() <= p.getStockMinimo())
                .count();

        int agotados = (int) productos.stream()
                .filter(p -> p.getStockActual() == 0)
                .count();

        BigDecimal valorTotal = productos.stream()
                .map(p -> p.getPrecioCompraConIva() != null
                        ? p.getPrecioCompraConIva()
                        .multiply(BigDecimal.valueOf(p.getStockActual()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDTO.IndicadoresInventarioDTO(
                productos.size(), stockBajo, agotados, valorTotal);
    }

    // ── RF-098 — Estado del taller ────────────────────

    private DashboardDTO.EstadoTallerDTO calcularEstadoTaller() {
        var otsActivas = otRepositorio.findAll().stream()
                .filter(o -> o.getEstado() != EstadoOT.ENTREGADO
                        && o.getEstado() != EstadoOT.CANCELADO)
                .toList();

        long recibidas = contarPorEstado(otsActivas, EstadoOT.RECIBIDO);
        long diagnostico = contarPorEstado(otsActivas, EstadoOT.EN_DIAGNOSTICO);
        long reparacion = contarPorEstado(otsActivas, EstadoOT.EN_REPARACION);
        long esperando = contarPorEstado(otsActivas, EstadoOT.ESPERANDO_REPUESTO);
        long listas = contarPorEstado(otsActivas, EstadoOT.LISTO);

        return new DashboardDTO.EstadoTallerDTO(
                otsActivas.size(),
                (int) recibidas, (int) diagnostico,
                (int) reparacion, (int) esperando, (int) listas);
    }

    private long contarPorEstado(
            List<com.sgi.auto.taller.OrdenDeTrabajo> ots, EstadoOT estado) {
        return ots.stream().filter(o -> o.getEstado() == estado).count();
    }

    // ── RF-099 — Cartera de créditos ──────────────────

    private DashboardDTO.CarteraCreditoDTO calcularCarteraCredito() {
        var creditos = creditoRepositorio.findAll().stream()
                .filter(c -> c.isEstaActivo())
                .toList();

        BigDecimal totalDeuda = creditos.stream()
                .map(c -> c.getMontoTotalCop() != null
                        ? c.getMontoTotalCop() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagado = creditos.stream()
                .map(c -> c.getMontoPagadoCop() != null
                        ? c.getMontoPagadoCop() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRestante = totalDeuda.subtract(totalPagado);

        return new DashboardDTO.CarteraCreditoDTO(
                creditos.size(), totalDeuda, totalPagado, totalRestante);
    }

    // ── RF-100 — Resumen de caja ──────────────────────

    private DashboardDTO.ResumenCajaDTO calcularResumenCaja() {
        return sesionCajaRepositorio.buscarSesionAbierta()
                .map(s -> {
                    BigDecimal saldoEsperado = s.getSaldoInicialCop()
                            .add(s.getTotalVentasCop())
                            .add(s.getTotalAbonosCreditoCop())
                            .subtract(s.getTotalGastosCop());
                    return new DashboardDTO.ResumenCajaDTO(
                            true,
                            s.getSaldoInicialCop(),
                            s.getTotalVentasCop(),
                            s.getTotalGastosCop(),
                            saldoEsperado);
                })
                .orElse(new DashboardDTO.ResumenCajaDTO(
                        false, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO));
    }
}