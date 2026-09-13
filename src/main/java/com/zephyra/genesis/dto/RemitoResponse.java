package com.zephyra.genesis.dto;

public record RemitoResponse(
        Long facturaId,
        Integer nroFactura,
        Boolean remitoRealizado,
        Integer cantidadTotalEmitida) {
}
