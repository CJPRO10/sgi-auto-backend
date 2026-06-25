package com.sgi.auto.inventario.dto;

import com.sgi.auto.inventario.TipoMovimientoStock;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record KardexRespuestaDTO(
        Long id,
        TipoMovimientoStock tipoMovimiento,
        int cantidad,
        int stockAntes,
        int stockDespues,
        BigDecimal costoUnitarioCop,
        String notas,
        String registradoPor,
        OffsetDateTime creadoEn
) {}