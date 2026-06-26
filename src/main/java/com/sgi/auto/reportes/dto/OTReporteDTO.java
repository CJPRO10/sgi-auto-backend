package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OTReporteDTO(
        Long id,
        String placa,
        String nombreCliente,
        String mecanicoNombre,
        String estado,
        BigDecimal granTotalCop,
        OffsetDateTime fechaIngreso,
        OffsetDateTime fechaEntrega
) {}
