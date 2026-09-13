package com.restaurante.clientes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.restaurante.clientes.dto.ClienteDTO;
import com.restaurante.clientes.dto.CrearClienteDTO;
import com.restaurante.clientes.exception.ConflictoException;
import com.restaurante.clientes.exception.NoEncontradoException;
import com.restaurante.clientes.model.Cliente;
import com.restaurante.clientes.repository.ClienteRepository;
import com.restaurante.clientes.repository.ReservaRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public ClienteService(ClienteRepository clienteRepository, ReservaRepository reservaRepository) {
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<ClienteDTO> listar(String nombre) {
        List<Cliente> clientes = (nombre == null || nombre.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByNombreContainingIgnoreCase(nombre);
        return clientes.stream().map(ClienteDTO::de).toList();
    }

    public ClienteDTO buscarPorId(Long id) {
        return ClienteDTO.de(buscarEntidad(id));
    }

    public ClienteDTO crear(CrearClienteDTO datos) {
        if (clienteRepository.existsByDocumento(datos.documento())) {
            throw new ConflictoException(
                "Ya existe un cliente con el documento " + datos.documento() + ".");
        }
        Cliente cliente = new Cliente(datos.nombre(), datos.documento(), datos.telefono());
        return ClienteDTO.de(clienteRepository.save(cliente));
    }

    public ClienteDTO actualizar(Long id, CrearClienteDTO datos) {
        Cliente cliente = buscarEntidad(id);

        clienteRepository.findByDocumento(datos.documento())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ConflictoException(
                        "Ya existe otro cliente con el documento " + datos.documento() + ".");
                });

        cliente.setNombre(datos.nombre());
        cliente.setDocumento(datos.documento());
        cliente.setTelefono(datos.telefono());
        return ClienteDTO.de(clienteRepository.save(cliente));
    }

    public void eliminar(Long id) {
        Cliente cliente = buscarEntidad(id);
        if (reservaRepository.existsByClienteId(id)) {
            throw new ConflictoException(
                "No se puede eliminar el cliente " + cliente.getNombre() + " porque tiene reservas asociadas.");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe un cliente con id " + id + "."));
    }
}