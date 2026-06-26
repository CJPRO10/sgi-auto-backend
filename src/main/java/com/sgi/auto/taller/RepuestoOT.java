package com.sgi.auto.taller;

import com.sgi.auto.inventario.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Repuesto usado en una OT.
 * RF-060 — Registro de repuestos
 * RF-061 — Descuento de inventario desde OT
 */
@Entity
@Table(name = "repuestos_ot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepuestoOT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id", nullable = false)
    private OrdenDeTrabajo ordenDeTrabajo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "nombre_repuesto_snapshot", nullable = false, length = 200)
    private String nombreRepuestoSnapshot;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "valor_unitario_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitarioCop;

    @Column(name = "subtotal_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCop;

    // true = el stock ya fue descontado del inventario (RF-061)
    @Column(name = "stock_descontado", nullable = false)
    @Builder.Default
    private boolean stockDescontado = false;
}
