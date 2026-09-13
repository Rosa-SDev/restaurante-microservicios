package com.restaurante.usuarios.dto;

import com.restaurante.usuarios.model.Rol;
import com.restaurante.usuarios.model.Usuario;

/** Lo que sale al cliente. Nunca incluye passwordHash. */
public record UsuarioDTO(Long id, String nombre, String correo, Rol rol, boolean activo) {

    public static UsuarioDTO de(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                Rol.de(usuario),
                usuario.isActivo()
        );
    }
}