package com.zephyra.genesis.dto;

public record RegisterRequest(String nombre, int cedula, String contraseña, String rol, String email, Integer telefono) {
}
