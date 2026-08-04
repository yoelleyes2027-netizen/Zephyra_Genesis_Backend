package com.zephyra.genesis.dto;

import java.util.Date;

public record UsuarioAdminResponse(Long id, String nombre, String email, int telefono, int cedula, String rol, String tenantDatabase, Date fechaCreacion) {
}