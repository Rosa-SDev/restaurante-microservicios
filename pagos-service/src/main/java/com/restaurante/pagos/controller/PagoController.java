package com.restaurante.pagos.controller;

import com.restaurante.pagos.dto.CambiarEstadoPagoDTO;
import com.restaurante.pagos.dto.CrearPagoDTO;
import com.restaurante.pagos.dto.PagoDTO;
import com.restaurante.pagos.model.EstadoPago;
import com.restaurante.pagos.model.Pago;
import com.restaurante.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Controlador REST para el registro, consulta, cambio de estado y eliminación de pagos.
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    /** 201 con el pago registrado en estado PENDIENTE. */
    @PostMapping
    public ResponseEntity<PagoDTO> registrar(@Valid @RequestBody CrearPagoDTO datos,
                                             @AuthenticationPrincipal Jwt jwt) {
        Pago creado = servicio.registrar(datos, extraerRegistradoPor(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoDTO.de(creado));
    }

    private String extraerRegistradoPor(Jwt jwt) {
        String usuario = jwt.getClaimAsString("preferred_username");
        if (usuario != null && !usuario.isBlank()) {
            return usuario;
        }
        String cliente = jwt.getClaimAsString("azp");
        if (cliente != null && !cliente.isBlank()) {
            return cliente;
        }
        return jwt.getSubject();
    }

    /** 200 con la lista de pagos, con filtros opcionales por estado y facturaId. */
    @GetMapping
    public List<PagoDTO> listar(@RequestParam(required = false) EstadoPago estado,
                                @RequestParam(required = false) Long facturaId) {
        return servicio.listar(estado, facturaId).stream()
                .map(PagoDTO::de)
                .toList();
    }

    /** 200 con el pago consultado, o 404 si no existe. */
    @GetMapping("/{id}")
    public PagoDTO buscar(@PathVariable Long id) {
        return PagoDTO.de(servicio.buscarPorId(id));
    }

    /** 200 con el pago actualizado a APROBADO o RECHAZADO. */
    @PutMapping("/{id}/estado")
    public PagoDTO cambiarEstado(@PathVariable Long id,
                                 @Valid @RequestBody CambiarEstadoPagoDTO datos) {
        return PagoDTO.de(servicio.cambiarEstado(id, datos.estado()));
    }

    /** 204 tras eliminar un pago que no esté APROBADO. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
