package com.sgi.auto.inventario;

import com.sgi.auto.compartido.EntidadBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creditos_proveedor")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditoProveedor extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id")
    private EntradaMercancia entrada;

    @Column(name = "monto_total_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotalCop;

    @Column(name = "monto_pagado_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal montoPagadoCop = BigDecimal.ZERO;

    @Column(name = "notas")
    private String notas;

    @Column(name = "esta_activo", nullable = false)
    @Builder.Default
    private boolean estaActivo = true;

    @OneToMany(mappedBy = "credito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PagoCreditoProveedor> pagos = new ArrayList<>();
}