package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.FacturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface FacturaRepository extends JpaRepository<FacturaEntity, Long> {
    List<FacturaEntity> findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(Date fechaInicio);
}
