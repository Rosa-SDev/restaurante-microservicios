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

/**
 * Línea de un pedido que guarda una copia congelada de los datos del platillo.
 *
 * Desnormalización deliberada: se copian nombre y precioUnitario para que
 * aumentos futuros en la carta no alteren facturas históricas ya emitidas.
 * Cada platillo pedido representa una fila independiente en la tabla.
 */
@Entity
@Table(name = "pedido_platillos")
public class LineaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long platilloId; // Referencia al microservicio carta-service, sin FK en BD

    @Column(nullable = false)
    private String nombre; // Copia congelada

    @Column(name = "precio_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioUnitario; // Copia congelada

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    protected LineaPedido() {
    }

    public LineaPedido(Long platilloId, String nombre, BigDecimal precioUnitario, Pedido pedido) {
        this.platilloId = platilloId;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
        this.pedido = pedido;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlatilloId() {
        return platilloId;
    }

    public void setPlatilloId(Long platilloId) {
        this.platilloId = platilloId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }
}
