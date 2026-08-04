package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.ProveedorRequest;
import com.zephyra.genesis.dto.ProveedorResponse;
import com.zephyra.genesis.service.ProveedorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(Map.of("ok", true, "data", proveedorService.listar()));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProveedorRequest request) {
        ProveedorResponse proveedor = proveedorService.crear(request);
        return ResponseEntity.status(201).body(Map.of("mensaje", "Proveedor creado correctamente", "proveedor", proveedor));
    }

    @GetMapping("/buscar/{valor}")
    public ResponseEntity<?> buscar(@PathVariable String valor) {
        try {
            return ResponseEntity.ok(Map.of("ok", true, "data", proveedorService.buscarPorDocumento(valor)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @GetMapping("/buscar/denominacion/{valor}")
    public ResponseEntity<?> buscarDenominacion(@PathVariable String valor) {
        try {
            return ResponseEntity.ok(Map.of("ok", true, "data", proveedorService.buscarPorDenominacion(valor)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @PutMapping("/{valor}")
    public ResponseEntity<?> actualizar(@PathVariable String valor, @RequestBody ProveedorRequest request) {
        try {
            ProveedorResponse proveedor = proveedorService.actualizarPorDocumento(valor, request);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Proveedor actualizado correctamente", "proveedor", proveedor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @DeleteMapping("/desactivar/{valor}")
    public ResponseEntity<?> desactivar(@PathVariable String valor) {
        try {
            proveedorService.desactivarPorDocumento(valor);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Proveedor eliminado correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }
}
