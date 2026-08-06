package com.zephyra.genesis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente_comun")
public class ClienteComunEntity extends ClienteEntity {

    public ClienteComunEntity() {
    }

    public ClienteComunEntity(String name, String email, int telefono) {
        super(name, email, telefono);
    }
}