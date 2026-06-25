package com.sgi.auto.inventario;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "items_entrada")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = false)
    private EntradaMercancia entrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "costo_unitario_con_iva", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitarioConIva;

    @Column(name = "costo_unitario_sin_iva", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitarioSinIva;

    @Column(name = "subtotal_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCop;
}