package com.sgi.auto.clientes;

import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Registro histórico de cada movimiento de puntos de un cliente.
 * RF-009, RF-014, RF-015, RF-037, RF-079
 */
@Entity
@Table(name = "historial_puntos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_movimiento", nullable = false)
    private TipoMovimientoPuntos tipoMovimiento;

    @Column(name = "puntos", nullable = false)
    private int puntos;

    @Column(name = "saldo_antes", nullable = false)
    private int saldoAntes;

    @Column(name = "saldo_despues", nullable = false)
    private int saldoDespues;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "venta_id")
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}