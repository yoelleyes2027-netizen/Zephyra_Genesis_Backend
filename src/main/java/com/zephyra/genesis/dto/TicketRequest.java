package com.zephyra.genesis.dto;

import com.zephyra.genesis.entity.FORMA_DE_PAGO;

import java.util.List;

public record TicketRequest(Long clienteId, FORMA_DE_PAGO formaDePago, List<TicketItemRequest> detalleTickets) {
}