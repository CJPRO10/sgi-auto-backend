package com.sgi.auto.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaDTO(
        @NotBlank String contrasenaActual,
        @NotBlank @Size(min = 8) String contrasenaNueva
) {}