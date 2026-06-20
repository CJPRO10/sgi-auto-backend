package com.sgi.auto.usuarios.dto;

import com.sgi.auto.usuarios.RolUsuario;

import java.time.OffsetDateTime;

public record UsuarioRespuestaDTO(
        Long id,
        String nombreCompleto,
        String nombreUsuario,
        String correo,
        RolUsuario rol,
        boolean puedeAplicarDescuento,
        boolean puedeAnularVenta,
        boolean puedeCerrarCaja,
        boolean puedeVerReportes,
        boolean puedeGestionarCredito,
        boolean estaActivo,
        OffsetDateTime ultimoIngresoEn
) {}