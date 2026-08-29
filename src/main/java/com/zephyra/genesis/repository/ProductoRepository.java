package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Long> {
    Optional<ProductoEntity> findByCodigoDeBarras(int codigoDeBarras);
    List<ProductoEntity> findByActivoTrueOrderByDescripcionAsc();
    List<ProductoEntity> findByActivoTrueAndDescripcionContainingIgnoreCaseOrderByDescripcionAsc(String descripcion);
    boolean existsByCodigoDeBarras(int codigoDeBarras);
    boolean existsByDescripcionIgnoreCase(String descripcion);
    List<ProductoEntity> findByProveedorId_Id(Long proveedorId);

        @Query("""
            SELECT p FROM ProductoEntity p
            WHERE p.activo = true
              AND p.proveedorId.id = :proveedorId
              AND (CAST(p.codigoDeBarras AS string) LIKE CONCAT('%', :busqueda, '%')
               OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%'))
               OR LOWER(p.etiqueta) LIKE LOWER(CONCAT('%', :busqueda, '%')))
            ORDER BY p.descripcion ASC
            """)
        List<ProductoEntity> buscarActivosPorProveedor(
            @Param("proveedorId") Long proveedorId,
            @Param("busqueda") String busqueda);
}