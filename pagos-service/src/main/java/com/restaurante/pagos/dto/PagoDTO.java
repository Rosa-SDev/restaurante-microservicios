package com.restaurante.pagos.dto;

import com.restaurante.pagos.model.EstadoPago;
import com.restaurante.pagos.model.MetodoPago;
import com.restaurante.pagos.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que sale en el JSON de las respuestas del servicio de pagos.
 */
public record PagoDTO(Long id,
                      Long facturaId,
                      BigDecimal monto,
                      MetodoPago metodo,
                      EstadoPago estado,
                      String referencia,
                      String registradoPor,
                      LocalDateTime fechaRegistro,
                      LocalDateTime fechaActualizacion) {

    public static PagoDTO de(Pago pago) {
        if (pago == null) {
            return null;
        }
        return new PagoDTO(pago.getId(),
                           pago.getFacturaId(),
                           pago.getMonto(),
                           pago.getMetodo(),
                           pago.getEstado(),
                           pago.getReferencia(),
                           pago.getRegistradoPor(),
                           pago.getFechaRegistro(),
                           pago.getFechaActualizacion());
    }
}
