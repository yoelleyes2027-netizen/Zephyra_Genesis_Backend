package com.zephyra.genesis.dto;

public record RemitoResponse(
        Long remitoId,
        Long facturaOrigenId,
        Integer nroFactura,
        Integer cantidadTotalEmitida) {
}
