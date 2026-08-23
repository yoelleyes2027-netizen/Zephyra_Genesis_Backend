package com.zephyra.genesis.dto;

import com.zephyra.genesis.entity.TIPO_EGRESO;

public record EgresoRequest(TIPO_EGRESO tipoEgreso, Float monto) {
}
