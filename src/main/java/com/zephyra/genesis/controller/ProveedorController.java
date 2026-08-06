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
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        ProveedorRequest request = toRequest(body);
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
    public ResponseEntity<?> actualizar(@PathVariable String valor, @RequestBody Map<String, Object> body) {
        try {
            ProveedorRequest request = toRequest(body);
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

    private ProveedorRequest toRequest(Map<String, Object> body) {
        return new ProveedorRequest(
                stringValue(body, "name"),
                stringValue(body, "email"),
                intValue(body, "telefono"),
                firstStringValue(body, "numeroDocumento", "numero_documento", "numerodocumento"),
                stringValue(body, "direccion"),
                firstStringValue(body, "razonSocial", "razon_social", "razonsocial"),
                firstStringValue(body, "tipoDocumento", "tipo_documento", "tipodocumento")
        );
    }

    private String firstStringValue(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            String value = stringValue(body, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private int intValue(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).trim().isBlank()) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }
}
