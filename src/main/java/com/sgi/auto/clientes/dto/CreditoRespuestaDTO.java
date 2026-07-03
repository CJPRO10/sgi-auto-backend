package com.sgi.auto.clientes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CreditoRespuestaDTO(
        Long id,
        Long clienteId,
        String nombreCliente,
        BigDecimal montoTotalCop,
        BigDecimal montoPagadoCop,
        BigDecimal montoRestanteCop,
        boolean estaActivo,
        OffsetDateTime creadoEn,
        List<MovimientoCreditoDTO> movimientos
) {
    public record MovimientoCreditoDTO(
            String tipo,
            BigDecimal montoCop,
            String notas,
            OffsetDateTime fecha
    ) {}
}