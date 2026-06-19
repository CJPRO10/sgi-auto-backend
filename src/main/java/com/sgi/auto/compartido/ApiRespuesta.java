package com.sgi.auto.compartido;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiRespuesta<T> {

    private final boolean exito;
    private final T datos;
    private final String mensaje;
    private final OffsetDateTime timestamp = OffsetDateTime.now();

    private ApiRespuesta(boolean exito, T datos, String mensaje) {
        this.exito   = exito;
        this.datos   = datos;
        this.mensaje = mensaje;
    }

    public static <T> ApiRespuesta<T> exitoso(T datos) {
        return new ApiRespuesta<>(true, datos, null);
    }

    public static <T> ApiRespuesta<T> exitoso(T datos, String mensaje) {
        return new ApiRespuesta<>(true, datos, mensaje);
    }

    public static <T> ApiRespuesta<T> error(String mensaje) {
        return new ApiRespuesta<>(false, null, mensaje);
    }
}