package com.restaurante.pagos.repository;

import com.restaurante.pagos.model.EstadoPago;
import com.restaurante.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByEstado(EstadoPago estado);

    List<Pago> findByFacturaId(Long facturaId);

    List<Pago> findByEstadoAndFacturaId(EstadoPago estado, Long facturaId);

    boolean existsByFacturaIdAndEstado(Long facturaId, EstadoPago estado);
}
