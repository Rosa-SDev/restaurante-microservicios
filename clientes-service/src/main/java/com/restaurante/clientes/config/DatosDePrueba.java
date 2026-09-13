package com.restaurante.clientes.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.restaurante.clientes.model.Cliente;
import com.restaurante.clientes.model.Reserva;
import com.restaurante.clientes.repository.ClienteRepository;
import com.restaurante.clientes.repository.ReservaRepository;

@Component
public class DatosDePrueba implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public DatosDePrueba(ClienteRepository clienteRepository, ReservaRepository reservaRepository) {
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    public void run(String... args) {
        if (clienteRepository.count() > 0) {
            return;
        }

        Cliente ana = clienteRepository.save(new Cliente("Ana Torres", "1001234567", "3001234567"));
        Cliente luis = clienteRepository.save(new Cliente("Luis Ramírez", "1007654321", "3007654321"));
        clienteRepository.save(new Cliente("Marcela Gómez", "1009988776", "3009988776"));

        reservaRepository.save(new Reserva(
                LocalDateTime.now().plusDays(1).withHour(19).withMinute(0),
                4, ana, 1L, 1L));

        reservaRepository.save(new Reserva(
                LocalDateTime.now().plusDays(2).withHour(20).withMinute(30),
                2, luis, 2L, 1L));
    }
}