package com.sgi.auto.pos.dto;

import com.sgi.auto.pos.MetodoPago;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record VentaCrearDTO(

        // UUID generado en el frontend para idempotencia offline
        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String claveIdempotencia,

        Long clienteId,
        String nombreClienteAnonimo,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<ItemVentaDTO> items,

        BigDecimal descuentoCop,
        BigDecimal puntosCanjeadosCop,
        BigDecimal montoPagadoCop,
        BigDecimal montoEfectivoCop,
        BigDecimal montoTransferenciaCop,
        BigDecimal montoCreditoCop
) {
    public record ItemVentaDTO(
            @NotNull Long productoId,
            @Min(1) int cantidad,
            @NotNull BigDecimal precioUnitarioCop,
            BigDecimal descuentoUnitarioCop
    ) {}
}