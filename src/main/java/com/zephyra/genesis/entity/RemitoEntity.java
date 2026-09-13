package com.zephyra.genesis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "remito")
@PrimaryKeyJoinColumn(name = "id")
public class RemitoEntity extends FacturaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_origen_id", nullable = false)
    private FacturaNormalEntity facturaOrigen;

    public FacturaNormalEntity getFacturaOrigen() {
        return facturaOrigen;
    }

    public void setFacturaOrigen(FacturaNormalEntity facturaOrigen) {
        this.facturaOrigen = facturaOrigen;
    }
}
