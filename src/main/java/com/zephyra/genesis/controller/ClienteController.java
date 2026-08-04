package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.ClienteRequest;
import com.zephyra.genesis.dto.ClienteResponse;
import com.zephyra.genesis.service.ClienteService;
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
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.crear(request);
        return ResponseEntity.status(201).body(Map.of("mensaje", "Cliente creado correctamente", "cliente", cliente));
    }

    @GetMapping("/buscar/{valor}")
    public ResponseEntity<?> buscar(@PathVariable String valor) {
        try {
            if (valor.matches("\\d+@.*") || valor.contains("@")) {
                return ResponseEntity.ok(clienteService.buscarPorEmail(valor));
            }
            List<ClienteResponse> clientes = clienteService.buscarPorNombre(valor);
            if (clientes.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("mensaje", "Cliente no encontrado"));
            }
            return ResponseEntity.ok(clientes.get(0));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("mensaje", ex.getMessage()));
        }
    }

    @GetMapping("/buscar/denominacion/{valor}")
    public ResponseEntity<?> buscarDenominacion(@PathVariable String valor) {
        return buscar(valor);
    }

    @PutMapping("/{valor}")
    public ResponseEntity<?> actualizar(@PathVariable String valor, @RequestBody ClienteRequest request) {
        try {
            ClienteResponse cliente = clienteService.actualizarPorEmail(valor, request);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Cliente actualizado correctamente", "cliente", cliente));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @DeleteMapping("/desactivar/{valor}")
    public ResponseEntity<?> desactivar(@PathVariable String valor) {
        try {
            clienteService.desactivarPorEmail(valor);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Cliente eliminado correctamente"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }
}
