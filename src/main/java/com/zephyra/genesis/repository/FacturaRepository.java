package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.FacturaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface FacturaRepository extends JpaRepository<FacturaEntity, Long> {
    @EntityGraph(attributePaths = {"proveedor", "detallesFactura", "detallesFactura.producto"})
    List<FacturaEntity> findByNroSerieIgnoreCase(String nroSerie);

    @EntityGraph(attributePaths = {"proveedor", "detallesFactura", "detallesFactura.producto"})
    List<FacturaEntity> findByProveedor_Id(Long proveedorId);

    List<FacturaEntity> findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(Date fechaInicio);
}
