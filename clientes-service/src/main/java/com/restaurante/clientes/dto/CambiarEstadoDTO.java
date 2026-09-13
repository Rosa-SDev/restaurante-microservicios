package com.restaurante.clientes.dto;

import jakarta.validation.constraints.NotNull;

import com.restaurante.clientes.model.EstadoReserva;

public record CambiarEstadoDTO(
    @NotNull(message = "El nuevo estado es obligatorio")
    EstadoReserva nuevoEstado
) { }