package com.zephyra.genesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_factura")
public class DetalleFactura {
    @EmbeddedId
    private DetalleFacturaId id = new DetalleFacturaId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("facturaId")
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaEntity factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoId")
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_compra", nullable = false)
    private float precioCompra;

    public DetalleFacturaId getId() {
        return id;
    }

    public FacturaEntity getFactura() {
        return factura;
    }

    public ProductoEntity getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public float getPrecioCompra() {
        return precioCompra;
    }

    public void setId(DetalleFacturaId id) {
        this.id = id;
    }

    public void setFactura(FacturaEntity factura) {
        this.factura = factura;
    }

    public void setProducto(ProductoEntity producto) {
        this.producto = producto;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecioCompra(float precioCompra) {
        this.precioCompra = precioCompra;
    }
}
