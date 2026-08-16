package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ticket")
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_creacion", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;

    @Column(name = "forma_de_pago", nullable = false)
    @Enumerated(EnumType.STRING)
    private FORMA_DE_PAGO formaDePago;

    @Column(name = "monto_total", nullable = false)
    private float montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_moneda")
    private TIPO_MONEDA tipoMoneda;

    @Column(name = "monto_pagado")
    private Float montoPagado;

    @Column(name = "cambio_entregado")
    private Float cambioEntregado;

    @Column(nullable = false)
    private boolean devolucion = false;

    @Column(name = "devolucion_realizada", nullable = false)
    private boolean devolucionRealizada = false;

    @Column(name = "egresos_descripcion")
    private String egresosDescripcion;

    @Column(name = "egreso", nullable = false)
    private boolean egreso = false;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<DetalleTicket> detalleTickets;

    public TicketEntity() {
    }

    public TicketEntity(FORMA_DE_PAGO formaDePago, float montoTotal,
         UsuarioEntity usuario, ClienteEntity cliente, 
        java.util.List<DetalleTicket> detalleTickets) {
        this.fechaCreacion = new Date();
        this.formaDePago = formaDePago;
        this.montoTotal = montoTotal;
        this.usuario = usuario;
        this.cliente = cliente;
        this.detalleTickets = detalleTickets;
    }

    public Long getId() {
        return id;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public FORMA_DE_PAGO getFormaDePago() {
        return formaDePago;
    }

    public float getMontoTotal() {
        return montoTotal;
    }

    public TIPO_MONEDA getTipoMoneda() {
        return tipoMoneda;
    }

    public Float getMontoPagado() {
        return montoPagado;
    }

    public Float getCambioEntregado() {
        return cambioEntregado;
    }

    public boolean isDevolucion() {
        return devolucion;
    }

    public boolean isDevolucionRealizada() {
        return devolucionRealizada;
    }

    public String getEgresosDescripcion() {
        return egresosDescripcion;
    }

    public boolean isEgreso() {
        return egreso;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public ClienteEntity getCliente() {
        return cliente;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setFormaDePago(FORMA_DE_PAGO formaDePago) {
        this.formaDePago = formaDePago;
    }

    public void setMontoTotal(float montoTotal) {
        this.montoTotal = montoTotal;
    }

    public void setTipoMoneda(TIPO_MONEDA tipoMoneda) {
        this.tipoMoneda = tipoMoneda;
    }

    public void setMontoPagado(Float montoPagado) {
        this.montoPagado = montoPagado;
    }

    public void setCambioEntregado(Float cambioEntregado) {
        this.cambioEntregado = cambioEntregado;
    }

    public void setDevolucion(boolean devolucion) {
        this.devolucion = devolucion;
    }

    public void setDevolucionRealizada(boolean devolucionRealizada) {
        this.devolucionRealizada = devolucionRealizada;
    }

    public void setEgresosDescripcion(String egresosDescripcion) {
        this.egresosDescripcion = egresosDescripcion;
    }

    public void setEgreso(boolean egreso) {
        this.egreso = egreso;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public void setCliente(ClienteEntity cliente) {
        this.cliente = cliente;
    }
    public java.util.List<DetalleTicket> getDetalleTickets() {
        return detalleTickets;
    }

    public void setDetalleTickets(java.util.List<DetalleTicket> detalleTickets) {
        this.detalleTickets = detalleTickets;
    }
}
