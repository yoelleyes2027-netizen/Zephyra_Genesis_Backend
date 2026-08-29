package com.zephyra.genesis.dto;

import java.util.Date;

public record FacturaResponse(
        Long id,
        Integer nroFactura,
        String nroSerie,
        Date fechaCreacion,
        float montoTotal,
        String tipoMoneda) {
}
