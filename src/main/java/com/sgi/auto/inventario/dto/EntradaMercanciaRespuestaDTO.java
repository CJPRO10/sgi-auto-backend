package com.sgi.auto.inventario.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record EntradaMercanciaRespuestaDTO(
        Long id,
        String proveedorNombre,
        String numeroFacturaProveedor,
        BigDecimal costoTotalCop,
        String notas,
        String registradoPor,
        OffsetDateTime creadoEn,
        List<ItemRespuestaDTO> items
) {
    public record ItemRespuestaDTO(
            Long id,
            Long productoId,
            String productoNombre,
            String productoCodigo,
            int cantidad,
            BigDecimal costoUnitarioConIva,
            BigDecimal subtotalCop
    ) {}
}