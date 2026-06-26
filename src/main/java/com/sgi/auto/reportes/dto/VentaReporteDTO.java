package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VentaReporteDTO(
        Long id,
        String nombreCliente,
        String metodoPago,
        BigDecimal totalCop,
        int cantidadItems,
        OffsetDateTime fecha
) {}