package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {
    Optional<ClienteEntity> findByEmailIgnoreCase(String email);
    List<ClienteEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    boolean existsByEmailIgnoreCase(String email);
}