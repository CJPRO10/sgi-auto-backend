package com.sgi.auto.inventario;

import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entradas_mercancia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntradaMercancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "numero_factura_proveedor", length = 60)
    private String numeroFacturaProveedor;

    @Column(name = "costo_total_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal costoTotalCop = BigDecimal.ZERO;

    @Column(name = "notas")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @OneToMany(mappedBy = "entrada", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemEntrada> items = new ArrayList<>();
}