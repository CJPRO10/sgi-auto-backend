package com.sgi.auto.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CierreCajaDTO(
        @NotNull(message = "El saldo final contado es obligatorio")
        @DecimalMin(value = "0.0")
        BigDecimal saldoFinalContadoCop,

        String notas
) {}
