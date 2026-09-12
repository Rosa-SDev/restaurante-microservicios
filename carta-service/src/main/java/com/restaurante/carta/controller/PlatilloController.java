package com.restaurante.carta.controller;

import com.restaurante.carta.dto.CrearPlatilloDTO;
import com.restaurante.carta.dto.DescuentoDTO;
import com.restaurante.carta.dto.PlatilloDTO;
import com.restaurante.carta.model.Platillo;
import com.restaurante.carta.service.PlatilloService;

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
 * La puerta HTTP de la carta.
 *
 * Solo traduce: lee el JSON, llama al servicio, convierte el resultado a DTO y
 * fija el codigo de respuesta. Ninguna regla de negocio vive aqui.
 */
@RestController
@RequestMapping("/api/platillos")
public class PlatilloController {

    private final PlatilloService servicio;

    public PlatilloController(PlatilloService servicio) {
        this.servicio = servicio;
    }

    /** 201 con el platillo creado. */
    @PostMapping
    public ResponseEntity<PlatilloDTO> crear(@Valid @RequestBody CrearPlatilloDTO datos) {
        Platillo creado = servicio.crear(datos.aEntidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlatilloDTO.de(creado));
    }

    /** 200 con la lista. Los dos filtros son opcionales. */
    @GetMapping
    public List<PlatilloDTO> listar(@RequestParam(required = false) String categoria,
                                    @RequestParam(required = false) Boolean disponible) {
        return servicio.listar(categoria, disponible).stream()
                .map(PlatilloDTO::de)
                .toList();
    }

    /** 200, o 404 si no existe. */
    @GetMapping("/{id}")
    public PlatilloDTO buscar(@PathVariable Long id) {
        return PlatilloDTO.de(servicio.buscarPorId(id));
    }

    /** 200 con el platillo ya actualizado. */
    @PutMapping("/{id}")
    public PlatilloDTO actualizar(@PathVariable Long id,
                                  @Valid @RequestBody CrearPlatilloDTO datos) {
        return PlatilloDTO.de(servicio.actualizar(id, datos.aEntidad()));
    }

    /** 204: salio bien, no hay nada que devolver. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    /** 200 con el platillo ya rebajado, para no obligar a otro GET. */
    @PutMapping("/{id}/descuento")
    public PlatilloDTO aplicarDescuento(@PathVariable Long id,
                                        @Valid @RequestBody DescuentoDTO datos) {
        return PlatilloDTO.de(servicio.aplicarDescuento(id, datos.porcentaje()));
    }
}
