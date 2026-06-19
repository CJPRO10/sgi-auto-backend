// LoginSolicitudDTO.java
package com.sgi.auto.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginSolicitudDTO(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {}