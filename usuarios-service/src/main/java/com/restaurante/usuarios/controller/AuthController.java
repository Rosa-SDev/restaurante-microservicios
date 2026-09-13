package com.restaurante.usuarios.controller;

import com.restaurante.usuarios.dto.LoginDTO;
import com.restaurante.usuarios.dto.UsuarioDTO;
import com.restaurante.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService servicio;

    public AuthController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public UsuarioDTO login(@Valid @RequestBody LoginDTO dto) {
        return servicio.autenticar(dto.correo(), dto.password());
    }
}