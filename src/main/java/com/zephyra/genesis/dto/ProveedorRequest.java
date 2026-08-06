package com.zephyra.genesis.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ProveedorRequest(
	String name,
	String email,
	int telefono,
	@JsonAlias({"numero_documento"}) String numeroDocumento,
	String direccion,
	@JsonAlias({"razon_social"}) String razonSocial,
	@JsonAlias({"tipo_documento"}) String tipoDocumento) {
}