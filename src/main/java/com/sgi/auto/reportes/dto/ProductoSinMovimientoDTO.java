package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductoSinMovimientoDTO(
        String codigo,
        String nombre,
        String categoria,
        int stockActual,
        BigDecimal precioCompraSinIva,
        BigDecimal valorInmovilizadoCop,
        OffsetDateTime ultimoMovimiento, // null = nunca tuvo ningún movimiento registrado
        Long diasSinMovimiento           // null = nunca tuvo movimiento
) {}