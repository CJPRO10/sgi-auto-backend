package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO principal del dashboard.
 * RF-096 al RF-102
 */
public record DashboardDTO(

        // RF-096 — Resumen del día
        ResumenDiaDTO resumenDia,

        // RF-097 — Indicadores de inventario
        IndicadoresInventarioDTO inventario,

        // RF-098 — Estado del taller
        EstadoTallerDTO taller,

        // RF-099 — Cartera de créditos
        CarteraCreditoDTO cartera,

        // RF-100 — Resumen de caja actual
        ResumenCajaDTO caja
) {
    public record ResumenDiaDTO(
            LocalDate fecha,
            int totalVentas,
            BigDecimal ingresosCop,
            int puntosOtorgados,
            int clientesAtendidos
    ) {}

    public record IndicadoresInventarioDTO(
            int totalProductos,
            int productosStockBajo,
            int productosAgotados,
            BigDecimal valorTotalInventarioCop
    ) {}

    public record EstadoTallerDTO(
            int otsTotalesActivas,
            int otsRecibidas,
            int otsEnDiagnostico,
            int otsEnReparacion,
            int otsEsperandoRepuesto,
            int otsListas
    ) {}

    public record CarteraCreditoDTO(
            int totalCreditosActivos,
            BigDecimal totalDeudaCop,
            BigDecimal totalPagadoCop,
            BigDecimal totalRestanteCop
    ) {}

    public record ResumenCajaDTO(
            boolean cajaAbierta,
            BigDecimal saldoInicialCop,
            BigDecimal totalVentasCop,
            BigDecimal totalGastosCop,
            BigDecimal saldoEsperadoCop
    ) {}
}
