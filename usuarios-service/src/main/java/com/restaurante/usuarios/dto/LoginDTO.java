package com.restaurante.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** Lo que llega al iniciar sesión. */
public record LoginDTO(

        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password

) { }