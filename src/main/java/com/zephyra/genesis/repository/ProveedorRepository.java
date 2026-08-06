package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Long> {
    Optional<ProveedorEntity> findByNumeroDocumentoIgnoreCase(String numeroDocumento);
    List<ProveedorEntity> findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(String razonSocial);
    boolean existsByNumeroDocumentoIgnoreCase(String numeroDocumento);
    boolean existsByEmailIgnoreCase(String email);
}