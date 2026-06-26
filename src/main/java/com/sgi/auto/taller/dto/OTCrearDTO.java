package com.sgi.auto.taller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OTCrearDTO(

        Long clienteId,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombreCliente,

        String celularCliente,

        @NotBlank(message = "La placa es obligatoria")
        @Size(max = 10)
        String placa,

        String marcaVehiculo,
        String modeloVehiculo,
        Integer anioVehiculo,
        String colorVehiculo,
        Integer kilometraje,

        @NotBlank(message = "La descripción del problema es obligatoria")
        String descripcionProblema,

        Long mecanicoId,
        OffsetDateTime fechaPrometidaEntrega,
        BigDecimal descuentoCop
) {}
