package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.MonedaResponse;
import com.zephyra.genesis.service.MonedasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/monedas")
public class MonedasController {

    private final MonedasService monedasService;

    public MonedasController(MonedasService monedasService) {
        this.monedasService = monedasService;
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<MonedaResponse> obtenerMoneda(@PathVariable String codigo) {
        if (!"USD".equalsIgnoreCase(codigo)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(monedasService.obtenerDolar());
    }

    @PostMapping("/actualizar-dolar")
    public ResponseEntity<?> actualizarDolar() {
        double cotizacion = monedasService.obtenerValorUsdUYUDesdeApi();
        return ResponseEntity.ok(Map.of(
                "ok", true,
            "valorUSD", cotizacion,
                "msg", "Dólar actualizado correctamente"
        ));
    }
}
