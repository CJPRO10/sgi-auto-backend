package com.sgi.auto.clientes.dto;

import com.sgi.auto.clientes.TipoMovimientoPuntos;
import java.time.OffsetDateTime;

public record HistorialPuntosRespuestaDTO(
        Long id,
        TipoMovimientoPuntos tipoMovimiento,
        int puntos,
        int saldoAntes,
        int saldoDespues,
        String descripcion,
        OffsetDateTime creadoEn
) {}