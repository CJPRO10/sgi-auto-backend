package com.sgi.auto.inventario;

import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_movimiento", nullable = false)
    private TipoMovimientoStock tipoMovimiento;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "stock_antes", nullable = false)
    private int stockAntes;

    @Column(name = "stock_despues", nullable = false)
    private int stockDespues;

    @Column(name = "costo_unitario_cop", precision = 14, scale = 2)
    private BigDecimal costoUnitarioCop;

    @Column(name = "notas")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}