package com.restaurante.salon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Factura de un pedido.
 *
 * Aquí viven los impuestos: Pedido.calcularTotal() devuelve el consumo limpio
 * y esta clase le aplica el impuesto al consumo (8 %) y congela las tres cifras.
 */
@Entity
@Table(name = "facturas")
public class Factura {

    /** Impuesto al consumo del 8 %. Constante con dos decimales. */
    public static final BigDecimal IMPUESTO_CONSUMO = new BigDecimal("0.08");

    /** Todos los importes se guardan con dos decimales. */
    private static final int DECIMALES = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    private LocalDateTime fecha;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal impuestos;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private boolean anulada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "cajero_id", nullable = false)
    private Long cajeroId; // Referencia a usuarios-service

    protected Factura() {
    }

    public Factura(String numero, Pedido pedido, Long cajeroId) {
        this.numero = numero;
        this.pedido = pedido;
        this.cajeroId = cajeroId;
        this.subtotal = BigDecimal.ZERO.setScale(DECIMALES);
        this.impuestos = BigDecimal.ZERO.setScale(DECIMALES);
        this.total = BigDecimal.ZERO.setScale(DECIMALES);
        this.anulada = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public boolean isAnulada() {
        return anulada;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Long getCajeroId() {
        return cajeroId;
    }

    public void setCajeroId(Long cajeroId) {
        this.cajeroId = cajeroId;
    }

    /**
     * Congela las tres cifras y la fecha.
     *
     * Subtotal llega en escala 2 desde Pedido.calcularTotal().
     * Los impuestos se multiplican y redondean a 2 decimales con HALF_UP.
     * Total es la suma exacta de subtotal + impuestos ya redondeados.
     */
    public void emitir() {
        this.fecha = LocalDateTime.now();
        this.subtotal = pedido.calcularTotal();
        this.impuestos = subtotal.multiply(IMPUESTO_CONSUMO)
                                 .setScale(DECIMALES, RoundingMode.HALF_UP);
        this.total = subtotal.add(impuestos);
    }

    /**
     * Marca la factura como anulada sin poner los importes en cero,
     * para preservar el registro histórico de lo facturado.
     */
    public void anular() {
        this.anulada = true;
    }

    @Override
    public String toString() {
        return "Factura " + numero
                + " - subtotal $" + subtotal
                + " - impuestos $" + impuestos
                + " - total $" + total
                + (anulada ? " - ANULADA" : "");
    }
}
