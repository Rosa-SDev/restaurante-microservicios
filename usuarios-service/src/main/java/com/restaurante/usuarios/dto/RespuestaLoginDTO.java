package com.restaurante.usuarios.dto;

/**
 * Respuesta del login: envia un token y el usuario, no envia el hash de la contraseña (por eso UsuarioDTO)
 */
public record RespuestaLoginDTO(String token, String tipo, Long expiraEn, UsuarioDTO usuario) {
}
