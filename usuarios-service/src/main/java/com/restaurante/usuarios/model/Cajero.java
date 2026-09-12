package com.restaurante.usuarios.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CAJERO")
public class Cajero extends Usuario {

    protected Cajero() { }

    public Cajero(String nombre, String correo, String passwordHash) {
        super(nombre, correo, passwordHash);
    }
}