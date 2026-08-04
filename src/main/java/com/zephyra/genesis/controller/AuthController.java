package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.LoginRequest;
import com.zephyra.genesis.dto.RegisterRequest;
import com.zephyra.genesis.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TOKEN_COOKIE = "token";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, buildTokenCookie(result.token()).toString())
                .body(java.util.Map.of("ok", true, "user", result.user(), "msg", "Usuario registrado correctamente."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthService.AuthResult result = authService.login(request);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildTokenCookie(result.token()).toString())
                    .body(java.util.Map.of("ok", true, "user", result.user()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("ok", false, "msg", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("ok", false, "msg", ex.getMessage()));
        }
    }

    @GetMapping("/verificar-token")
    public ResponseEntity<?> verificarToken(@CookieValue(value = TOKEN_COOKIE, required = false) String token) {
        try {
            AuthUserResponse usuario = authService.validarToken(token);
            return ResponseEntity.ok(java.util.Map.of("ok", true, "usuario", usuario));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("ok", false, "msg", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(TOKEN_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build()
                .toString());
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    private ResponseCookie buildTokenCookie(String token) {
        return ResponseCookie.from(TOKEN_COOKIE, token)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }
}
