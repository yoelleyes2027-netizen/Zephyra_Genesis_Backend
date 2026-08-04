package com.zephyra.genesis.config;

import com.zephyra.genesis.service.JwtService;
import com.zephyra.genesis.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TOKEN_COOKIE = "token";
    private final JwtService jwtService;

    public TenantContextFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (shouldUseMaster(request)) {
                TenantContextHolder.setMaster();
            } else {
                applyTenantFromToken(request);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean shouldUseMaster(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth")
                || path.startsWith("/api/admin-sistema")
                || path.startsWith("/health")
                || path.startsWith("/error")
                || path.startsWith("/api/monedas");
    }

    private void applyTenantFromToken(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || token.isBlank() || !jwtService.isValid(token)) {
            TenantContextHolder.setMaster();
            return;
        }

        var claims = jwtService.parseClaims(token);
        String rol = claims.get("rol", String.class);
        String tenantDatabase = claims.get("tenantDatabase", String.class);
        if (rol == null || rol.isBlank() || "admin_sistema".equalsIgnoreCase(rol) || tenantDatabase == null || tenantDatabase.isBlank()) {
            TenantContextHolder.setMaster();
            return;
        }

        TenantContextHolder.setTenantDatabase(tenantDatabase);
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}