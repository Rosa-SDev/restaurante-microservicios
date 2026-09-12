package com.restaurante.carta.exception;

/**
 * El recurso que se pidio no existe. El manejador la traduce a 404.
 */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
