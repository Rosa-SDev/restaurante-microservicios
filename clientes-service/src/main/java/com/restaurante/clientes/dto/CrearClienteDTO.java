package com.restaurante.clientes.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearClienteDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El documento es obligatorio")
    String documento,

    String telefono
) { }