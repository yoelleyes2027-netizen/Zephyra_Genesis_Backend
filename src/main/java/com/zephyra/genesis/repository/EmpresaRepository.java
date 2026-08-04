package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.EmpresaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<EmpresaEntity, Long> {
    Optional<EmpresaEntity> findByNumeroDocumentoIgnoreCase(String numeroDocumento);
    List<EmpresaEntity> findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(String razonSocial);
}