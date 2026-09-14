package com.restaurante.clientes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurante.clientes.model.EstadoReserva;
import com.restaurante.clientes.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEstado(EstadoReserva estado);

    boolean existsByClienteId(Long clienteId);
}