package com.sgi.auto.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ClienteActualizarDTO(
        @Size(max = 200)
        String nombreCompleto,
        String direccion,
        @Size(max = 20)
        String celular,
        @Email
        String correo
) {}