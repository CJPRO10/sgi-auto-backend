package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record EntradaMercanciaCrearDTO(
        Long proveedorId,
        String numeroFacturaProveedor,
        String notas,

        @NotEmpty(message = "La entrada debe tener al menos un producto")
        List<ItemEntradaDTO> items
) {
    public record ItemEntradaDTO(
            @NotNull Long productoId,
            @Min(1) int cantidad,
            @NotNull BigDecimal costoUnitarioConIva,
            BigDecimal costoUnitarioSinIva
    ) {}
}