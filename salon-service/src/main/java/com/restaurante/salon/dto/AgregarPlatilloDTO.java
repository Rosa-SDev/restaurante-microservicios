package com.restaurante.salon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Lo que entra al agregar un platillo al pedido.
 * Desnormalización deliberada: se reciben platilloId, nombre y precioUnitario.
 * Todos los tipos son Wrappers / Objetos.
 */
public record AgregarPlatilloDTO(

        @NotNull(message = "El ID del platillo es obligatorio")
        Long platilloId,

        @NotBlank(message = "El nombre del platillo es obligatorio")
        String nombre,

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor que cero")
        BigDecimal precioUnitario) {
}
