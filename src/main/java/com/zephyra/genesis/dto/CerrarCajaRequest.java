package com.zephyra.genesis.dto;

public record CerrarCajaRequest(
        Float posDeclarado,
        Integer efectivoDeclarado,
        Integer cedulaAdmin,
        String contrasenaAdmin) {
}
