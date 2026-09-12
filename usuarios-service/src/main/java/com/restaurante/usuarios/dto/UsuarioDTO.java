package com.restaurante.usuarios.dto;

import com.restaurante.usuarios.model.Administrador;
import com.restaurante.usuarios.model.Cajero;
import com.restaurante.usuarios.model.Cocinero;
import com.restaurante.usuarios.model.Mesero;
import com.restaurante.usuarios.model.Rol;
import com.restaurante.usuarios.model.Usuario;

/** Lo que sale al cliente. Nunca incluye passwordHash. */
public record UsuarioDTO(Long id, String nombre, String correo, Rol rol, boolean activo) {

    public static UsuarioDTO de(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                rolDe(usuario),
                usuario.isActivo()
        );
    }

    private static Rol rolDe(Usuario usuario) {
        if (usuario instanceof Administrador) return Rol.ADMINISTRADOR;
        if (usuario instanceof Mesero) return Rol.MESERO;
        if (usuario instanceof Cocinero) return Rol.COCINERO;
        if (usuario instanceof Cajero) return Rol.CAJERO;
        throw new IllegalStateException("Rol de usuario desconocido.");
    }
}