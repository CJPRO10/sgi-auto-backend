package com.sgi.auto.inventario.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CreditoProveedorRespuestaDTO(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        Long entradaId,
        BigDecimal montoTotalCop,
        BigDecimal montoPagadoCop,
        BigDecimal montoRestanteCop,
        boolean estaActivo,
        String notas,
        OffsetDateTime creadoEn,
        List<PagoDTO> pagos
) {
    public record PagoDTO(
            Long id,
            BigDecimal montoCop,
            String notas,
            String registradoPor,
            OffsetDateTime creadoEn
    ) {}
}