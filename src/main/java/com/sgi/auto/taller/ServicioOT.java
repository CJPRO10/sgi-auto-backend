package com.sgi.auto.taller;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios_ot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServicioOT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenDeTrabajo ordenDeTrabajo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "precio_unitario_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitarioCop;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private int cantidad = 1;

    @Column(name = "subtotal_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalCop;
}
