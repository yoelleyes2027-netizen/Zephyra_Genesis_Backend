package com.zephyra.genesis.dto;

public record DetalleFacturaRemitoResponse(
        Long productoId,
        String productoDescripcion,
        Integer cantidad,
        Float precioCompra) {
}
