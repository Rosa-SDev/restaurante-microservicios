package com.restaurante.salon.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de una mesa.
 *
 * La lista guarda LineaPedido con repetición (dos hamburguesas = dos filas en BD).
 * La cantidad solo existe en la vista.
 *
 * El total calculado es el consumo SIN impuestos. Los impuestos y el total
 * definitivo son responsabilidad de Factura.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    private static final int DECIMALES = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago")
    private MetodoPago metodoPago;

    @Column(name = "monto_pagado", precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @Column(name = "mesero_id", nullable = false)
    private Long meseroId; // Referencia a usuarios-service

    @Column(name = "cocinero_id")
    private Long cocineroId; // Referencia a usuarios-service

    @Column(name = "cliente_id")
    private Long clienteId; // Referencia a clientes-service, puede ser null

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaPedido> platillos = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Mesa mesa, Long meseroId, Long clienteId, String observaciones) {
        this.mesa = mesa;
        this.meseroId = meseroId;
        this.clienteId = clienteId;
        this.observaciones = observaciones;
        this.fechaHora = LocalDateTime.now();
        this.estado = EstadoPedido.ABIERTO;
        this.platillos = new ArrayList<>();
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

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void setMesa(Mesa mesa) {
        this.mesa = mesa;
    }

    public Long getMeseroId() {
        return meseroId;
    }

    public void setMeseroId(Long meseroId) {
        this.meseroId = meseroId;
    }

    public Long getCocineroId() {
        return cocineroId;
    }

    public void setCocineroId(Long cocineroId) {
        this.cocineroId = cocineroId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<LineaPedido> getPlatillos() {
        return platillos;
    }

    /** Agrega una línea de platillo asociada al pedido. */
    public void agregarPlatillo(LineaPedido linea) {
        platillos.add(linea);
        linea.setPedido(this);
    }

    /** Quita una línea específica por su ID. */
    public boolean quitarPlatillo(Long lineaId) {
        return platillos.removeIf(l -> l.getId() != null && l.getId().equals(lineaId));
    }

    /**
     * Suma de los precios de los platillos, SIN impuestos.
     * Siempre devuelve con escala 2 y RoundingMode.HALF_UP (nunca null).
     */
    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (LineaPedido pl : platillos) {
            total = total.add(pl.getPrecioUnitario());
        }
        return total.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    /**
     * Registra el pago en el pedido.
     */
    public void registrarPago(MetodoPago met, BigDecimal monto) {
        this.metodoPago = met;
        this.montoPagado = monto;
        this.fechaPago = LocalDateTime.now();
    }

    /**
     * Verdadero cuando hay un pago registrado que cubre al menos el consumo sin impuestos.
     */
    public boolean estaPagado() {
        if (metodoPago == null || montoPagado == null) {
            return false;
        }
        return montoPagado.compareTo(calcularTotal()) >= 0;
    }

    /**
     * Cierra el pedido y libera la mesa.
     */
    public void cerrar() {
        this.estado = EstadoPedido.PAGADO;
        if (mesa != null) {
            mesa.liberar();
        }
    }

    /**
     * Cancela el pedido y libera la mesa.
     */
    public void cancelar() {
        this.estado = EstadoPedido.CANCELADO;
        if (mesa != null) {
            mesa.liberar();
        }
    }

    @Override
    public String toString() {
        return "Pedido #" + id
                + " - " + estado
                + " - mesa " + (mesa != null ? mesa.getNumero() : "-")
                + " - " + platillos.size() + " platillos"
                + " - $" + calcularTotal();
    }
}
