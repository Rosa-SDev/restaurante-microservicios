package com.restaurante.salon.repository;

import com.restaurante.salon.model.EstadoPedido;
import com.restaurante.salon.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByEstado(EstadoPedido estado);

    boolean existsByMesaIdAndEstado(Long mesaId, EstadoPedido estado);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.platillos LEFT JOIN FETCH p.mesa WHERE p.id = :id")
    Optional<Pedido> findByIdConDetalles(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.platillos LEFT JOIN FETCH p.mesa")
    List<Pedido> findAllConDetalles();

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.platillos LEFT JOIN FETCH p.mesa WHERE p.estado = :estado")
    List<Pedido> findByEstadoConDetalles(@Param("estado") EstadoPedido estado);
}
