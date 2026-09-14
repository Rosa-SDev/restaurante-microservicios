package com.restaurante.clientes.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearReservaDTO(
    @NotNull(message = "La fecha y hora son obligatorias")
    LocalDateTime fechaHora,

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "Debe ser al menos 1 persona")
    Integer numPersonas,

    @NotNull(message = "El cliente es obligatorio")
    Long clienteId,

    Long mesaId,
    Long meseroId
) { }