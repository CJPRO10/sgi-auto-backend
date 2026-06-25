package com.sgi.auto.inventario.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoCrearDTO(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50)
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200)
        String nombre,

        String descripcion,

        Long categoriaId,
        Long proveedorId,

        @Size(max = 30)
        String unidadMedida,

        @NotNull(message = "El precio de compra con IVA es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal precioCompraConIva,

        @NotNull(message = "El precio de compra sin IVA es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal precioCompraSinIva,

        @NotNull(message = "El precio de venta detal es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal precioVentaDetal,

        @NotNull(message = "El precio de venta mayor es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal precioVentaMayor,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        int stockMinimo,

        boolean mostrarEnListaPrecios
) {}