package com.sgi.auto.compartido;

public class ConflictoExcepcion extends RuntimeException{
    public ConflictoExcepcion(String mensaje) {
        super(mensaje);
    }
}
