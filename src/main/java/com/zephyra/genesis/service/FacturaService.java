package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.DetalleFacturaRequest;
import com.zephyra.genesis.dto.FacturaRequest;
import com.zephyra.genesis.dto.FacturaResponse;
import com.zephyra.genesis.entity.DetalleFactura;
import com.zephyra.genesis.entity.FacturaEntity;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.FacturaRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public FacturaService(
            FacturaRepository facturaRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            EntityManager entityManager) {
        this.facturaRepository = facturaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public FacturaResponse crear(FacturaRequest request, Long usuarioId) {
        validarRequest(request);
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.RECEPCION) {
            throw new IllegalArgumentException("Solo usuarios admin o recepcion pueden cargar facturas.");
        }

        ProveedorEntity proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        FacturaEntity factura = new FacturaEntity();
        factura.setFechaCreacion(new Date());
        factura.setTipoMoneda(request.tipoMoneda());
        factura.setFechaEmision(null);
        factura.setRemito(false);
        factura.setRemitoRealizado(false);
        factura.setNroSerie(normalizarNroSerie(request.nroSerie()));
        factura.setProveedor(proveedor);
        factura.setUsuario(usuario);

        Set<Long> productosIncluidos = new HashSet<>();
        float montoTotal = 0f;
        for (DetalleFacturaRequest item : request.detalles()) {
            validarItem(item, productosIncluidos);
            ProductoEntity producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
            if (producto.getproveedorId() == null
                    || !proveedor.getId().equals(producto.getproveedorId().getId())) {
                throw new IllegalArgumentException("Todos los productos deben pertenecer al proveedor seleccionado.");
            }

            DetalleFactura detalle = new DetalleFactura();
            detalle.setFactura(factura);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioCompra(item.precioCompra());
            factura.getDetallesFactura().add(detalle);

            // La carga de factura representa ingreso de mercaderia, por eso aumenta stock.
            producto.setStock(producto.getStock() + item.cantidad());
            producto.setFechaUltimoIngreso(new Date());
            montoTotal += item.cantidad() * item.precioCompra();
        }

        factura.setMontoTotal(montoTotal);
        FacturaEntity facturaGuardada = facturaRepository.save(factura);
        entityManager.flush();
        entityManager.refresh(facturaGuardada);
        return toResponse(facturaGuardada);
    }

    @Transactional
    public void actualizarPreciosCompraDesde(Date fechaInicio) {
        if (fechaInicio == null) {
            return;
        }
        List<FacturaEntity> facturas = facturaRepository.findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(fechaInicio);
        for (FacturaEntity factura : facturas) {
            for (DetalleFactura detalle : factura.getDetallesFactura()) {
                ProductoEntity producto = detalle.getProducto();
                producto.setPrecioCompra(detalle.getPrecioCompra());
            }
        }
    }

    private void validarRequest(FacturaRequest request) {
        if (request == null || request.proveedorId() == null || request.tipoMoneda() == null
                || request.detalles() == null || request.detalles().isEmpty()) {
            throw new IllegalArgumentException("Proveedor, moneda y al menos un producto son obligatorios.");
        }
    }

    private void validarItem(DetalleFacturaRequest item, Set<Long> productosIncluidos) {
        if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0
                || item.precioCompra() == null || item.precioCompra() < 0) {
            throw new IllegalArgumentException("Cada producto debe tener una cantidad y precio de compra validos.");
        }
        if (!productosIncluidos.add(item.productoId())) {
            throw new IllegalArgumentException("No se puede repetir un producto en la factura.");
        }
    }

    private String normalizarNroSerie(String nroSerie) {
        if (nroSerie == null || nroSerie.isBlank()) {
            return null;
        }
        return nroSerie.trim();
    }

    private FacturaResponse toResponse(FacturaEntity factura) {
        return new FacturaResponse(
                factura.getId(),
                factura.getNroFactura(),
                factura.getNroSerie(),
                factura.getFechaCreacion(),
                factura.getMontoTotal(),
                factura.getTipoMoneda().name());
    }
}
