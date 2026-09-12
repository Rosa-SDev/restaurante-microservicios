package com.restaurante.carta.dto;

import com.restaurante.carta.model.Platillo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Lo que entra en el cuerpo de un POST o de un PUT.
 *
 * No tiene id a proposito: lo genera la base de datos, no el cliente.
 *
 * Las anotaciones se ejecutan cuando el controlador marca el parametro con
 * @Valid. Si alguna falla, la peticion nunca llega al servicio y el manejador
 * de errores responde 400.
 */
public record CrearPlatilloDTO(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotNull(message = "La disponibilidad es obligatoria")
        Boolean disponible) {

    /** Construye la entidad a partir de lo que mando el cliente. */
    public Platillo aEntidad() {
        return new Platillo(nombre, descripcion, categoria, precio, disponible);
    }
}
