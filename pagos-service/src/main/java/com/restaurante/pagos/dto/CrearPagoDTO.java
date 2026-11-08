package com.restaurante.pagos.dto;

import com.restaurante.pagos.model.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Lo que entra en el cuerpo al registrar un pago (POST /api/pagos).
 * Todos los atributos son objetos (sin tipos primitivos).
 */
public record CrearPagoDTO(

        @NotNull(message = "El ID de la factura es obligatorio")
        Long facturaId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodo) {
}

