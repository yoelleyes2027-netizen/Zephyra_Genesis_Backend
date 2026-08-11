package com.zephyra.genesis.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "detalle_ticket")
public class DetalleTicket {
    @EmbeddedId
    private DetalleTicketId id = new DetalleTicketId();

    @ManyToOne
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @ManyToOne
    @MapsId("productoId")
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private float precioUnitario;

    public DetalleTicketId getId() {
        return id;
    }

    public void setId(DetalleTicketId id) {
        this.id = id;
    }

    public TicketEntity getTicket() {
        return ticket;
    }

    public void setTicket(TicketEntity ticket) {
        this.ticket = ticket;
    }

    public ProductoEntity getProducto() {
        return producto;
    }

    public void setProducto(ProductoEntity producto) {
        this.producto = producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public float getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(float precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public DetalleTicket() {
    }

    public DetalleTicket(TicketEntity ticket, ProductoEntity producto, int cantidad, float precioUnitario) {
        this.ticket = ticket;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }
}
