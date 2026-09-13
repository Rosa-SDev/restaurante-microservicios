package com.restaurante.clientes.dto;

public class ClienteDTO {

    private Long id;
    private String nombre;
    private String documento;
    private String telefono;

    public ClienteDTO(Long id, String nombre, String documento, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }
}