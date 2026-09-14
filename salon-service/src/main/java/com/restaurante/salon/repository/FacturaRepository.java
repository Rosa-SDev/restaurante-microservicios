package com.restaurante.salon.repository;

import com.restaurante.salon.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByPedidoId(Long pedidoId);

    Optional<Factura> findByPedidoIdAndAnuladaFalse(Long pedidoId);

    Optional<Factura> findByNumero(String numero);

    @Query("SELECT COALESCE(MAX(f.id), 0) FROM Factura f")
    Long findMaxId();

    @Query("SELECT f FROM Factura f JOIN FETCH f.pedido p LEFT JOIN FETCH p.mesa WHERE f.id = :id")
    Optional<Factura> findByIdConPedido(@Param("id") Long id);

    @Query("SELECT f FROM Factura f JOIN FETCH f.pedido p LEFT JOIN FETCH p.mesa")
    List<Factura> findAllConPedido();
}
