package com.sgi.auto.taller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ServicioOTDTO(
        @NotBlank String descripcion,
        @NotNull @DecimalMin("0.01") BigDecimal precioUnitarioCop,
        @Min(1) int cantidad
) {}
