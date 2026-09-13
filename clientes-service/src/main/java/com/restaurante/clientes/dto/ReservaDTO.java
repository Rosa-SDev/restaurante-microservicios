package com.restaurante.clientes.dto;

import java.time.LocalDateTime;

import com.restaurante.clientes.model.EstadoReserva;
import com.restaurante.clientes.model.Reserva;

public record ReservaDTO(Long id, LocalDateTime fechaHora, Integer numPersonas,
                          EstadoReserva estado, Long clienteId, String clienteNombre,
                          Long mesaId, Long meseroId) {

    public static ReservaDTO de(Reserva r) {
        return new ReservaDTO(r.getId(), r.getFechaHora(), r.getNumPersonas(), r.getEstado(),
                r.getCliente().getId(), r.getCliente().getNombre(), r.getMesaId(), r.getMeseroId());
    }
}