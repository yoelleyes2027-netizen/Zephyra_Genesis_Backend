package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ticket")
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCreacion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FORMA_DE_PAGO formaDePago;

    @Column(nullable = false)
    private float montoTotal;

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
