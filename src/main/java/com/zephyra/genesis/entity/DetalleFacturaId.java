package com.zephyra.genesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DetalleFacturaId implements Serializable {
    @Column(name = "factura_id")
    private Long facturaId;

    @Column(name = "producto_id")
    private Long productoId;

    public DetalleFacturaId() {
    }

    public DetalleFacturaId(Long facturaId, Long productoId) {
        this.facturaId = facturaId;
        this.productoId = productoId;
    }

    public Long getFacturaId() {
        return facturaId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setFacturaId(Long facturaId) {
        this.facturaId = facturaId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DetalleFacturaId other)) {
            return false;
        }
        return Objects.equals(facturaId, other.facturaId)
                && Objects.equals(productoId, other.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facturaId, productoId);
    }
}
