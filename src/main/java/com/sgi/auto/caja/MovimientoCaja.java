package com.sgi.auto.caja;

import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Registro de cada movimiento dentro de una sesión de caja.
 * RF-045 — Movimientos automáticos
 * RF-046 — Gastos operativos
 * RF-051 — Egresos autorizados
 */
@Entity
@Table(name = "movimientos_caja")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionCaja sesion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false)
    private TipoMovimientoCaja tipo;

    @Column(name = "monto_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoCop;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    // Referencia opcional a la venta que generó el movimiento
    @Column(name = "venta_id")
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}
