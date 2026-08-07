package com.zephyra.genesis.dto;

import com.zephyra.genesis.entity.UNIDAD_MEDIDA;

public record ProductoRequest(int codigoDeBarras, String descripcion, float precioVenta, float precioCompra, int stock, UNIDAD_MEDIDA unidadDeMedida, String etiqueta, String proveedorNumeroDocumento) {
}