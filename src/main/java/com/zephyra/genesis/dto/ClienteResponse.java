package com.zephyra.genesis.dto;

import java.util.Date;
///
public record ClienteResponse(Long id, String name, String email, int telefono, Date fechaCreacion) {
}