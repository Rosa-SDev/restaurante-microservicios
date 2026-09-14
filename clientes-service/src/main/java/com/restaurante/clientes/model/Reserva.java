package com.restaurante.clientes.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Reserva de una mesa a nombre de un cliente.
 *
 * mesaId y meseroId son solo el identificador (Long), no el objeto: Mesa vive
 * en salon-service y Mesero en usuarios-service, cada uno con su propia base
 * de datos. No hay relación JPA posible entre servicios distintos.
 *
 * Por lo mismo, confirmar()/cancelar() aquí solo cambian el estado propio de
 * la reserva — el original también tocaba el estado de la Mesa, pero eso ya
 * no es posible sin el objeto real. Validar mesa/capacidad queda fuera del
 * Avance 1 (decisión D8 del contrato).
 */
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHora;

    private int numPersonas;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private Long mesaId;
    private Long meseroId;

    protected Reserva() {
        // constructor vacío que exige JPA
    }

    public Reserva(LocalDateTime fechaHora, int numPersonas, Cliente cliente,
                    Long mesaId, Long meseroId) {
        this.fechaHora = fechaHora;
        this.numPersonas = numPersonas;
        this.cliente = cliente;
        this.mesaId = mesaId;
        this.meseroId = meseroId;
        this.estado = EstadoReserva.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
    }

    public Long getMeseroId() {
        return meseroId;
    }

    public void setMeseroId(Long meseroId) {
        this.meseroId = meseroId;
    }

    // TODO Avance 2: validar que numPersonas no supere la capacidad de la mesa
    // (requiere consultar salon-service; fuera de alcance del Avance 1)
    public void confirmar() {
        this.estado = EstadoReserva.CONFIRMADA;
    }

    // TODO Avance 2: comprobar que la mesa no esté ya ocupada al confirmar
    // (requiere consultar salon-service; fuera de alcance del Avance 1)
    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    @Override
    public String toString() {
        return "Reserva #" + id
                + " - " + estado
                + " - " + (cliente != null ? cliente.getNombre() : "-")
                + " - mesa " + mesaId
                + " - " + numPersonas + " personas"
                + " - " + fechaHora;
    }
}