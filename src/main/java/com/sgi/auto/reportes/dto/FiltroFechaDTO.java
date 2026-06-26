package com.sgi.auto.reportes.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record FiltroFechaDTO(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta
) {
    public FiltroFechaDTO {
        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();
    }
}