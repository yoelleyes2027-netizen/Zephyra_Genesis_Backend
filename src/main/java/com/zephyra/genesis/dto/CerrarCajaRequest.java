package com.zephyra.genesis.dto;

public record CerrarCajaRequest(
        Float posDeclarado,
        Integer efectivoDeclarado,
        Float dolaresDeclarados,
        Integer cedulaAdmin,
        String contrasenaAdmin) {
}
