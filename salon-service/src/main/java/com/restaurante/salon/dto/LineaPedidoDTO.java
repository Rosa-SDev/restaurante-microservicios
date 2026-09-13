package com.restaurante.salon.dto;

import com.restaurante.salon.model.LineaPedido;

import java.math.BigDecimal;

/**
 * Lo que sale en el JSON para cada platillo dentro de un pedido.
 */
public record LineaPedidoDTO(Long id,
                             Long platilloId,
                             String nombre,
                             BigDecimal precioUnitario) {

    public static LineaPedidoDTO de(LineaPedido linea) {
        if (linea == null) {
            return null;
        }
        return new LineaPedidoDTO(linea.getId(),
                                  linea.getPlatilloId(),
                                  linea.getNombre(),
                                  linea.getPrecioUnitario());
    }
}
