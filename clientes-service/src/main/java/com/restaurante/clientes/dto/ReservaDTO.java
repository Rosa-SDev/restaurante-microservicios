package com.restaurante.clientes.dto;

import java.time.LocalDateTime;

import com.restaurante.clientes.model.EstadoReserva;

public class ReservaDTO {

    private Long id;
    private LocalDateTime fechaHora;
    private int numPersonas;
    private EstadoReserva estado;
    private Long clienteId;
    private String clienteNombre;
    private Long mesaId;
    private Long meseroId;

    public ReservaDTO(Long id, LocalDateTime fechaHora, int numPersonas, EstadoReserva estado,
                       Long clienteId, String clienteNombre, Long mesaId, Long meseroId) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.numPersonas = numPersonas;
        this.estado = estado;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.mesaId = mesaId;
        this.meseroId = meseroId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public Long getMeseroId() {
        return meseroId;
    }
}