package com.sgi.auto.caja;

public enum TipoMovimientoCaja {
    VENTA,          // ingreso automático por venta POS
    ABONO_CREDITO,  // ingreso por pago de crédito
    GASTO,          // egreso operativo
    EGRESO_DUENO,   // retiro autorizado por el dueño
    APERTURA,       // saldo inicial de la sesión
    AJUSTE          // corrección manual
}