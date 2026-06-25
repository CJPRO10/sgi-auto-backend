package com.sgi.auto.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GastoDTO(
        @NotNull
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal montoCop,

        @NotBlank(message = "La descripción del gasto es obligatoria")
        String descripcion
) {}
