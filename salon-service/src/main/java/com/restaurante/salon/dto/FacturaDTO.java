package com.restaurante.salon.dto;

import com.restaurante.salon.model.Factura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que sale en el JSON de las respuestas de facturación.
 */
public record FacturaDTO(Long id,
                         String numero,
                         LocalDateTime fecha,
                         BigDecimal subtotal,
                         BigDecimal impuestos,
                         BigDecimal total,
                         boolean anulada,
                         Long pedidoId,
                         Long cajeroId) {

    public static FacturaDTO de(Factura factura) {
        if (factura == null) {
            return null;
        }
        Long pedidoId = factura.getPedido() != null ? factura.getPedido().getId() : null;
        return new FacturaDTO(factura.getId(),
                              factura.getNumero(),
                              factura.getFecha(),
                              factura.getSubtotal(),
                              factura.getImpuestos(),
                              factura.getTotal(),
                              factura.isAnulada(),
                              pedidoId,
                              factura.getCajeroId());
    }
}
