package com.restaurante.salon.controller;

import com.restaurante.salon.dto.CrearMesaDTO;
import com.restaurante.salon.dto.MesaDTO;
import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.Mesa;
import com.restaurante.salon.service.MesaService;
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
 * Controlador REST para la gestión de mesas.
 */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService servicio;

    public MesaController(MesaService servicio) {
        this.servicio = servicio;
    }

    /** 201 con la mesa creada. */
    @PostMapping
    public ResponseEntity<MesaDTO> crear(@Valid @RequestBody CrearMesaDTO datos) {
        Mesa creada = servicio.crear(datos.aEntidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(MesaDTO.de(creada));
    }

    /** 200 con el listado de mesas, con filtro opcional por estado. */
    @GetMapping
    public List<MesaDTO> listar(@RequestParam(required = false) EstadoMesa estado) {
        return servicio.listar(estado).stream()
                .map(MesaDTO::de)
                .toList();
    }

    /** 200 con la mesa encontrada, o 404 si no existe. */
    @GetMapping("/{id}")
    public MesaDTO buscar(@PathVariable Long id) {
        return MesaDTO.de(servicio.buscarPorId(id));
    }

    /** 200 con la mesa actualizada. */
    @PutMapping("/{id}")
    public MesaDTO actualizar(@PathVariable Long id, @Valid @RequestBody CrearMesaDTO datos) {
        return MesaDTO.de(servicio.actualizar(id, datos.aEntidad()));
    }

    /** 204 si la eliminación fue exitosa. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
