package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.Date;

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

    @Column(name = "transferencia_calculada")
    private float transferenciaCalculada;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCierre;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;

    @Column
    private float diferenciaPos;

    @Column
    private float diferenciaEfectivo;

    @Column
    private float diferenciaDolares;

    @Column
    private float posCalculado;
    @Column
    private float posDeclarado;

    @Column
    private int efectivoCalculado;

    @Column
    private int efectivoDeclarado;

    @Column
    private float dolaresCalculados;

    @Column
    private float dolaresDeclarados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_global_id")
    private CajaGlobalEntity cajaGlobal;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    public CajaDiariaEntity() {
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

    public float getTransferenciaCalculada() {
        return transferenciaCalculada;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public float getDiferenciaPos() {
        return diferenciaPos;
    }

    public float getDiferenciaEfectivo() {
        return diferenciaEfectivo;
    }

    public float getDiferenciaDolares() {
        return diferenciaDolares;
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

    public float getDolaresCalculados() {
        return dolaresCalculados;
    }

    public float getDolaresDeclarados() {
        return dolaresDeclarados;
    }

    public CajaGlobalEntity getCajaGlobal() {
        return cajaGlobal;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
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

    public void setTransferenciaCalculada(float transferenciaCalculada) {
        this.transferenciaCalculada = transferenciaCalculada;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setDiferenciaPos(float diferenciaPos) {
        this.diferenciaPos = diferenciaPos;
    }

    public void setDiferenciaEfectivo(float diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
    }

    public void setDiferenciaDolares(float diferenciaDolares) {
        this.diferenciaDolares = diferenciaDolares;
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

    public void setDolaresCalculados(float dolaresCalculados) {
        this.dolaresCalculados = dolaresCalculados;
    }

    public void setDolaresDeclarados(float dolaresDeclarados) {
        this.dolaresDeclarados = dolaresDeclarados;
    }

    public void setCajaGlobal(CajaGlobalEntity cajaGlobal) {
        this.cajaGlobal = cajaGlobal;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }
}
