package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.FacturaRequest;
import com.zephyra.genesis.dto.FacturaRemitoResponse;
import com.zephyra.genesis.dto.FacturaResponse;
import com.zephyra.genesis.service.AuthService;
import com.zephyra.genesis.service.FacturaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;
    private final AuthService authService;

    public FacturaController(FacturaService facturaService, AuthService authService) {
        this.facturaService = facturaService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody FacturaRequest request, HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        FacturaResponse factura = facturaService.crear(request, usuarioId);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "mensaje", "Factura cargada correctamente.",
                "factura", factura));
    }

    @GetMapping("/buscar-remito")
    public ResponseEntity<?> buscarParaRemito(
            @RequestParam(required = false) String nroSerie,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        List<FacturaRemitoResponse> facturas = facturaService.buscarParaRemito(usuarioId, nroSerie, proveedorId, fecha);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensaje", "Facturas encontradas.",
                "data", facturas));
    }

    private Long obtenerUsuarioIdDesdeCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        for (Cookie cookie : request.getCookies()) {
            if ("token".equals(cookie.getName())) {
                return authService.validarToken(cookie.getValue()).id();
            }
        }
        throw new IllegalArgumentException("Usuario no autenticado");
    }
}
