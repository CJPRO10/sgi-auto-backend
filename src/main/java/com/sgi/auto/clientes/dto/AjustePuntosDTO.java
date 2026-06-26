package com.sgi.auto.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjustePuntosDTO(
        @NotNull(message = "La cantidad de puntos es obligatoria")
        int puntos,  // positivo = agregar, negativo = quitar

        @NotBlank(message = "La descripción del ajuste es obligatoria")
        String descripcion
) {}