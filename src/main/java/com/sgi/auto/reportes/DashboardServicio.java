package com.sgi.auto.reportes;

import com.sgi.auto.caja.SesionCaja;
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
import org.springframework.security.core.Authentication;
import com.sgi.auto.usuarios.UsuarioRepositorio;
import com.sgi.auto.reportes.dto.DashboardMecanicoDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServicio {

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final OrdenDeTrabajoRepositorio otRepositorio;
    private final CreditoRepositorio creditoRepositorio;
    private final SesionCajaRepositorio sesionCajaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
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

    // ── Resumen del día ──────────────────────

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
    @Transactional(readOnly = true)
    public DashboardMecanicoDTO obtenerDashboardMecanico(String nombreUsuario) {
        var mecanico = usuarioRepositorio.buscarPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new com.sgi.auto.compartido.RecursoNoEncontradoExcepcion(
                        "Usuario no encontrado"));

        var todasLasOts = otRepositorio.findAll();

        var otsMecanico = todasLasOts.stream()
                .filter(o -> o.getMecanico() != null
                        && o.getMecanico().getId().equals(mecanico.getId()))
                .toList();

        long enReparacion = otsMecanico.stream()
                .filter(o -> o.getEstado() == com.sgi.auto.taller.EstadoOT.EN_REPARACION)
                .count();

        long listas = otsMecanico.stream()
                .filter(o -> o.getEstado() == com.sgi.auto.taller.EstadoOT.LISTO)
                .count();

        long activas = otsMecanico.stream()
                .filter(o -> o.getEstado() != com.sgi.auto.taller.EstadoOT.ENTREGADO
                        && o.getEstado() != com.sgi.auto.taller.EstadoOT.CANCELADO)
                .count();

        var hoy = java.time.LocalDate.now();
        long entregadasHoy = otsMecanico.stream()
                .filter(o -> o.getEstado() == com.sgi.auto.taller.EstadoOT.ENTREGADO
                        && o.getFechaEntregaReal() != null
                        && o.getFechaEntregaReal().toLocalDate().equals(hoy))
                .count();

        var inicioMes = hoy.withDayOfMonth(1).atStartOfDay()
                .atOffset(java.time.ZoneOffset.of("-05:00"));

        var otsMes = otsMecanico.stream()
                .filter(o -> o.getCreadoEn() != null
                        && !o.getCreadoEn().isBefore(inicioMes))
                .toList();

        java.math.BigDecimal totalFacturado = otsMes.stream()
                .map(o -> o.getGranTotalCop() != null
                        ? o.getGranTotalCop() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return new DashboardMecanicoDTO(
                mecanico.getNombreCompleto(),
                hoy,
                (int) activas,
                (int) enReparacion,
                (int) listas,
                (int) entregadasHoy,
                otsMes.size(),
                totalFacturado
        );
    }

    // ── Indicadores de inventario ───────────

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
        List<SesionCaja> sesionesAbiertas = sesionCajaRepositorio.listarSesionesAbiertas();

        if (sesionesAbiertas.isEmpty()) {
            return new DashboardDTO.ResumenCajaDTO(
                    false, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalVentas = sesionesAbiertas.stream()
                .map(SesionCaja::getTotalVentasCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGastos = sesionesAbiertas.stream()
                .map(SesionCaja::getTotalGastosCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAbonos = sesionesAbiertas.stream()
                .map(SesionCaja::getTotalAbonosCreditoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoInicial = sesionesAbiertas.stream()
                .map(SesionCaja::getSaldoInicialCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoEsperado = saldoInicial
                .add(totalVentas)
                .add(totalAbonos)
                .subtract(totalGastos);

        return new DashboardDTO.ResumenCajaDTO(
                true,
                saldoInicial,
                totalVentas,
                totalGastos,
                saldoEsperado);
    }
}