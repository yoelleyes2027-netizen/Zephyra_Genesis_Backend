package com.zephyra.genesis.dto;

import java.util.Date;
import java.util.List;

public record TicketResponse(Long id, Date fechaCreacion, String formaDePago, float montoTotal, Long usuarioId, String usuarioNombre, Long clienteId, String clienteNombre, List<TicketDetalleResponse> detalleTickets) {
}