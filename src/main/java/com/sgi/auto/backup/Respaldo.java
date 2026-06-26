package com.sgi.auto.backup;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Registro de cada backup generado.
 * RF-091 al RF-095
 */
@Entity
@Table(name = "respaldos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Respaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "ruta_almacenamiento", nullable = false)
    private String rutaAlmacenamiento;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "exitoso", nullable = false)
    @Builder.Default
    private boolean exitoso = true;

    @Column(name = "mensaje_error")
    private String mensajeError;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}