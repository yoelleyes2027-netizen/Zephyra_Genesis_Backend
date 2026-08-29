package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.ProductoRequest;
import com.zephyra.genesis.dto.ProductoResponse;
import com.zephyra.genesis.dto.StockUpdateRequest;
import com.zephyra.genesis.service.ProductoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProductoRequest request) {
        ProductoResponse producto = productoService.crear(request);
        return ResponseEntity.status(201).body(Map.of("ok", true, "mensaje", "Producto agregado correctamente", "producto", producto));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<?> buscarPorCodigo(@PathVariable int codigo) {
        try {
            return ResponseEntity.ok(productoService.buscarPorCodigo(codigo));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @GetMapping("/descripcion/{descripcion}")
    public ResponseEntity<?> buscarPorDescripcion(@PathVariable String descripcion) {
        return ResponseEntity.ok(Map.of("ok", true, "data", productoService.buscarPorDescripcion(descripcion)));
    }
    
    @GetMapping("/factura")
    public ResponseEntity<?> buscarParaFactura(
            @RequestParam Long proveedorId,
            @RequestParam(defaultValue = "") String busqueda) {
        return ResponseEntity.ok(Map.of("ok", true, "data", productoService.buscarPorProveedor(proveedorId, busqueda)));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<?> actualizar(@PathVariable int codigo, @RequestBody ProductoRequest request) {
        try {
            ProductoResponse producto = productoService.actualizarPorCodigo(codigo, request);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Producto actualizado correctamente", "producto", producto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<?> eliminar(@PathVariable int codigo) {
        try {
            productoService.eliminarPorCodigo(codigo);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Producto eliminado correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).body(Map.of("ok", false, "mensaje", "No se puede eliminar el producto porque tiene registros asociados."));
        }
    }

    @PutMapping("/actualizar-stock")
    public ResponseEntity<?> actualizarStock(@RequestBody StockUpdateRequest request) {
        try {
            productoService.actualizarStock(request.productos(), request.operacion());
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Stock actualizado correctamente."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(400).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }
}
