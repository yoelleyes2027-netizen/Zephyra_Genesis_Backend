package com.zephyra.genesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

@Entity
@Table(name = "remito")
@PrimaryKeyJoinColumn(name = "id")
public class RemitoEntity extends FacturaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_origen_id", nullable = false)
    private FacturaNormalEntity facturaOrigen;

    @Column(name = "fecha_emision_remito", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmisionRemito;

    public FacturaNormalEntity getFacturaOrigen() {
        return facturaOrigen;
    }

    public Date getFechaEmisionRemito() {
        return fechaEmisionRemito;
    }

    public void setFacturaOrigen(FacturaNormalEntity facturaOrigen) {
        this.facturaOrigen = facturaOrigen;
    }

    public void setFechaEmisionRemito(Date fechaEmisionRemito) {
        this.fechaEmisionRemito = fechaEmisionRemito;
    }
}
