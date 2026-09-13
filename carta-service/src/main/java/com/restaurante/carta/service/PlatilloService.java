package com.restaurante.carta.service;

import com.restaurante.carta.exception.ConflictoException;
import com.restaurante.carta.exception.NoEncontradoException;
import com.restaurante.carta.model.Platillo;
import com.restaurante.carta.repository.PlatilloRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reglas de negocio de la carta.
 *
 * Esta clase no sabe que existe HTTP: no recibe peticiones ni devuelve codigos
 * de estado. Cuando algo no se puede hacer, lanza una excepcion propia y el
 * ManejadorDeErrores la traduce.
 *
 * Reparto de validaciones: el DTO de entrada valida la FORMA de la peticion
 * (nombre vacio, precio menor o igual que cero). Aqui se valida el ESTADO del
 * sistema, que es lo unico que necesita consultar la base de datos: si el
 * platillo existe y si el nombre esta repetido.
 */
@Service
public class PlatilloService {

    private final PlatilloRepository repositorio;

    /** Spring ve este constructor y pasa el repositorio automaticamente. */
    public PlatilloService(PlatilloRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Platillo crear(Platillo platillo) {
        if (repositorio.existsByNombreIgnoreCase(platillo.getNombre())) {
            throw new ConflictoException("Ya existe un platillo con el nombre " + platillo.getNombre() + ".");
        }
        return repositorio.save(platillo);
    }

    /**
     * Los dos filtros son opcionales e independientes, asi que hay cuatro casos.
     * Un filtro nulo significa "no filtres por este campo".
     */
    public List<Platillo> listar(String categoria, Boolean disponible) {
        if (categoria != null && disponible != null) {
            return repositorio.findByCategoriaAndDisponible(categoria, disponible);
        }
        if (categoria != null) {
            return repositorio.findByCategoria(categoria);
        }
        if (disponible != null) {
            return repositorio.findByDisponible(disponible);
        }
        return repositorio.findAll();
    }

    public Platillo buscarPorId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("El platillo " + id + " no existe."));
    }

    /**
     * Cambia los datos del platillo que ya esta guardado.
     *
     * El nombre repetido solo se comprueba si de verdad cambio: si no, el
     * platillo chocaria consigo mismo.
     */
    public Platillo actualizar(Long id, Platillo datos) {
        Platillo platillo = buscarPorId(id);

        boolean cambiaElNombre = !platillo.getNombre().equalsIgnoreCase(datos.getNombre());
        if (cambiaElNombre && repositorio.existsByNombreIgnoreCase(datos.getNombre())) {
            throw new ConflictoException("Ya existe un platillo con el nombre " + datos.getNombre() + ".");
        }

        platillo.setNombre(datos.getNombre());
        platillo.setDescripcion(datos.getDescripcion());
        platillo.setCategoria(datos.getCategoria());
        platillo.actualizarPrecio(datos.getPrecio());
        platillo.cambiarDisponibilidad(datos.isDisponible());

        return repositorio.save(platillo);
    }

    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new NoEncontradoException("El platillo " + id + " no existe.");
        }
        repositorio.deleteById(id);
    }

    /**
     * Rebaja el precio en el porcentaje indicado.
     *
     * El calculo vive en Platillo.aplicarDescuento: es una regla del propio
     * platillo, no del servicio. Que el porcentaje este entre 0 y 100 lo valida
     * el DTO de entrada antes de llegar hasta aqui.
     */
    public Platillo aplicarDescuento(Long id, BigDecimal porcentaje) {
        Platillo platillo = buscarPorId(id);
        platillo.aplicarDescuento(porcentaje);
        return repositorio.save(platillo);
    }
}
