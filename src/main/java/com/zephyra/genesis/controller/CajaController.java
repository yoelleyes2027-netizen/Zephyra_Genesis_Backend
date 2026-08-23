package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.CerrarCajaRequest;
import com.zephyra.genesis.entity.CajaDiariaEntity;
import com.zephyra.genesis.entity.CajaGlobalEntity;
import com.zephyra.genesis.service.AuthService;
import com.zephyra.genesis.service.CajaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private static final String TOKEN_COOKIE = "token";

    private final CajaService cajaService;
    private final AuthService authService;

    public CajaController(CajaService cajaService, AuthService authService) {
        this.cajaService = cajaService;
        this.authService = authService;
    }

    @GetMapping("/validar-acceso")
    public ResponseEntity<?> validarAccesoCajas(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        Long usuarioId = authService.validarToken(token).id();
        cajaService.validarAccesoCajas(usuarioId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/estado-apertura")
    public ResponseEntity<?> estadoApertura(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        Long usuarioId = authService.validarToken(token).id();
        boolean abierta = cajaService.cajaAbiertaParaUsuario(usuarioId);
        return ResponseEntity.ok(Map.of("ok", true, "abierta", abierta));
    }

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        Long usuarioId = authService.validarToken(token).id();
        CajaDiariaEntity cajaDiaria = cajaService.abrirCaja(usuarioId);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensaje", "Caja abierta correctamente",
                "caja_diaria_id", cajaDiaria.getId(),
                "fecha_inicio", cajaDiaria.getFechaInicio()));
    }

    @PostMapping("/iniciar-dia")
    public ResponseEntity<?> iniciarDia(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        Long usuarioId = authService.validarToken(token).id();
        CajaGlobalEntity cajaGlobal = cajaService.iniciarDia(usuarioId);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensaje", "Dia iniciado correctamente",
                "caja_global_id", cajaGlobal.getId(),
                "cotizacion_usd_uyu", cajaGlobal.getCotizacionUsdUyuInicio()));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestBody CerrarCajaRequest request, HttpServletRequest httpRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpRequest);
        CajaDiariaEntity caja = cajaService.cerrarCaja(usuarioId, request);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensaje", "Caja cerrada correctamente",
                "caja_diaria_id", caja.getId(),
                "fecha_cierre", caja.getFechaCierre()));
    }

    @PostMapping("/cerrar-dia")
    public ResponseEntity<?> cerrarDia(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        Long usuarioId = authService.validarToken(token).id();
        CajaGlobalEntity cajaGlobal = cajaService.cerrarDia(usuarioId);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "mensaje", "Dia cerrado correctamente",
                "caja_global_id", cajaGlobal.getId(),
                "fecha_cierre", cajaGlobal.getFechaCierre()));
    }

    private Long obtenerUsuarioIdDesdeCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        for (Cookie cookie : request.getCookies()) {
            if (TOKEN_COOKIE.equals(cookie.getName())) {
                return authService.validarToken(cookie.getValue()).id();
            }
        }

        throw new IllegalArgumentException("Usuario no autenticado");
    }
}
