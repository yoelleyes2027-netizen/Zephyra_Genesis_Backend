package com.zephyra.genesis.dto;

import com.zephyra.genesis.entity.FORMA_DE_PAGO;
import com.zephyra.genesis.entity.TIPO_MONEDA;

public record DevolucionRequest(FORMA_DE_PAGO formaDePago, TIPO_MONEDA tipoMoneda, Float montoPagado) {
}