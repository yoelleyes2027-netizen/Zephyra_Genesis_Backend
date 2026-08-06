package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {
	boolean existsByCliente_Id(Long clienteId);
}