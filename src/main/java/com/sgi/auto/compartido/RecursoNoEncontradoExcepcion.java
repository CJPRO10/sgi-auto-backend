package com.sgi.auto.compartido;

public class RecursoNoEncontradoExcepcion extends RuntimeException{
    public RecursoNoEncontradoExcepcion(String mensaje) {
        super(mensaje);
    }
}
