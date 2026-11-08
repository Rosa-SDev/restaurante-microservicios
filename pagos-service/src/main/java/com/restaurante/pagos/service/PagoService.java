package com.restaurante.pagos.service;

import com.restaurante.pagos.dto.CrearPagoDTO;
import com.restaurante.pagos.exception.ConflictoException;
import com.restaurante.pagos.exception.NoEncontradoException;
import com.restaurante.pagos.model.EstadoPago;
import com.restaurante.pagos.model.Pago;
import com.restaurante.pagos.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio para el registro, cambio de estado y eliminación de pagos.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public Pago registrar(CrearPagoDTO datos, String registradoPor) {
        if (pagoRepository.existsByFacturaIdAndEstado(datos.facturaId(), EstadoPago.APROBADO)) {
            throw new ConflictoException(
                    "La factura " + datos.facturaId() + " ya tiene un pago aprobado.");
        }

        Pago pago = new Pago(datos.facturaId(), datos.monto(), datos.metodo(), registradoPor);
        return pagoRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public List<Pago> listar(EstadoPago estado, Long facturaId) {
        if (estado != null && facturaId != null) {
            return pagoRepository.findByEstadoAndFacturaId(estado, facturaId);
        }
        if (estado != null) {
            return pagoRepository.findByEstado(estado);
        }
        if (facturaId != null) {
            return pagoRepository.findByFacturaId(facturaId);
        }
        return pagoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("El pago " + id + " no existe."));
    }

    @Transactional
    public Pago cambiarEstado(Long id, EstadoPago nuevo) {
        Pago pago = buscarPorId(id);
        EstadoPago actual = pago.getEstado();

        if (actual != EstadoPago.PENDIENTE
                || (nuevo != EstadoPago.APROBADO && nuevo != EstadoPago.RECHAZADO)) {
            throw new ConflictoException(
                    "No se puede cambiar un pago de " + actual + " a " + nuevo + ".");
        }

        if (nuevo == EstadoPago.APROBADO
                && pagoRepository.existsByFacturaIdAndEstado(pago.getFacturaId(), EstadoPago.APROBADO)) {
            throw new ConflictoException(
                    "La factura " + pago.getFacturaId() + " ya tiene un pago aprobado.");
        }

        pago.setEstado(nuevo);
        return pagoRepository.save(pago);
    }

    @Transactional
    public void eliminar(Long id) {
        Pago pago = buscarPorId(id);

        if (pago.getEstado() == EstadoPago.APROBADO) {
            throw new ConflictoException("Un pago aprobado no se puede eliminar.");
        }

        pagoRepository.delete(pago);
    }
}
