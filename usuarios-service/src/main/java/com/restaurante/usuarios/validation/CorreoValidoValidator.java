package com.restaurante.usuarios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Reimplementa como anotación reutilizable las reglas de ControllerUsuario.correoValido()
 * del sistema de escritorio, y agrega dos chequeos que ese metodo no cubría: caracteres no
 * permitidos y puntos consecutivos.
 */
public class CorreoValidoValidator implements ConstraintValidator<CorreoValido, String> {

    private static final String CARACTERES_LOCAL_PERMITIDOS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._%+-";
    private static final String CARACTERES_DOMINIO_PERMITIDOS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-";

    @Override
    public boolean isValid(String correo, ConstraintValidatorContext context) {
        // @NotBlank ya se encarga de null y vacío; este validador no opina sobre eso.
        if (correo == null || correo.isBlank()) {
            return true;
        }

        int arroba = correo.indexOf('@');
        if (arroba < 0 || arroba != correo.lastIndexOf('@')) {
            return false;
        }

        String parteLocal = correo.substring(0, arroba);
        String dominio = correo.substring(arroba + 1);

        if (parteLocal.isBlank() || dominio.isBlank()) {
            return false;
        }
        if (tieneCaracterNoPermitido(parteLocal, CARACTERES_LOCAL_PERMITIDOS)
                || tieneCaracterNoPermitido(dominio, CARACTERES_DOMINIO_PERMITIDOS)) {
            return false;
        }
        if (parteLocal.contains("..") || dominio.contains("..")
                || parteLocal.startsWith(".") || parteLocal.endsWith(".")
                || dominio.startsWith(".") || dominio.endsWith(".")
                || dominio.startsWith("-") || dominio.endsWith("-")) {
            return false;
        }

        // Igual que el metodo original: un punto en el dominio, con texto a los dos lados
        int punto = dominio.indexOf('.');
        return punto > 0 && punto < dominio.length() - 1;
    }

    private boolean tieneCaracterNoPermitido(String texto, String permitidos) {
        for (int i = 0; i < texto.length(); i++) {
            if (permitidos.indexOf(texto.charAt(i)) < 0) {
                return true;
            }
        }
        return false;
    }
}