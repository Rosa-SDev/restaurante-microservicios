package com.restaurante.clientes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.restaurante.clientes.dto.CambiarEstadoDTO;
import com.restaurante.clientes.dto.CrearReservaDTO;
import com.restaurante.clientes.dto.ReservaDTO;
import com.restaurante.clientes.model.EstadoReserva;
import com.restaurante.clientes.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaDTO> crear(@Valid @RequestBody CrearReservaDTO dto) {
        ReservaDTO creada = reservaService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping
    public List<ReservaDTO> listar(@RequestParam(required = false) EstadoReserva estado) {
        return reservaService.listar(estado);
    }

    @GetMapping("/{id}")
    public ReservaDTO buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id);
    }

    @PutMapping("/{id}/estado")
    public ReservaDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoDTO dto) {
        return reservaService.cambiarEstado(id, dto.nuevoEstado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        reservaService.eliminar(id);
    }
}