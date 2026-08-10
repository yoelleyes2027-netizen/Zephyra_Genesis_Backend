package com.zephyra.genesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DetalleTicketId implements Serializable {

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "producto_id")
    private Long productoId;

    public DetalleTicketId() {
    }

    public DetalleTicketId(Long ticketId, Long productoId) {
        this.ticketId = ticketId;
        this.productoId = productoId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getProductoId() {
        return productoId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DetalleTicketId that)) {
            return false;
        }
        return Objects.equals(ticketId, that.ticketId)
                && Objects.equals(productoId, that.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, productoId);
    }
}