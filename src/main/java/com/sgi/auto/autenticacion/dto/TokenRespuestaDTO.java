// TokenRespuestaDTO.java
package com.sgi.auto.autenticacion.dto;

import java.util.Map;

public record TokenRespuestaDTO(
        String token,
        String nombreCompleto,
        String rol,
        Map<String, Object> permisos
) {}