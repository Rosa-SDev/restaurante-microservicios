package com.restaurante.salon.controller;

import com.restaurante.salon.dto.EmitirFacturaDTO;
import com.restaurante.salon.dto.FacturaDTO;
import com.restaurante.salon.model.Factura;
import com.restaurante.salon.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la emisión, consulta y anulación de facturas.
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService servicio;

    public FacturaController(FacturaService servicio) {
        this.servicio = servicio;
    }

    /** 201 con la factura emitida. */
    @PostMapping
    public ResponseEntity<FacturaDTO> emitir(@Valid @RequestBody EmitirFacturaDTO datos) {
        Factura emitida = servicio.emitirFactura(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(FacturaDTO.de(emitida));
    }

    /** 200 con la lista de todas las facturas. */
    @GetMapping
    public List<FacturaDTO> listar() {
        return servicio.listar().stream()
                .map(FacturaDTO::de)
                .toList();
    }

    /** 200 con la factura consultada, o 404 si no existe. */
    @GetMapping("/{id}")
    public FacturaDTO buscar(@PathVariable Long id) {
        return FacturaDTO.de(servicio.buscarPorId(id));
    }

    /**
     * 200 con la factura anulada.
     * Retorna 200 (y no 204) porque no borra la fila de la base de datos,
     * sino que devuelve el comprobante en su nuevo estado anulado.
     */
    @DeleteMapping("/{id}")
    public FacturaDTO anular(@PathVariable Long id) {
        return FacturaDTO.de(servicio.anularFactura(id));
    }
}
