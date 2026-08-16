package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.CajaDiariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CajaDiariaRepository extends JpaRepository<CajaDiariaEntity, Long> {
	Optional<CajaDiariaEntity> findByUsuario_Id(Long usuarioId);
}