package com.sgi.auto.clientes;

import com.sgi.auto.compartido.EntidadBase;
import com.sgi.auto.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends EntidadBase {

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "tipo_identificacion", nullable = false, length = 2)
    @Builder.Default
    private String tipoIdentificacion = "CC";

    @Column(name = "numero_identificacion", nullable = false, length = 20)
    private String numeroIdentificacion;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "credito_habilitado", nullable = false)
    @Builder.Default
    private boolean creditoHabilitado = false;

    @Column(name = "cupo_credito_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal cupoCreditoCop = BigDecimal.ZERO;

    @Column(name = "saldo_credito_cop", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal saldoCreditoCop = BigDecimal.ZERO;

    @Column(name = "saldo_puntos", nullable = false)
    @Builder.Default
    private int saldoPuntos = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;
}