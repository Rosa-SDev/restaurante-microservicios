package com.restaurante.usuarios.model;

public enum Rol {
    ADMINISTRADOR,
    MESERO,
    COCINERO,
    CAJERO;

    public static Rol de(Usuario usuario) {
        if (usuario instanceof Administrador) return Rol.ADMINISTRADOR;
        if (usuario instanceof Mesero) return Rol.MESERO;
        if (usuario instanceof Cocinero) return Rol.COCINERO;
        if (usuario instanceof Cajero) return Rol.CAJERO;
        throw new IllegalStateException("Rol de usuario desconocido.");
    }
}