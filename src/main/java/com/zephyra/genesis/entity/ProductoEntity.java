package com.zephyra.genesis.entity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "producto")
public class ProductoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private int codigoDeBarras;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fechadeingreso", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaDeIngreso;

    @Column(name = "fechaultimoingreso", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaUltimoIngreso;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UNIDAD_MEDIDA unidadDeMedida;

    @Column(nullable = false)
    private float precioCompra;

    @Column(nullable = false, unique = true)
    private String descripcion;

    @Column(nullable = false)
    private float precioVenta;

    @Column(nullable = false)
    private String etiqueta;

    @ManyToOne
    @JoinColumn(name = "proveedorId", nullable = false)
    private ProveedorEntity proveedorId;

    public ProductoEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaDeIngreso == null) {
            fechaDeIngreso = new Date();
        }
        if (fechaUltimoIngreso == null) {
            fechaUltimoIngreso = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (fechaUltimoIngreso == null) {
            fechaUltimoIngreso = new Date();
        }
    }

    public ProductoEntity(int codigoDeBarras, boolean activo, Date fechaUltimoIngreso, int stock, UNIDAD_MEDIDA unidadDeMedida, float precioCompra, String descripcion, float precioVenta, String etiqueta, ProveedorEntity proveedorId) {
        this.codigoDeBarras = codigoDeBarras;
        this.activo = activo;
        this.fechaDeIngreso = new Date();
        this.fechaUltimoIngreso = fechaUltimoIngreso != null ? fechaUltimoIngreso : new Date();
        this.stock = stock;
        this.unidadDeMedida = unidadDeMedida;
        this.precioCompra = precioCompra;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.etiqueta = etiqueta;
        this.proveedorId = proveedorId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCodigoDeBarras() {
        return codigoDeBarras;
    }

    public void setCodigoDeBarras(int codigoDeBarras) {
        this.codigoDeBarras = codigoDeBarras;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Date getFechaDeIngreso() {
        return fechaDeIngreso;
    }

    public void setFechaDeIngreso(Date fechaDeIngreso) {
        this.fechaDeIngreso = fechaDeIngreso;
    }

    public Date getFechaUltimoIngreso() {
        return fechaUltimoIngreso;
    }

    public void setFechaUltimoIngreso(Date fechaUltimoIngreso) {
        this.fechaUltimoIngreso = fechaUltimoIngreso;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public UNIDAD_MEDIDA getUnidadDeMedida() {
        return unidadDeMedida;
    }

    public void setUnidadDeMedida(UNIDAD_MEDIDA unidadDeMedida) {
        this.unidadDeMedida = unidadDeMedida;
    }

    public float getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(float precioCompra) {
        this.precioCompra = precioCompra;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public float getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(float precioVenta) {
        this.precioVenta = precioVenta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }
    public ProveedorEntity getproveedorId() {
        return proveedorId;
    }

    public void setproveedorId(ProveedorEntity proveedorId) {
        this.proveedorId = proveedorId;
    }
}
