package com.zephyra.genesis.dto;

import java.util.List;

public record RemitoRequest(
        Long facturaId,
        List<RemitoItemRequest> detalles) {
}
