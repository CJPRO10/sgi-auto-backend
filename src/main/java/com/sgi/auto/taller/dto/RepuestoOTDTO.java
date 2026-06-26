package com.sgi.auto.taller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RepuestoOTDTO(
        @NotNull Long productoId,
        @Min(1) int cantidad,
        @NotNull BigDecimal precioUnitarioCop
) {}
