package com.sgi.auto.backup.dto;

import java.time.OffsetDateTime;

public record RespaldoRespuestaDTO(
        Long id,
        String nombreArchivo,
        String rutaAlmacenamiento,
        Long tamanoBytes,
        boolean exitoso,
        String mensajeError,
        OffsetDateTime creadoEn
) {}