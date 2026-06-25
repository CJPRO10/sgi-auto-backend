package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjusteStockDTO(
        @NotNull(message = "La cantidad es obligatoria")
        int cantidad,  // positivo = incremento, negativo = decremento

        @NotBlank(message = "La razón del ajuste es obligatoria")
        String notas
) {}