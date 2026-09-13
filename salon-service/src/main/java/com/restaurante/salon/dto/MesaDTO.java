package com.restaurante.salon.dto;

import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.Mesa;

/**
 * Lo que sale en el JSON de las respuestas de mesa.
 */
public record MesaDTO(Long id,
                      Integer numero,
                      Integer capacidad,
                      EstadoMesa estado) {

    public static MesaDTO de(Mesa mesa) {
        if (mesa == null) {
            return null;
        }
        return new MesaDTO(mesa.getId(),
                           mesa.getNumero(),
                           mesa.getCapacidad(),
                           mesa.getEstado());
    }
}
