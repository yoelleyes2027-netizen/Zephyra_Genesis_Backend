package com.zephyra.genesis.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "factura")
@PrimaryKeyJoinColumn(name = "id")
public class FacturaEntity extends DocumentoEntity {
    @Column(name = "fecha_emision", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @Column(nullable = false)
    private boolean remito;

    @Column(name = "nro_factura", unique = true, insertable = false, updatable = false)
    private Integer nroFactura;

    @Column(name = "nro_serie", nullable = false, unique = true)
    private String nroSerie;

    @Column(name = "remito_realizado", nullable = false)
    private boolean remitoRealizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private ProveedorEntity proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleFactura> detallesFactura = new ArrayList<>();

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public boolean isRemito() {
        return remito;
    }

    public Integer getNroFactura() {
        return nroFactura;
    }

    public String getNroSerie() {
        return nroSerie;
    }

    public boolean isRemitoRealizado() {
        return remitoRealizado;
    }

    public ProveedorEntity getProveedor() {
        return proveedor;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public List<DetalleFactura> getDetallesFactura() {
        return detallesFactura;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public void setRemito(boolean remito) {
        this.remito = remito;
    }

    public void setNroSerie(String nroSerie) {
        this.nroSerie = nroSerie;
    }

    public void setRemitoRealizado(boolean remitoRealizado) {
        this.remitoRealizado = remitoRealizado;
    }

    public void setProveedor(ProveedorEntity proveedor) {
        this.proveedor = proveedor;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public void setDetallesFactura(List<DetalleFactura> detallesFactura) {
        this.detallesFactura = detallesFactura;
    }
}
