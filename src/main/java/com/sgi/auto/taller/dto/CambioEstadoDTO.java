package com.sgi.auto.taller.dto;

import com.sgi.auto.taller.EstadoOT;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoOT nuevoEstado,
        String observaciones
) {}
