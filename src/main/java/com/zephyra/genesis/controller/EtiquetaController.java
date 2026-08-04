package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.EtiquetaRequest;
import com.zephyra.genesis.service.EtiquetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/etiquetas")
public class EtiquetaController {

    private final EtiquetaService etiquetaService;

    public EtiquetaController(EtiquetaService etiquetaService) {
        this.etiquetaService = etiquetaService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(etiquetaService.listar());
    }

    @PostMapping("/agregar")
    public ResponseEntity<?> agregar(@RequestBody EtiquetaRequest request) {
        var etiqueta = etiquetaService.agregar(request.nombre());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "msg", "Etiqueta agregada correctamente.",
                "etiqueta", etiqueta
        ));
    }
}
