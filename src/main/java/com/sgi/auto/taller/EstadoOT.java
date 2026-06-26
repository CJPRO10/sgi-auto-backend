package com.sgi.auto.taller;

public enum EstadoOT {
    RECIBIDO,       // vehículo ingresó al taller
    EN_DIAGNOSTICO, // mecánico evaluando
    EN_REPARACION,  // trabajo en curso
    ESPERANDO_REPUESTO, // pausada por falta de repuesto
    LISTO,          // trabajo terminado, esperando entrega
    ENTREGADO,      // vehículo entregado al cliente
    CANCELADO       // OT cancelada
}