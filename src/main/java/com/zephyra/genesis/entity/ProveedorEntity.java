package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "proveedor")
public class ProveedorEntity extends PersonaEntity {

    @Column(nullable = false, unique = true)
    private String numeroDocumento;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TIPO_DOCUMENTO tipoDocumento;

    @OneToMany(mappedBy = "proveedorId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoEntity> productos;

    public ProveedorEntity() {
    }

    public ProveedorEntity(String name, String email, int telefono, String numeroDocumento, String direccion,
         String razonSocial, TIPO_DOCUMENTO tipoDocumento, List<ProductoEntity> productos) {
        super(name, email, telefono);
        this.numeroDocumento = numeroDocumento;
        this.direccion = direccion;
        this.razonSocial = razonSocial;
        this.tipoDocumento = tipoDocumento;
        this.productos = productos;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
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
    public List<ProductoEntity> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoEntity> productos) {
        this.productos = productos;
    }
}
