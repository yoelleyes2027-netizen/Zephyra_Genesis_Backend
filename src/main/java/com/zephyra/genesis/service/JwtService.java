package com.zephyra.genesis.service;

import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.dto.AuthUserResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret:change-me-change-me-change-me-change-me}") String secret,
            @Value("${app.jwt.expiration-minutes:10080}") long expirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UsuarioEntity usuario) {
        return generateToken(
                usuario.getId(),
                usuario.getCedula(),
                usuario.getName(),
                Optional.ofNullable(usuario.getRol()).map(Enum::name).map(String::toLowerCase).orElse(""),
                usuario.getTenantDatabase());
    }

    public String generateToken(AuthUserResponse user) {
        return generateToken(user.id(), user.cedula(), user.nombre(), user.rol(), user.tenantDatabase());
    }

    public String generateToken(Long id, int cedula, String nombre, String rol, String tenantDatabase) {
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("cedula", cedula)
                .claim("rol", rol != null ? rol : "")
                .claim("nombre", nombre)
                .claim("tenantDatabase", tenantDatabase)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
