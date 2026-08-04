package com.zephyra.genesis.dto;

import java.util.List;

public record StockUpdateRequest(List<StockItemRequest> productos, String operacion) {
}