package com.restaurante.usuarios.config;

import com.restaurante.usuarios.dto.CrearUsuarioDTO;
import com.restaurante.usuarios.model.Rol;
import com.restaurante.usuarios.repository.UsuarioRepository;
import com.restaurante.usuarios.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inserta un usuario de cada rol al arrancar, solo si la tabla está vacía.
 * Con H2 en memoria esto se cumple en cada reinicio; el chequeo importa de
 * verdad cuando en el Avance 2 la base de datos sea persistente en MySQL.
 */
@Component
public class CargaUsuariosDemostracion implements CommandLineRunner {

    private final UsuarioRepository repositorio;
    private final UsuarioService servicio;

    public CargaUsuariosDemostracion(UsuarioRepository repositorio, UsuarioService servicio) {
        this.repositorio = repositorio;
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }

        servicio.crear(new CrearUsuarioDTO(
                "Administrador Demo", "administrador@restaurante.com", Rol.ADMINISTRADOR, "admin123"));
        servicio.crear(new CrearUsuarioDTO(
                "Mesero Demo", "mesero@restaurante.com", Rol.MESERO, "mesero123"));
        servicio.crear(new CrearUsuarioDTO(
                "Cocinero Demo", "cocinero@restaurante.com", Rol.COCINERO, "cocinero123"));
        servicio.crear(new CrearUsuarioDTO(
                "Cajero Demo", "cajero@restaurante.com", Rol.CAJERO, "cajero123"));
    }
}
