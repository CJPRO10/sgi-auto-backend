package com.sgi.auto.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardMecanicoDTO(
        String nombreMecanico,
        LocalDate fecha,
        int otsActivasHoy,
        int otsEnReparacion,
        int otsListas,
        int otsEntregadasHoy,
        int otsTotalesDelMes,
        BigDecimal totalFacturadoMesCop
) {}