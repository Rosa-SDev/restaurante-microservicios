package com.restaurante.carta;

import com.restaurante.carta.model.Platillo;
import com.restaurante.carta.repository.PlatilloRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga unos platillos de ejemplo al arrancar el servicio.
 *
 * CommandLineRunner es una interfaz de un solo metodo que Spring ejecuta una
 * vez, justo despues de terminar de arrancar el contexto.
 *
 * La base de datos esta en memoria, asi que sin esto cada arranque empezaria
 * con la carta vacia y habria que crear los platillos a mano antes de probar.
 *
 * Los precios se construyen con new BigDecimal("38000.00"), desde texto: un
 * double no representa exactamente los decimales y arrastraria error.
 */
@Component
public class DatosDeDemostracion implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosDeDemostracion.class);

    private final PlatilloRepository repositorio;

    public DatosDeDemostracion(PlatilloRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Solo carga si la tabla esta vacia.
     *
     * Hoy lo esta siempre, porque H2 vive en memoria. La guarda es para el
     * Avance 2: con MySQL, sin ella cada reinicio duplicaria los cinco.
     */
    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }

        repositorio.saveAll(List.of(
                new Platillo("Bandeja paisa",
                             "Frijoles, chicharrón, chorizo, arepa y huevo",
                             "Fuertes", new BigDecimal("38000.00"), true),
                new Platillo("Ajiaco santafereño",
                             "Sopa de tres papas con pollo, guascas y alcaparras",
                             "Sopas", new BigDecimal("28000.00"), true),
                new Platillo("Arepa de choclo",
                             "Arepa de maíz tierno con queso costeño",
                             "Entradas", new BigDecimal("9000.00"), true),
                new Platillo("Postre de natas",
                             "Natas de leche con brevas en almíbar",
                             "Postres", new BigDecimal("12000.00"), true),
                new Platillo("Limonada de coco",
                             "Limón exprimido con leche de coco, servido frío",
                             "Bebidas", new BigDecimal("11000.00"), false)));

        LOG.info("Cargados {} platillos de demostración.", repositorio.count());
    }
}
