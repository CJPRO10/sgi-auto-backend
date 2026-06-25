package com.sgi.auto.pos;

import com.sgi.auto.inventario.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "items_venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre_producto_snapshot", nullable = false, length = 200)
    private String nombreProductoSnapshot;

    @Column(name = "codigo_producto_snapshot", nullable = false, length = 50)
    private String codigoProductoSnapshot;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitarioCop;

    @Column(name = "descuento_unitario_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal descuentoUnitarioCop = BigDecimal.ZERO;

    @Column(name = "subtotal_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCop;

    // True si el item viene de una OT
    @Column(name = "desde_ot", nullable = false)
    @Builder.Default
    private boolean desdeOt = false;
}