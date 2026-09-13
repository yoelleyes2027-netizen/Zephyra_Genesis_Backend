package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.DetalleFacturaRequest;
import com.zephyra.genesis.dto.DetalleFacturaRemitoResponse;
import com.zephyra.genesis.dto.FacturaRequest;
import com.zephyra.genesis.dto.FacturaRemitoResponse;
import com.zephyra.genesis.dto.FacturaResponse;
import com.zephyra.genesis.dto.RemitoItemRequest;
import com.zephyra.genesis.dto.RemitoRequest;
import com.zephyra.genesis.dto.RemitoResponse;
import com.zephyra.genesis.entity.DetalleFactura;
import com.zephyra.genesis.entity.FacturaEntity;
import com.zephyra.genesis.entity.FacturaNormalEntity;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.RemitoEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.FacturaRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import com.zephyra.genesis.repository.RemitoRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final RemitoRepository remitoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public FacturaService(
            FacturaRepository facturaRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            RemitoRepository remitoRepository,
            UsuarioRepository usuarioRepository,
            EntityManager entityManager) {
        this.facturaRepository = facturaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.remitoRepository = remitoRepository;
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
        FacturaNormalEntity factura = new FacturaNormalEntity();
        factura.setFechaCreacion(new Date());
        factura.setTipoMoneda(request.tipoMoneda());
        factura.setFechaEmision(null);
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
        FacturaNormalEntity facturaGuardada = facturaRepository.save(factura);
        entityManager.flush();
        entityManager.refresh(facturaGuardada);
        return toResponse(facturaGuardada);
    }

    @Transactional
    public void actualizarPreciosCompraDesde(Date fechaInicio) {
        if (fechaInicio == null) {
            return;
        }
        List<FacturaNormalEntity> facturas = facturaRepository.findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(fechaInicio);
        for (FacturaNormalEntity factura : facturas) {
            for (DetalleFactura detalle : factura.getDetallesFactura()) {
                ProductoEntity producto = detalle.getProducto();
                producto.setPrecioCompra(detalle.getPrecioCompra());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FacturaRemitoResponse> buscarParaRemito(Long usuarioId, String nroSerie, Long proveedorId, LocalDate fecha) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.RECEPCION) {
            throw new IllegalArgumentException("Solo usuarios admin o recepcion pueden buscar facturas para remito.");
        }

        if (nroSerie != null && !nroSerie.isBlank()) {
            List<FacturaNormalEntity> facturas = facturaRepository.findByNroSerieIgnoreCase(nroSerie.trim());
            return facturas.stream().map(this::toRemitoResponse).toList();
        }

        if (proveedorId == null || fecha == null) {
            throw new IllegalArgumentException("Para buscar por fecha debes indicar proveedor y fecha.");
        }

        List<FacturaNormalEntity> facturasProveedor = facturaRepository.findByProveedor_Id(proveedorId);
        return facturasProveedor.stream()
                .filter((factura) -> fecha.equals(fechaReferencia(factura)))
                .map(this::toRemitoResponse)
                .toList();
    }

    @Transactional
    public RemitoResponse emitirRemito(Long usuarioId, RemitoRequest request) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.RECEPCION) {
            throw new IllegalArgumentException("Solo usuarios admin o recepcion pueden emitir remitos.");
        }

        if (request == null || request.facturaId() == null || request.detalles() == null || request.detalles().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar factura y al menos un producto para emitir remito.");
        }

        FacturaNormalEntity factura = facturaRepository.findById(request.facturaId())
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada."));

        if (remitoRepository.existsByFacturaOrigen_Id(factura.getId())) {
            throw new IllegalArgumentException("La factura ya tiene un remito emitido.");
        }

        Map<Long, DetalleFactura> detallePorProducto = new HashMap<>();
        for (DetalleFactura detalleFactura : factura.getDetallesFactura()) {
            detallePorProducto.put(detalleFactura.getProducto().getId(), detalleFactura);
        }

        Set<Long> productosVistos = new HashSet<>();
        List<ProductoEntity> productosAActualizar = new ArrayList<>();
        int cantidadTotal = 0;

        for (RemitoItemRequest item : request.detalles()) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) {
                throw new IllegalArgumentException("Cada producto del remito debe tener productoId y cantidad mayor a 0.");
            }
            if (!productosVistos.add(item.productoId())) {
                throw new IllegalArgumentException("No se puede repetir un producto dentro del remito.");
            }

            DetalleFactura detalleFactura = detallePorProducto.get(item.productoId());
            if (detalleFactura == null) {
                throw new IllegalArgumentException("El producto " + item.productoId() + " no pertenece a la factura seleccionada.");
            }

            if (item.cantidad() > detalleFactura.getCantidad()) {
                throw new IllegalArgumentException("La cantidad para " + detalleFactura.getProducto().getDescripcion()
                        + " no puede superar lo registrado en la factura.");
            }

            ProductoEntity producto = detalleFactura.getProducto();
            producto.setStock(producto.getStock() - item.cantidad());
            productosAActualizar.add(producto);
            cantidadTotal += item.cantidad();
        }

        if (cantidadTotal <= 0) {
            throw new IllegalArgumentException("El remito debe tener al menos una unidad a emitir.");
        }

        for (ProductoEntity producto : productosAActualizar) {
            productoRepository.save(producto);
        }

        Date ahora = new Date();
        float montoTotalRemito = request.detalles().stream()
            .map((item) -> {
                DetalleFactura detalle = detallePorProducto.get(item.productoId());
                return item.cantidad() * detalle.getPrecioCompra();
            })
            .reduce(0f, Float::sum);

        RemitoEntity remito = new RemitoEntity();
        remito.setFechaCreacion(ahora);
        remito.setFechaEmision(ahora);
        remito.setTipoMoneda(factura.getTipoMoneda());
        remito.setMontoTotal(montoTotalRemito);
        remito.setNroSerie(null);
        remito.setProveedor(factura.getProveedor());
        remito.setUsuario(usuario);
        remito.setFacturaOrigen(factura);
        remito.setFechaEmisionRemito(ahora);
        RemitoEntity remitoGuardado = remitoRepository.save(remito);

        return new RemitoResponse(
            remitoGuardado.getId(),
            factura.getId(),
                factura.getNroFactura(),
                cantidadTotal);
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

    private FacturaRemitoResponse toRemitoResponse(FacturaEntity factura) {
        List<DetalleFacturaRemitoResponse> detalles = factura.getDetallesFactura().stream()
                .map((detalle) -> new DetalleFacturaRemitoResponse(
                        detalle.getProducto().getId(),
                        detalle.getProducto().getDescripcion(),
                        detalle.getCantidad(),
                        detalle.getPrecioCompra()))
                .toList();

        return new FacturaRemitoResponse(
                factura.getId(),
                factura.getNroFactura(),
                factura.getNroSerie(),
                factura.getFechaEmision(),
                factura.getFechaCreacion(),
                factura.getProveedor().getId(),
                factura.getProveedor().getRazonSocial(),
                detalles);
    }

    private LocalDate fechaReferencia(FacturaEntity factura) {
        Date fecha = factura.getFechaEmision() != null ? factura.getFechaEmision() : factura.getFechaCreacion();
        if (fecha == null) {
            return null;
        }
        return Instant.ofEpochMilli(fecha.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
