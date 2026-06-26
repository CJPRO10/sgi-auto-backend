package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;

public record ProductoReporteDTO(
        String codigo,
        String nombre,
        String categoria,
        int stockActual,
        int stockMinimo,
        BigDecimal precioVentaDetal,
        BigDecimal precioVentaMayor,
        BigDecimal margenGananciaPct,
        boolean stockBajo
) {}