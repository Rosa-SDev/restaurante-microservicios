package com.restaurante.salon.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de negocio a códigos HTTP.
 *
 * Se escribe una sola vez por servicio y vale para todos los controladores.
 * Sin esta clase, las excepciones saldrían como 500 en vez de 404, 409 o 400.
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
     * La lanza @Valid cuando el cuerpo de la petición no cumple las
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

    /**
     * El cuerpo no se pudo leer: un JSON roto o un tipo incompatible.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> jsonInvalido(HttpMessageNotReadableException excepcion) {
        return Map.of("error", "El cuerpo de la petición no es un JSON válido.");
    }
}
