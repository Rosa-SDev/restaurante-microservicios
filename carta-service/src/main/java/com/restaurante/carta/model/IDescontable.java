package com.restaurante.carta.model;

import java.math.BigDecimal;

/**
 * Lo que se le puede aplicar un descuento porcentual.
 *
 * Viene del sistema de escritorio y se conserva tal cual: solo cambia el paquete.
 */
public interface IDescontable {

    BigDecimal aplicarDescuento(BigDecimal porcentaje);
}
