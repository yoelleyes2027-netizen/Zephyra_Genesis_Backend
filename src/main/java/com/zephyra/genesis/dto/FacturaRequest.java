package com.zephyra.genesis.dto;

import com.zephyra.genesis.entity.TIPO_MONEDA;

import java.time.LocalDate;
import java.util.List;

public record FacturaRequest(
        Long proveedorId,
        TIPO_MONEDA tipoMoneda,
        String nroSerie,
        LocalDate fechaEmision,
        List<DetalleFacturaRequest> detalles) {
}
