package com.restaurante.carta.dto;

import com.restaurante.carta.model.Platillo;

import java.math.BigDecimal;

/**
 * Lo que sale en el JSON de las respuestas.
 *
 * La entidad nunca se devuelve tal cual: asi el JSON no cambia aunque manana
 * cambie la tabla, y no se filtra ningun campo interno.
 */
public record PlatilloDTO(Long id,
                          String nombre,
                          String descripcion,
                          String categoria,
                          BigDecimal precio,
                          boolean disponible) {

    /** Convierte la entidad en el objeto que viaja por la red. */
    public static PlatilloDTO de(Platillo platillo) {
        return new PlatilloDTO(platillo.getId(),
                               platillo.getNombre(),
                               platillo.getDescripcion(),
                               platillo.getCategoria(),
                               platillo.getPrecio(),
                               platillo.isDisponible());
    }
}
