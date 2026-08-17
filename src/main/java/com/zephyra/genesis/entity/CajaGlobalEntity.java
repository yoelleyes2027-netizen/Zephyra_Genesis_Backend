package com.zephyra.genesis.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "caja_global")
public class CajaGlobalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Float totalIngresos;

    @Column
    private Float totalEgresos;

    @Column
    private Float transferenciaCalculada;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaInicio;

    @Column
    @Temporal(TemporalType.DATE)
    private Date fechaCierre;

    @Column
    private Float diferenciaPos;

    @Column
    private Float diferenciaEfectivo;

    @Column
    private Float posCalculado;

    @Column
    private Float posDeclarado;

    @Column
    private Integer efectivoCalculado;

    @Column
    private Integer efectivoDeclarado;

    @OneToMany(mappedBy = "cajaGlobal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CajaDiariaEntity> cajasDiarias = new ArrayList<>();

    public CajaGlobalEntity() {
    }

    public CajaGlobalEntity(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Long getId() {
        return id;
    }

    public Float getTotalIngresos() {
        return totalIngresos;
    }

    public Float getTotalEgresos() {
        return totalEgresos;
    }

    public Float getTransferenciaCalculada() {
        return transferenciaCalculada;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public Float getDiferenciaPos() {
        return diferenciaPos;
    }

    public Float getDiferenciaEfectivo() {
        return diferenciaEfectivo;
    }

    public Float getPosCalculado() {
        return posCalculado;
    }

    public Float getPosDeclarado() {
        return posDeclarado;
    }

    public Integer getEfectivoCalculado() {
        return efectivoCalculado;
    }

    public Integer getEfectivoDeclarado() {
        return efectivoDeclarado;
    }

    public List<CajaDiariaEntity> getCajasDiarias() {
        return cajasDiarias;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTotalIngresos(Float totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public void setTotalEgresos(Float totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public void setTransferenciaCalculada(Float transferenciaCalculada) {
        this.transferenciaCalculada = transferenciaCalculada;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public void setDiferenciaPos(Float diferenciaPos) {
        this.diferenciaPos = diferenciaPos;
    }

    public void setDiferenciaEfectivo(Float diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
    }

    public void setPosCalculado(Float posCalculado) {
        this.posCalculado = posCalculado;
    }

    public void setPosDeclarado(Float posDeclarado) {
        this.posDeclarado = posDeclarado;
    }

    public void setEfectivoCalculado(Integer efectivoCalculado) {
        this.efectivoCalculado = efectivoCalculado;
    }

    public void setEfectivoDeclarado(Integer efectivoDeclarado) {
        this.efectivoDeclarado = efectivoDeclarado;
    }

    public void setCajasDiarias(List<CajaDiariaEntity> cajasDiarias) {
        this.cajasDiarias = cajasDiarias;
    }
}