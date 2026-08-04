package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "caja_diaria")
public class CajaDiariaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private float totalIngresos;

    @Column
    private float totalEgresos;

    @Column
    @Temporal(TemporalType.DATE)
    private Date fechaCierre;

    @Column
    private float diferencia;

    @Column
    private float posCalculado;
    @Column
    private float posDeclarado;

    @Column
    private int efectivoCalculado;

    @Column
    private int efectivoDeclarado;

    @OneToMany(mappedBy = "cajaDiaria", cascade = CascadeType.ALL)
    private List<UsuarioEntity> usuarios;

    public CajaDiariaEntity() {
    }

    public CajaDiariaEntity(float totalIngresos, float totalEgresos, Date fechaCierre,
         float diferencia, float posCalculado, float posDeclarado,
          int efectivoCalculado, int efectivoDeclarado, List<UsuarioEntity> usuarios) {
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.fechaCierre = fechaCierre;
        this.diferencia = diferencia;
        this.posCalculado = posCalculado;
        this.posDeclarado = posDeclarado;
        this.efectivoCalculado = efectivoCalculado;
        this.efectivoDeclarado = efectivoDeclarado;
        this.usuarios = usuarios;
    }

    public Long getId() {
        return id;
    }

    public float getTotalIngresos() {
        return totalIngresos;
    }

    public float getTotalEgresos() {
        return totalEgresos;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public float getDiferencia() {
        return diferencia;
    }

    public float getPosCalculado() {
        return posCalculado;
    }

    public float getPosDeclarado() {
        return posDeclarado;
    }

    public int getEfectivoCalculado() {
        return efectivoCalculado;
    }

    public int getEfectivoDeclarado() {
        return efectivoDeclarado;
    }

    public List<UsuarioEntity> getUsuarios() {
        return usuarios;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTotalIngresos(float totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public void setTotalEgresos(float totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public void setDiferencia(float diferencia) {
        this.diferencia = diferencia;
    }

    public void setPosCalculado(float posCalculado) {
        this.posCalculado = posCalculado;
    }

    public void setPosDeclarado(float posDeclarado) {
        this.posDeclarado = posDeclarado;
    }

    public void setEfectivoCalculado(int efectivoCalculado) {
        this.efectivoCalculado = efectivoCalculado;
    }

    public void setEfectivoDeclarado(int efectivoDeclarado) {
        this.efectivoDeclarado = efectivoDeclarado;
    }

    public void setUsuarios(List<UsuarioEntity> usuarios) {
        this.usuarios = usuarios;
    }
}
