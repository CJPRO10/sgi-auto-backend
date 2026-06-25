package com.sgi.auto.clientes.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ClienteRespuestaDTO(
        Long id,
        String nombreCompleto,
        String tipoIdentificacion,
        String numeroIdentificacion,
        String direccion,
        String celular,
        String correo,
        boolean creditoHabilitado,
        BigDecimal cupoCreditoCop,
        BigDecimal saldoCreditoCop,
        int saldoPuntos,
        OffsetDateTime creadoEn
) {}