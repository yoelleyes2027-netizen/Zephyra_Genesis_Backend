package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.EmpresaRequest;
import com.zephyra.genesis.dto.EmpresaResponse;
import com.zephyra.genesis.service.EmpresaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar() {
        return ResponseEntity.ok(empresaService.listar());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody EmpresaRequest request) {
        EmpresaResponse empresa = empresaService.crear(request);
        return ResponseEntity.status(201).body(Map.of("ok", true, "mensaje", "Empresa creada correctamente", "empresa", empresa));
    }

    @GetMapping("/buscar/{valor}")
    public ResponseEntity<?> buscar(@PathVariable String valor) {
        try {
            return ResponseEntity.ok(Map.of("ok", true, "data", empresaService.buscarPorDocumento(valor)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @GetMapping("/buscar/denominacion/{valor}")
    public ResponseEntity<?> buscarDenominacion(@PathVariable String valor) {
        try {
            return ResponseEntity.ok(Map.of("ok", true, "data", empresaService.buscarPorRazonSocial(valor)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @PutMapping("/{valor}")
    public ResponseEntity<?> actualizar(@PathVariable String valor, @RequestBody EmpresaRequest request) {
        try {
            EmpresaResponse empresa = empresaService.actualizarPorDocumento(valor, request);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Empresa actualizada correctamente", "empresa", empresa));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @DeleteMapping("/desactivar/{valor}")
    public ResponseEntity<?> desactivar(@PathVariable String valor) {
        try {
            empresaService.eliminarPorDocumento(valor);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Empresa eliminada correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }
}