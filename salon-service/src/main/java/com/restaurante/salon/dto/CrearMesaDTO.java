package com.restaurante.salon.dto;

import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.Mesa;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Lo que entra en el cuerpo al crear o actualizar una mesa.
 * Utiliza wrappers para evitar valores por defecto primitivos silenciosos.
 */
public record CrearMesaDTO(

        @NotNull(message = "El número de la mesa es obligatorio")
        @Min(value = 1, message = "El número de la mesa debe ser mayor que cero")
        Integer numero,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad de la mesa debe ser mayor que cero")
        Integer capacidad,

        EstadoMesa estado) {

    public Mesa aEntidad() {
        return new Mesa(null, numero, capacidad, estado != null ? estado : EstadoMesa.LIBRE);
    }
}
