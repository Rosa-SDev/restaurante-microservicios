package com.restaurante.usuarios.service;

import com.restaurante.usuarios.dto.CrearUsuarioDTO;
import com.restaurante.usuarios.dto.UsuarioDTO;
import com.restaurante.usuarios.exception.ConflictoException;
import com.restaurante.usuarios.exception.CredencialesInvalidasException;
import com.restaurante.usuarios.exception.NoEncontradoException;
import com.restaurante.usuarios.model.*;
import com.restaurante.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioDTO crear(CrearUsuarioDTO dto) {
        if (repositorio.existsByCorreoIgnoreCase(dto.correo())) {
            throw new ConflictoException("Ya existe un usuario con ese correo.");
        }
        Usuario usuario = construirPorRol(dto.rol(), dto.nombre(), dto.correo(), passwordEncoder.encode(dto.password()));
        return UsuarioDTO.de(repositorio.save(usuario));
    }

    public List<UsuarioDTO> listar(Rol rol) {
        return repositorio.findAll().stream()
                .filter(usuario -> rol == null || Rol.de(usuario) == rol)
                .map(UsuarioDTO::de)
                .toList();
    }

    public UsuarioDTO buscarPorId(Long id) {
        return UsuarioDTO.de(buscarEntidad(id));
    }

    /**
     * Actualiza nombre, correo y contraseña. El rol no se puede cambiar por
     * esta vía: en SINGLE_TABLE cambiar de subclase implicaría borrar y
     * recrear la fila, y el contrato no pide esa operación.
     */
    public UsuarioDTO actualizar(Long id, CrearUsuarioDTO dto) {
        Usuario usuario = buscarEntidad(id);

        repositorio.findByCorreoIgnoreCase(dto.correo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictoException("Ya existe otro usuario con ese correo.");
                });

        usuario.setNombre(dto.nombre());
        usuario.setCorreo(dto.correo());
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        return UsuarioDTO.de(repositorio.save(usuario));
    }

    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new NoEncontradoException("El usuario " + id + " no existe.");
        }
        repositorio.deleteById(id);
    }

    /**
     * Mismo mensaje para correo inexistente, clave incorrecta y usuario
     * inactivo: distinguirlos revelaría qué correos están registrados.
     */
    public UsuarioDTO autenticar(String correo, String password) {
        Usuario usuario = repositorio.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));

        if (!usuario.isActivo() || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }
        return UsuarioDTO.de(usuario);
    }

    private Usuario buscarEntidad(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("El usuario " + id + " no existe."));
    }

    private Usuario construirPorRol(Rol rol, String nombre, String correo, String passwordHash) {
        return switch (rol) {
            case ADMINISTRADOR -> new Administrador(nombre, correo, passwordHash);
            case MESERO -> new Mesero(nombre, correo, passwordHash);
            case COCINERO -> new Cocinero(nombre, correo, passwordHash);
            case CAJERO -> new Cajero(nombre, correo, passwordHash);
        };
    }
}
