package com.sgi.auto.caja;

/**
 * Evento publicado cuando se cierra una sesión de caja. Se usa para
 * disparar acciones secundarias (como el backup automático) de forma
 * desacoplada y solo DESPUÉS de que la transacción de cierre haya
 * confirmado por completo en la base de datos.
 */
public record CajaCerradaEvento(Long sesionId) {}