package com.restaurante.carta.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * El cuerpo de PUT /api/platillos/{id}/descuento.
 *
 * Que el porcentaje este entre 0 y 100 se valida aqui, no en el servicio: es la
 * forma de la peticion, y asi el error sale como 400 por el mismo camino que
 * los demas.
 */
public record DescuentoDTO(

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El porcentaje debe ser mayor que cero")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede ser mayor que cien")
        BigDecimal porcentaje) {
}
