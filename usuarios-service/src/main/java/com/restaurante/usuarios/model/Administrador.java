package com.restaurante.usuarios.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {

    protected Administrador() { }

    public Administrador(String nombre, String correo, String passwordHash) {
        super(nombre, correo, passwordHash);
    }
}