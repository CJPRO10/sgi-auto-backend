package com.sgi.auto.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoCreditoDTO(
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal montoCop,
        String notas
) {}