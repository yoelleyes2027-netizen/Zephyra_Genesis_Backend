package com.zephyra.genesis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

@Entity
@Table(name = "documento")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class DocumentoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto_total", nullable = false)
    private float montoTotal;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "tipo_moneda", nullable = false)
    private TIPO_MONEDA tipoMoneda;

    @Column(name = "fecha_creacion", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;

    public Long getId() {
        return id;
    }

    public float getMontoTotal() {
        return montoTotal;
    }

    public TIPO_MONEDA getTipoMoneda() {
        return tipoMoneda;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMontoTotal(float montoTotal) {
        this.montoTotal = montoTotal;
    }

    public void setTipoMoneda(TIPO_MONEDA tipoMoneda) {
        this.tipoMoneda = tipoMoneda;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
