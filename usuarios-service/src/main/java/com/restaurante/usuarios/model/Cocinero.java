package com.restaurante.usuarios.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("COCINERO")
public class Cocinero extends Usuario {

    protected Cocinero() { }

    public Cocinero(String nombre, String correo, String passwordHash) {
        super(nombre, correo, passwordHash);
    }
}