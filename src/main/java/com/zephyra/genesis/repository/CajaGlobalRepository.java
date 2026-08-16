package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.CajaGlobalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CajaGlobalRepository extends JpaRepository<CajaGlobalEntity, Long> {
	Optional<CajaGlobalEntity> findTopByOrderByIdDesc();
}