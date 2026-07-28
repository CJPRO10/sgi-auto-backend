package com.sgi.auto.caja.dto;

import com.sgi.auto.caja.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SesionCajaRespuestaDTO(
        Long id,
        String cajera,
        BigDecimal saldoInicialCop,
        BigDecimal saldoFinalCop,
        BigDecimal totalVentasCop,
        BigDecimal totalEfectivoCop,
        BigDecimal totalTransferenciaCop,
        BigDecimal totalCreditoCop,
        BigDecimal totalGastosCop,
        BigDecimal totalAbonosCreditoCop,
        BigDecimal diferenciaCop,
        BigDecimal saldoEsperadoCop,
        boolean estaAbierta,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn,
        String notasCierre,
        List<MovimientoRespuestaDTO> movimientos
) {
    public record MovimientoRespuestaDTO(
            Long id,
            TipoMovimientoCaja tipo,
            BigDecimal montoCop,
            String descripcion,
            String registradoPor,
            OffsetDateTime creadoEn
    ) {}
}