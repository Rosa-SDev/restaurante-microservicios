package com.restaurante.usuarios.model;

import jakarta.persistence.*;

/**
 * Persona que usa el sistema. Es abstracta porque no existe un usuario generico:
 * todo usuario es Administrador, Mesero, Cocinero o Cajero.
 * Los cuatro roles no agregan atributos propios; solo se diferencian por el
 * valor de la columna discriminadora "rol".
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rol", discriminatorType = DiscriminatorType.STRING)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo;

    protected Usuario() { }

    protected Usuario(String nombre, String correo, String passwordHash) {
        this.nombre = nombre;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
