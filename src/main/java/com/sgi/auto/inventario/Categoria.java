package com.sgi.auto.inventario;

import com.sgi.auto.compartido.EntidadBase;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Categoria extends EntidadBase {

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "esta_activa", nullable = false)
    @Builder.Default
    private boolean estaActiva = true;
}
