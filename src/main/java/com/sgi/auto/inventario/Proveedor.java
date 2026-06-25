package com.sgi.auto.inventario;

import com.sgi.auto.compartido.EntidadBase;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor extends EntidadBase {

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "nit", length = 20)
    private String nit;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "persona_contacto", length = 150)
    private String personaContacto;

    @Column(name = "notas")
    private String notas;

    @Column(name = "esta_activo", nullable = false)
    @Builder.Default
    private boolean estaActivo = true;
}