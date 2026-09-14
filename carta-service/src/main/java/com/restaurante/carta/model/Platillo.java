package com.restaurante.carta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Un plato de la carta. Es la tabla "platillos" de la base de datos.
 *
 * La categoria es un String, no una clase: el diagrama corregido elimino
 * CategoriaPlatillo. El precio es BigDecimal, nunca double, porque los double
 * acumulan error de redondeo en operaciones de dinero.
 *
 * Esta clase no valida nada: solo representa el dato. Las reglas de negocio
 * viven en PlatilloService y las validaciones de formato en los DTO.
 */
@Entity
@Table(name = "platillos")
public class Platillo implements IDescontable {

    /** Todos los importes se guardan con dos decimales. */
    private static final int DECIMALES = 2;
    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    private boolean disponible;

    /**
     * Constructor vacio que exige JPA: construye el objeto sin datos y despues
     * rellena los campos. No lo borres.
     */
    protected Platillo() {
    }

    /** El id no se recibe: lo genera la base de datos al guardar. */
    public Platillo(String nombre, String descripcion, String categoria,
                    BigDecimal precio, boolean disponible) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precio = normalizar(precio);
        this.disponible = disponible;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    /** Operacion del diagrama. El precio no tiene setter: se cambia por aqui. */
    public void actualizarPrecio(BigDecimal nuevoPrecio) {
        this.precio = normalizar(nuevoPrecio);
    }

    /** Operacion del diagrama. */
    public void cambiarDisponibilidad(boolean disponible) {
        this.disponible = disponible;
    }

    /**
     * Rebaja el precio en el porcentaje indicado y devuelve cuanto se desconto.
     * Es la implementacion de IDescontable.
     */
    @Override
    public BigDecimal aplicarDescuento(BigDecimal porcentaje) {
        BigDecimal descuento = precio.multiply(porcentaje)
                                     .divide(CIEN, DECIMALES, RoundingMode.HALF_UP);
        this.precio = precio.subtract(descuento);
        return descuento;
    }

    /** Deja todos los importes con la misma escala para que las comparaciones sean fiables. */
    private static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(DECIMALES)
                             : valor.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return nombre + " ($" + precio + ")";
    }
}
