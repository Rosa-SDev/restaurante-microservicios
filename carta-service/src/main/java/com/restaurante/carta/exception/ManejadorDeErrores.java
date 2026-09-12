package com.restaurante.carta.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de negocio a codigos HTTP.
 *
 * Se escribe una sola vez por servicio y vale para todos los controladores.
 * Sin esta clase, las excepciones saldrian como 500 en vez de 404, 409 o 400.
 *
 * A proposito no hay un manejador de Exception: una excepcion inesperada debe
 * dejar su traza completa en la consola.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(NoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> noEncontrado(NoEncontradoException excepcion) {
        return Map.of("error", excepcion.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> conflicto(ConflictoException excepcion) {
        return Map.of("error", excepcion.getMessage());
    }

    /**
     * La lanza @Valid cuando el cuerpo de la peticion no cumple las
     * validaciones del DTO. Se devuelven todos los campos que fallaron.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> datosInvalidos(MethodArgumentNotValidException excepcion) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }
        return Map.of("error", "La petición tiene campos inválidos",
                      "campos", campos);
    }
}
