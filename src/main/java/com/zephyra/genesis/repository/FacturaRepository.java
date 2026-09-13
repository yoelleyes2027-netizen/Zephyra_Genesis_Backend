package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.FacturaNormalEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface FacturaRepository extends JpaRepository<FacturaNormalEntity, Long> {
    @EntityGraph(attributePaths = {"proveedor", "detallesFactura", "detallesFactura.producto"})
    List<FacturaNormalEntity> findByNroSerieIgnoreCase(String nroSerie);

    @EntityGraph(attributePaths = {"proveedor", "detallesFactura", "detallesFactura.producto"})
    List<FacturaNormalEntity> findByProveedor_Id(Long proveedorId);

    List<FacturaNormalEntity> findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(Date fechaInicio);
}
