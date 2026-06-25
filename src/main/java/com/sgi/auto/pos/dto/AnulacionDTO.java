package com.sgi.auto.pos.dto;

import jakarta.validation.constraints.NotBlank;

public record AnulacionDTO(
        @NotBlank(message = "La razón de anulación es obligatoria")
        String razon
) {}