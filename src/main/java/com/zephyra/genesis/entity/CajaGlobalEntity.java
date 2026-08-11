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
    private float totalIngresos;

    @Column
    private float totalEgresos;

    @Column
    @Temporal(TemporalType.DATE)
    private Date fechaInicio;

    @Column
    @Temporal(TemporalType.DATE)
    private Date fechaCierre;

    @Column
    private float diferencia;

    @Column
    private float diferenciaPos;

    @Column
    private float diferenciaEfectivo;

    @Column
    private float posCalculado;

    @Column
    private float posDeclarado;

    @Column
    private int efectivoCalculado;

    @Column
    private int efectivoDeclarado;

    @OneToMany(mappedBy = "cajaGlobal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CajaDiariaEntity> cajasDiarias = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public float getTotalIngresos() {
        return totalIngresos;
    }

    public float getTotalEgresos() {
        return totalEgresos;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public Date getFechaCierre() {
        return fechaCierre;
    }

    public float getDiferencia() {
        return diferencia;
    }

    public float getDiferenciaPos() {
        return diferenciaPos;
    }

    public float getDiferenciaEfectivo() {
        return diferenciaEfectivo;
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

    public List<CajaDiariaEntity> getCajasDiarias() {
        return cajasDiarias;
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

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaCierre(Date fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public void setDiferencia(float diferencia) {
        this.diferencia = diferencia;
    }

    public void setDiferenciaPos(float diferenciaPos) {
        this.diferenciaPos = diferenciaPos;
    }

    public void setDiferenciaEfectivo(float diferenciaEfectivo) {
        this.diferenciaEfectivo = diferenciaEfectivo;
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

    public void setCajasDiarias(List<CajaDiariaEntity> cajasDiarias) {
        this.cajasDiarias = cajasDiarias;
    }
}