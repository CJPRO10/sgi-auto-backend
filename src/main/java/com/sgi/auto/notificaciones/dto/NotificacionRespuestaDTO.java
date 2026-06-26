package com.sgi.auto.notificaciones.dto;

import com.sgi.auto.notificaciones.TipoNotificacion;
import java.time.OffsetDateTime;

public record NotificacionRespuestaDTO(
        Long id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        boolean leida,
        String entidadTipo,
        Long entidadId,
        OffsetDateTime creadoEn
) {}