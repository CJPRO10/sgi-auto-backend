package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreditoProveedorCrearDTO(
        @NotNull Long proveedorId,
        Long entradaId,
        @NotNull @DecimalMin("0.01") BigDecimal montoTotalCop,
        String notas
) {}