package com.zephyra.genesis.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "empresa")
public class EmpresaEntity extends ClienteEntity {

    @Column(name = "razonsocial", nullable = false)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipodocumento", nullable = false)
    private TIPO_DOCUMENTO tipoDocumento;

    @Column(nullable = false)
    private String direccion;

    @Column(name = "numerodocumento", nullable = false, unique = true)
    private String numeroDocumento;

    public EmpresaEntity() {
    }

    public EmpresaEntity(String name, String email, int telefono, String razonSocial, TIPO_DOCUMENTO tipoDocumento, String direccion, String numeroDocumento) {
        super(name, email, telefono);
        this.razonSocial = razonSocial;
        this.tipoDocumento = tipoDocumento;
        this.direccion = direccion;
        this.numeroDocumento = numeroDocumento;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public TIPO_DOCUMENTO getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TIPO_DOCUMENTO tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }
}
