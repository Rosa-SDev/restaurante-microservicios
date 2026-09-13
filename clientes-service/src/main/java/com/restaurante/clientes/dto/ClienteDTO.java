package com.restaurante.clientes.dto;

import com.restaurante.clientes.model.Cliente;

public record ClienteDTO(Long id, String nombre, String documento, String telefono) {

    public static ClienteDTO de(Cliente c) {
        return new ClienteDTO(c.getId(), c.getNombre(), c.getDocumento(), c.getTelefono());
    }
}