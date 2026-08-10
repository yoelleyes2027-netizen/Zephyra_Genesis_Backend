package com.zephyra.genesis.repository;

import com.zephyra.genesis.entity.DetalleTicket;
import com.zephyra.genesis.entity.DetalleTicketId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleTicketRepository extends JpaRepository<DetalleTicket, DetalleTicketId> {
    List<DetalleTicket> findByTicketId(Long ticketId);
}