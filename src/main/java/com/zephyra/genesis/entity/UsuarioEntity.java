package com.zephyra.genesis.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "usuario")
public class UsuarioEntity extends PersonaEntity {
    @Column(nullable = false, unique = true)
    private int cedula;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ROL rol;

    @Column(name = "tenant_database")
    private String tenantDatabase;

    @Lob
    @Column(name = "foto_perfil")
    private byte[] fotoPerfil;

    @Column
    private Date fechaInicioDeDia;

    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private CajaDiariaEntity cajaDiaria;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketEntity> tickets;

    public UsuarioEntity() {
    }

    public UsuarioEntity(String name, String email, int telefono, int cedula, String password, ROL rol,
         byte[] fotoPerfil, Date fechaInicioDeDia, CajaDiariaEntity cajaDiaria, List<TicketEntity> tickets) {
        super(name, email, telefono);
        this.cedula = cedula;
        this.password = password;
        this.rol = rol;
        this.fotoPerfil = fotoPerfil;
        this.fechaInicioDeDia = fechaInicioDeDia;
        this.cajaDiaria = cajaDiaria;
        this.tickets = tickets;
    }

    public int getCedula() {
        return cedula;
    }

    public void setCedula(int cedula) {
        this.cedula = cedula;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ROL getRol() {
        return rol;
    }

    public void setRol(ROL rol) {
        this.rol = rol;
    }

    public String getTenantDatabase() {
        return tenantDatabase;
    }

    public void setTenantDatabase(String tenantDatabase) {
        this.tenantDatabase = tenantDatabase;
    }

    public byte[] getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(byte[] fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public Date getFechaInicioDeDia() {
        return fechaInicioDeDia;
    }

    public void setFechaInicioDeDia(Date fechaInicioDeDia) {
        this.fechaInicioDeDia = fechaInicioDeDia;
    }

    public CajaDiariaEntity getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiariaEntity cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public List<TicketEntity> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketEntity> tickets) {
        this.tickets = tickets;
    }
}
