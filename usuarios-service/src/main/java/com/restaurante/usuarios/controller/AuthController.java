package com.restaurante.usuarios.controller;

import com.restaurante.usuarios.dto.LoginDTO;
import com.restaurante.usuarios.dto.RespuestaLoginDTO;
import com.restaurante.usuarios.dto.UsuarioDTO;
import com.restaurante.usuarios.service.EmisorDeTokens;
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
    private final EmisorDeTokens emisor;

    public AuthController(UsuarioService servicio, EmisorDeTokens emisor) {
        this.servicio = servicio;
        this.emisor = emisor;
    }

    @PostMapping("/login")
    public RespuestaLoginDTO login(@Valid @RequestBody LoginDTO dto) {
        UsuarioDTO usuario = servicio.autenticar(dto.correo(), dto.password());
        String token = emisor.emitir(usuario);
        return new RespuestaLoginDTO(token, "Bearer", emisor.getExpiracionSegundos(), usuario);
    }
}