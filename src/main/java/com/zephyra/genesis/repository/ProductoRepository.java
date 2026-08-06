package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Long> {
    Optional<ProductoEntity> findByCodigoDeBarras(int codigoDeBarras);
    List<ProductoEntity> findByActivoTrueOrderByDescripcionAsc();
    List<ProductoEntity> findByActivoTrueAndDescripcionContainingIgnoreCaseOrderByDescripcionAsc(String descripcion);
    boolean existsByCodigoDeBarras(int codigoDeBarras);
    List<ProductoEntity> findByProveedorId_Id(Long proveedorId);
}