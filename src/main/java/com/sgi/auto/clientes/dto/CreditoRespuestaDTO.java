package com.sgi.auto.clientes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreditoRespuestaDTO(
        Long id,
        Long clienteId,
        String nombreCliente,
        BigDecimal montoTotalCop,
        BigDecimal montoPagadoCop,
        BigDecimal montoRestanteCop,
        boolean estaActivo,
        OffsetDateTime creadoEn
) {}