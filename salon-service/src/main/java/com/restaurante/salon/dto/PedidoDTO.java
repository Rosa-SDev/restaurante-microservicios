package com.restaurante.salon.dto;

import com.restaurante.salon.model.EstadoPedido;
import com.restaurante.salon.model.MetodoPago;
import com.restaurante.salon.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que sale en el JSON de las respuestas de pedido.
 * Incluye las líneas de platillos desnormalizadas y el total calculado sin impuestos.
 */
public record PedidoDTO(Long id,
                        LocalDateTime fechaHora,
                        EstadoPedido estado,
                        String observaciones,
                        MetodoPago metodoPago,
                        BigDecimal montoPagado,
                        LocalDateTime fechaPago,
                        Long mesaId,
                        Integer mesaNumero,
                        Long meseroId,
                        Long cocineroId,
                        Long clienteId,
                        List<LineaPedidoDTO> platillos,
                        BigDecimal total) {

    public static PedidoDTO de(Pedido pedido) {
        if (pedido == null) {
            return null;
        }
        List<LineaPedidoDTO> lineas = pedido.getPlatillos() != null
                ? pedido.getPlatillos().stream().map(LineaPedidoDTO::de).toList()
                : List.of();

        Long mesaId = pedido.getMesa() != null ? pedido.getMesa().getId() : null;
        Integer mesaNumero = pedido.getMesa() != null ? pedido.getMesa().getNumero() : null;

        return new PedidoDTO(pedido.getId(),
                             pedido.getFechaHora(),
                             pedido.getEstado(),
                             pedido.getObservaciones(),
                             pedido.getMetodoPago(),
                             pedido.getMontoPagado(),
                             pedido.getFechaPago(),
                             mesaId,
                             mesaNumero,
                             pedido.getMeseroId(),
                             pedido.getCocineroId(),
                             pedido.getClienteId(),
                             lineas,
                             pedido.calcularTotal());
    }
}
