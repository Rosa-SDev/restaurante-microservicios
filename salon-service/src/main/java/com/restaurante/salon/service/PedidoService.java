package com.restaurante.salon.service;

import com.restaurante.salon.dto.AgregarPlatilloDTO;
import com.restaurante.salon.dto.CrearPedidoDTO;
import com.restaurante.salon.exception.ConflictoException;
import com.restaurante.salon.exception.NoEncontradoException;
import com.restaurante.salon.model.EstadoMesa;
import com.restaurante.salon.model.EstadoPedido;
import com.restaurante.salon.model.LineaPedido;
import com.restaurante.salon.model.Mesa;
import com.restaurante.salon.model.Pedido;
import com.restaurante.salon.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de los pedidos del restaurante.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaService mesaService;

    public PedidoService(PedidoRepository pedidoRepository, MesaService mesaService) {
        this.pedidoRepository = pedidoRepository;
        this.mesaService = mesaService;
    }

    /**
     * Da de alta un pedido y ocupa la mesa.
     * Una mesa RESERVADA sí acepta pedido (transición natural a OCUPADA).
     * Una mesa OCUPADA rechaza un nuevo pedido (HTTP 409).
     */
    @Transactional
    public Pedido crear(CrearPedidoDTO dto) {
        Mesa mesa = mesaService.buscarPorId(dto.mesaId());

        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new ConflictoException("La mesa " + mesa.getNumero() + " ya está ocupada por otro pedido.");
        }

        mesa.ocupar();
        Pedido pedido = new Pedido(mesa, dto.meseroId(), dto.clienteId(), dto.observaciones());
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar(EstadoPedido estado) {
        if (estado != null) {
            return pedidoRepository.findByEstadoConDetalles(estado);
        }
        return pedidoRepository.findAllConDetalles();
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new NoEncontradoException("El pedido " + id + " no existe."));
    }

    /**
     * Agrega una línea de platillo desnormalizada al pedido.
     */
    @Transactional
    public Pedido agregarPlatillo(Long id, AgregarPlatilloDTO dto) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new ConflictoException("Solo se pueden agregar platillos a un pedido abierto.");
        }

        // TODO Avance 2: Consultar carta-service para obtener nombre y precio unitario
        LineaPedido linea = new LineaPedido(dto.platilloId(), dto.nombre(), dto.precioUnitario(), pedido);
        pedido.agregarPlatillo(linea);

        return pedidoRepository.save(pedido);
    }

    /**
     * Quita una sola unidad/línea de platillo del pedido.
     */
    @Transactional
    public void quitarPlatillo(Long pedidoId, Long lineaId) {
        Pedido pedido = buscarPorId(pedidoId);

        if (pedido.getEstado() != EstadoPedido.ABIERTO) {
            throw new ConflictoException("Solo se pueden quitar platillos de un pedido abierto.");
        }

        boolean removido = pedido.quitarPlatillo(lineaId);
        if (!removido) {
            throw new NoEncontradoException("La línea de platillo " + lineaId + " no existe en este pedido.");
        }

        pedidoRepository.save(pedido);
    }

    /**
     * Cambia el estado del pedido (EN_PREPARACION, SERVIDO, CANCELADO).
     * Cancelar libera la mesa.
     */
    @Transactional
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new ConflictoException("No se puede modificar un pedido cancelado.");
        }
        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new ConflictoException("No se puede modificar un pedido ya pagado.");
        }

        if (nuevoEstado == EstadoPedido.CANCELADO) {
            pedido.cancelar();
        } else {
            pedido.setEstado(nuevoEstado);
        }

        return pedidoRepository.save(pedido);
    }

    /**
     * Cierra el pedido y libera su mesa. Requiere que esté pagado.
     */
    @Transactional
    public Pedido cerrar(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new ConflictoException("No se puede cerrar un pedido cancelado.");
        }
        if (pedido.getEstado() == EstadoPedido.PAGADO) {
            throw new ConflictoException("El pedido ya está cerrado.");
        }
        if (!pedido.estaPagado()) {
            throw new ConflictoException("No se puede cerrar el pedido sin un pago registrado.");
        }

        pedido.cerrar();
        return pedidoRepository.save(pedido);
    }
}
