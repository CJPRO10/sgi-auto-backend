package com.sgi.auto.clientes;

import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Pago o abono a un crédito.
 * RF-050 — Abono a crédito desde caja
 */
@Entity
@Table(name = "pagos_credito")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id", nullable = false)
    private Credito credito;

    @Column(name = "monto_cop", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoCop;

    @Column(name = "notas")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}