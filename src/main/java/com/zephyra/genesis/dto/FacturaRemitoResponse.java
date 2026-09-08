package com.zephyra.genesis.dto;

import java.util.Date;
import java.util.List;

public record FacturaRemitoResponse(
        Long id,
        Integer nroFactura,
        String nroSerie,
        Date fechaEmision,
        Date fechaCreacion,
        Long proveedorId,
        String proveedorRazonSocial,
        List<DetalleFacturaRemitoResponse> detallesFactura) {
}
