package com.zephyra.genesis.dto;

public record UsuarioAdminRequest(String nombre, int cedula, String contraseña, String rol, String email, Integer telefono, String tenantDatabase) {
}