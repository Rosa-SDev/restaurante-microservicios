package com.restaurante.pagos.dto;

import com.restaurante.pagos.model.EstadoPago;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que entra al actualizar el estado de un pago (PUT /api/pagos/{id}/estado).
 */
public record CambiarEstadoPagoDTO(

        @NotNull(message = "El estado es obligatorio")
        EstadoPago estado) {
}

