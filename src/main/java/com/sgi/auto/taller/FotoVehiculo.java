package com.sgi.auto.taller;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Foto del vehículo al ingreso.
 * RF-056 — Fotografía de ingreso
 * RF-057 — Almacenamiento en Cloudinary
 */
@Entity
@Table(name = "fotos_vehiculo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FotoVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenDeTrabajo ordenDeTrabajo;

    @Column(name = "url_foto", nullable = false)
    private String urlFoto;

    // Identificador en Cloudinary para poder eliminar la foto (RF-057)
    @Column(name = "public_id", nullable = false, length = 200)
    private String publicId;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "subida_en", nullable = false)
    @Builder.Default
    private OffsetDateTime subidaEn = OffsetDateTime.now();
}
