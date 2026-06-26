package com.sgi.auto.clientes;

import com.sgi.auto.compartido.EntidadBase;
import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Crédito habilitado para un cliente.
 * RF-012, RF-013, RF-016
 */
@Entity
@Table(name = "creditos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Credito extends EntidadBase {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(name = "monto_total_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotalCop;

    @Column(name = "monto_pagado_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal montoPagadoCop = BigDecimal.ZERO;

    // monto_restante_cop es columna GENERATED en BD
    @Column(name = "monto_restante_cop", insertable = false, updatable = false,
            precision = 14, scale = 2)
    private BigDecimal montoRestanteCop;

    @Column(name = "esta_activo", nullable = false)
    @Builder.Default
    private boolean estaActivo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private Usuario aprobadoPor;

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PagoCredito> pagos = new ArrayList<>();
}