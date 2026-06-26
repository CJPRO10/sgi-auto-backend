package com.sgi.auto.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HabilitarCreditoDTO(
        @NotNull(message = "El monto del crédito es obligatorio")
        @DecimalMin(value = "1.0", message = "El monto debe ser mayor a cero")
        BigDecimal montoTotalCop
) {}