package com.restaurante.carta.repository;

import com.restaurante.carta.model.Platillo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos de la carta.
 *
 * No se escribe implementacion: Spring la genera al arrancar. De JpaRepository
 * se heredan save, findById, findAll, deleteById y existsById.
 *
 * Los metodos de abajo se traducen a SQL a partir de su propio nombre, que debe
 * coincidir con los campos de Platillo.
 */
public interface PlatilloRepository extends JpaRepository<Platillo, Long> {

    List<Platillo> findByCategoria(String categoria);

    List<Platillo> findByDisponible(boolean disponible);

    List<Platillo> findByCategoriaAndDisponible(String categoria, boolean disponible);

    boolean existsByNombreIgnoreCase(String nombre);
}
