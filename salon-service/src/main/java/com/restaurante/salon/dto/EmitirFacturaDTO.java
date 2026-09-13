package com.restaurante.salon.dto;

import com.restaurante.salon.model.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Lo que entra en el cuerpo al emitir una factura (POST /api/facturas).
 * Todos los atributos son objetos Wrapper.
 */
public record EmitirFacturaDTO(

        @NotNull(message = "El ID del pedido es obligatorio")
        Long pedidoId,

        @NotNull(message = "El ID del cajero es obligatorio")
        Long cajeroId,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @NotNull(message = "El monto pagado es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto pagado debe ser mayor que cero")
        BigDecimal monto) {
}
