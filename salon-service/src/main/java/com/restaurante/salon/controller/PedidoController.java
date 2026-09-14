package com.restaurante.salon.controller;

import com.restaurante.salon.dto.AgregarPlatilloDTO;
import com.restaurante.salon.dto.CambiarEstadoPedidoDTO;
import com.restaurante.salon.dto.CrearPedidoDTO;
import com.restaurante.salon.dto.PedidoDTO;
import com.restaurante.salon.model.EstadoPedido;
import com.restaurante.salon.model.Pedido;
import com.restaurante.salon.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión de pedidos y sus líneas de platillos.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    /** 201 con el pedido creado y mesa ocupada. */
    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@Valid @RequestBody CrearPedidoDTO datos) {
        Pedido creado = servicio.crear(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoDTO.de(creado));
    }

    /** 200 con el listado de pedidos, con filtro opcional por estado. */
    @GetMapping
    public List<PedidoDTO> listar(@RequestParam(required = false) EstadoPedido estado) {
        return servicio.listar(estado).stream()
                .map(PedidoDTO::de)
                .toList();
    }

    /** 200 con el pedido consultado, o 404 si no existe. */
    @GetMapping("/{id}")
    public PedidoDTO buscar(@PathVariable Long id) {
        return PedidoDTO.de(servicio.buscarPorId(id));
    }

    /** 200 con el pedido actualizado tras agregar una línea de platillo. */
    @PostMapping("/{id}/platillos")
    public PedidoDTO agregarPlatillo(@PathVariable Long id,
                                     @Valid @RequestBody AgregarPlatilloDTO datos) {
        return PedidoDTO.de(servicio.agregarPlatillo(id, datos));
    }

    /** 204 tras remover una línea de platillo del pedido. */
    @DeleteMapping("/{id}/platillos/{lineaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarPlatillo(@PathVariable Long id, @PathVariable Long lineaId) {
        servicio.quitarPlatillo(id, lineaId);
    }

    /** 200 con el pedido actualizado con su nuevo estado. */
    @PutMapping("/{id}/estado")
    public PedidoDTO cambiarEstado(@PathVariable Long id,
                                   @Valid @RequestBody CambiarEstadoPedidoDTO datos) {
        return PedidoDTO.de(servicio.cambiarEstado(id, datos.estado()));
    }
}
