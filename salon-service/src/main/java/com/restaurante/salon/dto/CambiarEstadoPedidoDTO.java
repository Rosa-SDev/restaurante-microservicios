package com.restaurante.salon.dto;

import com.restaurante.salon.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que entra al actualizar el estado de un pedido (PUT /api/pedidos/{id}/estado).
 */
public record CambiarEstadoPedidoDTO(

        @NotNull(message = "El estado del pedido es obligatorio")
        EstadoPedido estado) {
}
