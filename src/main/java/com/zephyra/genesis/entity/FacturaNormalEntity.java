package com.zephyra.genesis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "factura_normal")
@PrimaryKeyJoinColumn(name = "id")
public class FacturaNormalEntity extends FacturaEntity {
}
