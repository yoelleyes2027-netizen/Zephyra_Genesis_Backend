package com.zephyra.genesis.dto;

public record TicketDetalleResponse(Long id, Long productoId, String productoDescripcion, int cantidad, float precioUnitario, float subtotal) {
}