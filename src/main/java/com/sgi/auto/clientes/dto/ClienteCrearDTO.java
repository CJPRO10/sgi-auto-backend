package com.sgi.auto.clientes.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ClienteCrearDTO(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 200)
        String nombreCompleto,

        @NotBlank(message = "El tipo de identificación es obligatorio")
        @Pattern(regexp = "CC|NIT", message = "El tipo debe ser CC o NIT")
        String tipoIdentificacion,

        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(max = 20)
        String numeroIdentificacion,

        String direccion,

        @Size(max = 20)
        String celular,

        @Email(message = "El correo no tiene formato válido")
        String correo
) {}