package com.zephyra.genesis.dto;

import java.util.Date;

public record EmpresaResponse(Long id, String name, String email, int telefono, String razonSocial, String tipoDocumento, String direccion, String numeroDocumento, Date fechaCreacion) {
}