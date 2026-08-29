package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.ProductoRequest;
import com.zephyra.genesis.dto.ProductoResponse;
import com.zephyra.genesis.dto.StockItemRequest;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final EtiquetaService etiquetaService;

    public ProductoService(ProductoRepository productoRepository, ProveedorRepository proveedorRepository, EtiquetaService etiquetaService) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.etiquetaService = etiquetaService;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findByActivoTrueOrderByDescripcionAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorCodigo(int codigo) {
        return productoRepository.findByCodigoDeBarras(codigo)
                .filter(ProductoEntity::isActivo)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarPorDescripcion(String descripcion) {
        return productoRepository.findByActivoTrueAndDescripcionContainingIgnoreCaseOrderByDescripcionAsc(descripcion).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarPorProveedor(Long proveedorId, String busqueda) {
        if (proveedorId == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio.");
        }
        String textoBusqueda = busqueda == null ? "" : busqueda.trim();
        return productoRepository.buscarActivosPorProveedor(proveedorId, textoBusqueda).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        validarDuplicados(null, request);
        ProveedorEntity proveedor = resolverProveedor(request);
        ProductoEntity producto = new ProductoEntity(
                request.codigoDeBarras(),
                true,
                null,
                request.stock(),
                request.unidadDeMedida(),
                request.precioCompra(),
                request.descripcion(),
                request.precioVenta(),
                request.etiqueta(),
                proveedor
        );
        ProductoResponse response = toResponse(productoRepository.save(producto));
        etiquetaService.registrarDesdeProducto(producto.getEtiqueta());
        return response;
    }

    @Transactional
    public ProductoResponse actualizarPorCodigo(int codigo, ProductoRequest request) {
        ProductoEntity producto = productoRepository.findByCodigoDeBarras(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        validarDuplicados(producto, request);
        ProveedorEntity proveedor = resolverProveedor(request);
        producto.setDescripcion(request.descripcion());
        producto.setPrecioVenta(request.precioVenta());
        producto.setPrecioCompra(request.precioCompra());
        producto.setStock(request.stock());
        producto.setUnidadDeMedida(request.unidadDeMedida());
        producto.setEtiqueta(request.etiqueta());
        producto.setproveedorId(proveedor);
        producto.setFechaUltimoIngreso(new java.util.Date());
        ProductoResponse response = toResponse(productoRepository.save(producto));
        etiquetaService.registrarDesdeProducto(producto.getEtiqueta());
        return response;
    }

    private ProveedorEntity resolverProveedor(ProductoRequest request) {
        String numeroDocumento = request.proveedorNumeroDocumento() != null ? request.proveedorNumeroDocumento().trim() : "";
        if (!numeroDocumento.isBlank()) {
            return proveedorRepository.findByNumeroDocumentoIgnoreCase(numeroDocumento)
                    .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        }

        throw new IllegalArgumentException("El número de documento del proveedor es obligatorio.");
    }

    private void validarDuplicados(ProductoEntity actual, ProductoRequest request) {
        if (productoRepository.existsByCodigoDeBarras(request.codigoDeBarras())
                && (actual == null || actual.getCodigoDeBarras() != request.codigoDeBarras())) {
            throw new IllegalArgumentException("Ya existe un producto con ese código de barras.");
        }

        String descripcion = request.descripcion() != null ? request.descripcion().trim() : "";
        if (!descripcion.isBlank()) {
            boolean descripcionEnUso = productoRepository.existsByDescripcionIgnoreCase(descripcion)
                    && (actual == null || actual.getDescripcion() == null || !descripcion.equalsIgnoreCase(actual.getDescripcion()));
            if (descripcionEnUso) {
                throw new IllegalArgumentException("Ya existe un producto con esa descripción.");
            }
        }
    }

    @Transactional
    public void eliminarPorCodigo(int codigo) {
        ProductoEntity producto = productoRepository.findByCodigoDeBarras(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        productoRepository.delete(producto);
    }

    @Transactional
    public void actualizarStock(List<StockItemRequest> items, String operacion) {
        int factor = "devolucion".equalsIgnoreCase(operacion) ? 1 : -1;
        for (StockItemRequest item : items) {
            ProductoEntity producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            int nuevoStock = Math.max(producto.getStock() + (factor * item.cantidad()), 0);
            producto.setStock(nuevoStock);
            producto.setFechaUltimoIngreso(new java.util.Date());
            productoRepository.save(producto);
        }
    }

    private ProductoResponse toResponse(ProductoEntity producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigoDeBarras(),
                producto.getDescripcion(),
                producto.getPrecioVenta(),
                producto.getPrecioCompra(),
                producto.getStock(),
                producto.getUnidadDeMedida(),
                producto.getEtiqueta(),
                producto.getproveedorId() != null ? producto.getproveedorId().getNumeroDocumento() : null,
                producto.getproveedorId() != null ? producto.getproveedorId().getRazonSocial() : null,
                producto.isActivo()
        );
    }
}
