package com.sgi.auto.caja;

import com.sgi.auto.compartido.EntidadBase;
import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sesiones_caja")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SesionCaja extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "abierta_por", nullable = false)
    private Usuario cajera;

    @Column(name = "monto_apertura_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicialCop = BigDecimal.ZERO;

    @Column(name = "efectivo_declarado_cop", precision = 14, scale = 2)
    private BigDecimal saldoFinalCop;

    @Column(name = "efectivo_teorico_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalVentasCop = BigDecimal.ZERO;

    @Column(name = "total_gastos_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGastosCop = BigDecimal.ZERO;

    @Column(name = "total_abonos_credito_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAbonosCreditoCop = BigDecimal.ZERO;

    @Column(name = "diferencia_cop", precision = 14, scale = 2)
    private BigDecimal diferenciaCop;

    @Column(name = "notas_cierre")
    private String notasCierre;

    @Column(name = "abierta_en", nullable = false)
    @Builder.Default
    private OffsetDateTime abiertaEn = OffsetDateTime.now();

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    @Column(name = "esta_abierta", nullable = false)
    @Builder.Default
    private boolean estaAbierta = true;

    @Column(name = "total_efectivo_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalEfectivoCop = BigDecimal.ZERO;

    @Column(name = "total_transferencia_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalTransferenciaCop = BigDecimal.ZERO;

    @Column(name = "total_credito_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCreditoCop = BigDecimal.ZERO;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MovimientoCaja> movimientos = new ArrayList<>();
}