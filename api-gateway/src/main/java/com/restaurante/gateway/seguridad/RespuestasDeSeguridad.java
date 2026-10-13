package com.restaurante.gateway.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Cuerpos de error del gateway cuando la petición no pasa el control de acceso.
 *
 * Una sola clase atiende los dos casos para que el mensaje sea siempre el mismo,
 * sin importar por qué filtro se rechazó la petición: falta de token, token
 * inválido o vencido (401) y token correcto sin permiso suficiente (403).
 *
 * El tipo de contenido lleva charset=UTF-8 a propósito, para que las tildes del
 * mensaje lleguen bien al cliente.
 */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String SIN_TOKEN_VALIDO = "Debe iniciar sesión con un token válido.";

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta operación.";

    /**
     * No hay token, o el que llegó no se pudo validar.
     */
    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        responder(respuesta, HttpStatus.UNAUTHORIZED, SIN_TOKEN_VALIDO);
    }

    /**
     * El token es válido, pero el rol no alcanza para esta operación.
     */
    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        responder(respuesta, HttpStatus.FORBIDDEN, SIN_PERMISOS);
    }

    private void responder(HttpServletResponse respuesta, HttpStatus estado, String mensaje) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"error\": \"" + mensaje + "\"}");
    }
}
