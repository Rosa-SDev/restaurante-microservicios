package com.restaurante.usuarios.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MESERO")
public class Mesero extends Usuario {

    protected Mesero() { }

    public Mesero(String nombre, String correo, String passwordHash) {
        super(nombre, correo, passwordHash);
    }
}