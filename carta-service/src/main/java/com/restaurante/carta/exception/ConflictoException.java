package com.restaurante.carta.exception;

/**
 * El recurso existe, pero su estado no permite la operacion. Se traduce a 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
