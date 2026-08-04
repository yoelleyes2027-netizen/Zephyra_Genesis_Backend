package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByCedula(int cedula);
    boolean existsByCedula(int cedula);
}
