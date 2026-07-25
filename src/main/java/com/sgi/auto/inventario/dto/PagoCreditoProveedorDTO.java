package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoCreditoProveedorDTO(
        @NotNull @DecimalMin("0.01") BigDecimal montoCop,
        String notas
) {}
