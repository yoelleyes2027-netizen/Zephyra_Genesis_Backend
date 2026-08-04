package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.DetalleTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleTicketRepository extends JpaRepository<DetalleTicket, Long> {
    List<DetalleTicket> findByTicketId(Long ticketId);
}