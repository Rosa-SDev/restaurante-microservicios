package com.restaurante.salon.service;

import com.restaurante.salon.exception.ConflictoException;
import com.restaurante.salon.exception.NoEncontradoException;
import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.Mesa;
import com.restaurante.salon.repository.MesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de las mesas del restaurante.
 */
@Service
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    @Transactional
    public Mesa crear(Mesa mesa) {
        if (mesaRepository.existsByNumero(mesa.getNumero())) {
            throw new ConflictoException("Ya existe una mesa con el número " + mesa.getNumero() + ".");
        }
        return mesaRepository.save(mesa);
    }

    @Transactional(readOnly = true)
    public List<Mesa> listar(EstadoMesa estado) {
        if (estado != null) {
            return mesaRepository.findByEstado(estado);
        }
        return mesaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Mesa buscarPorId(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("La mesa " + id + " no existe."));
    }

    @Transactional
    public Mesa actualizar(Long id, Mesa datos) {
        Mesa mesa = buscarPorId(id);

        if (mesa.getNumero() != datos.getNumero() && mesaRepository.existsByNumeroAndIdNot(datos.getNumero(), id)) {
            throw new ConflictoException("Ya existe una mesa con el número " + datos.getNumero() + ".");
        }

        mesa.setNumero(datos.getNumero());
        mesa.setCapacidad(datos.getCapacidad());
        if (datos.getEstado() != null) {
            mesa.setEstado(datos.getEstado());
        }

        return mesaRepository.save(mesa);
    }

    @Transactional
    public void eliminar(Long id) {
        Mesa mesa = buscarPorId(id);
        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new ConflictoException("No se puede eliminar una mesa OCUPADA.");
        }
        mesaRepository.delete(mesa);
    }
}
