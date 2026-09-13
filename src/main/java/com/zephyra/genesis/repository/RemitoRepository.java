package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.RemitoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemitoRepository extends JpaRepository<RemitoEntity, Long> {
    boolean existsByFacturaOrigen_Id(Long facturaOrigenId);

    Optional<RemitoEntity> findByFacturaOrigen_Id(Long facturaOrigenId);
}
