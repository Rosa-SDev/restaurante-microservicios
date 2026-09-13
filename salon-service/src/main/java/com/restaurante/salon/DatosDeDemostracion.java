package com.restaurante.salon;

import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.Mesa;
import com.restaurante.salon.repository.MesaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga cinco mesas de demostración al arrancar el servicio.
 *
 * Permite probar de inmediato el flujo de pedidos y facturación sin
 * tener que crear mesas manualmente tras cada reinicio de H2 en memoria.
 */
@Component
public class DatosDeDemostracion implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosDeDemostracion.class);

    private final MesaRepository mesaRepository;

    public DatosDeDemostracion(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    @Override
    public void run(String... args) {
        if (mesaRepository.count() > 0) {
            return;
        }

        mesaRepository.saveAll(List.of(
                new Mesa(null, 1, 2, EstadoMesa.LIBRE),
                new Mesa(null, 2, 4, EstadoMesa.LIBRE),
                new Mesa(null, 3, 4, EstadoMesa.LIBRE),
                new Mesa(null, 4, 6, EstadoMesa.LIBRE),
                new Mesa(null, 5, 8, EstadoMesa.LIBRE)
        ));

        LOG.info("Cargadas {} mesas de demostración.", mesaRepository.count());
    }
}
