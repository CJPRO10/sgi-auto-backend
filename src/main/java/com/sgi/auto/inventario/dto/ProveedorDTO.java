package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorDTO(
        Long id,
        @NotBlank String nombre,
        @Size(max = 20) String nit,
        @Size(max = 20) String telefono,
        @Size(max = 150) String correo,
        String direccion,
        String personaContacto,
        String notas,
        boolean estaActivo
) {}