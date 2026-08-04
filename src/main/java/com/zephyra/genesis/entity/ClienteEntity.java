package com.zephyra.genesis.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cliente")
public class ClienteEntity extends PersonaEntity {

    public ClienteEntity() {
    }

    public ClienteEntity(String name, String email, int telefono) {
        super(name, email, telefono);
    }

    public ClienteEntity(String name, String email, int telefono, List<TicketEntity> tickets) {
        super(name, email, telefono);
        this.tickets = tickets;
    }

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TicketEntity> tickets;

    public List<TicketEntity> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketEntity> tickets) {
        this.tickets = tickets;
    }

}
