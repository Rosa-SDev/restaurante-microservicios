package com.restaurante.clientes.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurante.clientes.dto.CrearReservaDTO;
import com.restaurante.clientes.dto.ReservaDTO;
import com.restaurante.clientes.exception.NoEncontradoException;
import com.restaurante.clientes.model.Cliente;
import com.restaurante.clientes.model.EstadoReserva;
import com.restaurante.clientes.model.Reserva;
import com.restaurante.clientes.repository.ClienteRepository;
import com.restaurante.clientes.repository.ReservaRepository;
import com.restaurante.clientes.exception.ConflictoException;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;

    public ReservaService(ReservaRepository reservaRepository, ClienteRepository clienteRepository) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<ReservaDTO> listar(EstadoReserva estado) {
        List<Reserva> reservas = (estado == null)
                ? reservaRepository.findAll()
                : reservaRepository.findByEstado(estado);
        return reservas.stream().map(ReservaDTO::de).toList();
    }

    public ReservaDTO buscarPorId(Long id) {
        return ReservaDTO.de(buscarEntidad(id));
    }

    public ReservaDTO crear(CrearReservaDTO datos) {
        Cliente cliente = clienteRepository.findById(datos.clienteId())
                .orElseThrow(() -> new NoEncontradoException(
                    "No existe un cliente con id " + datos.clienteId() + "."));

        // Se compara por minutos: el formulario original solo maneja "dd/MM/yyyy HH:mm",
        // así que comparar por segundos rechazaría reservas válidas por medio minuto.
        if (datos.fechaHora().truncatedTo(ChronoUnit.MINUTES)
                .isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new IllegalArgumentException("No se puede reservar para una fecha que ya pasó.");
        }

        // TODO Avance 2: validar que numPersonas no supere la capacidad de mesaId
        // (requiere consultar salon-service; fuera de alcance del Avance 1)

        Reserva reserva = new Reserva(datos.fechaHora(), datos.numPersonas(), cliente,
                datos.mesaId(), datos.meseroId());
        return ReservaDTO.de(reservaRepository.save(reserva));
    }
    
    public ReservaDTO cambiarEstado(Long id, EstadoReserva nuevoEstado) {
    Reserva reserva = buscarEntidad(id);
    EstadoReserva actual = reserva.getEstado();

    boolean transicionValida = switch (nuevoEstado) {
        case CONFIRMADA -> actual == EstadoReserva.PENDIENTE;
        case CANCELADA -> actual == EstadoReserva.PENDIENTE || actual == EstadoReserva.CONFIRMADA;
        case CUMPLIDA -> actual == EstadoReserva.CONFIRMADA;
        case PENDIENTE -> false;
    };

    if (!transicionValida) {
        throw new ConflictoException(
            "No se puede pasar una reserva de " + actual + " a " + nuevoEstado + ".");
    }

    if (nuevoEstado == EstadoReserva.CONFIRMADA) {
        reserva.confirmar();
    } else if (nuevoEstado == EstadoReserva.CANCELADA) {
        reserva.cancelar();
    } else {
        reserva.setEstado(nuevoEstado);
    }

    return ReservaDTO.de(reservaRepository.save(reserva));
}

    private Reserva buscarEntidad(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe una reserva con id " + id + "."));
    }
}