package com.zephyra.genesis.dto;

public record TicketDetalleResponse(Long ticketId, Long productoId, String productoDescripcion, int cantidad, float precioUnitario, float subtotal) {
}