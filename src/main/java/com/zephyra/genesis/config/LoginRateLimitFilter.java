package com.zephyra.genesis.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Frena ataques de fuerza bruta contra el login contando fallos por IP en una ventana deslizante. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_FALLOS = 10;
    private static final long VENTANA_MS = 5L * 60L * 1000L;
    private static final int MAX_ENTRADAS = 10_000;

    private final Map<String, Intentos> intentosPorCliente = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!esLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cliente = resolverCliente(request);
        if (estaBloqueado(cliente)) {
            responderDemasiadosIntentos(response);
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            registrarFallo(cliente);
        } else if (response.getStatus() < HttpStatus.BAD_REQUEST.value()) {
            intentosPorCliente.remove(cliente);
        }
    }

    private boolean esLogin(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI());
    }

    private boolean estaBloqueado(String cliente) {
        Intentos intentos = intentosPorCliente.get(cliente);
        if (intentos == null) {
            return false;
        }
        if (intentos.expirado()) {
            intentosPorCliente.remove(cliente);
            return false;
        }
        return intentos.fallos.get() >= MAX_FALLOS;
    }

    private void registrarFallo(String cliente) {
        purgarSiEsNecesario();
        intentosPorCliente.compute(cliente, (clave, intentos) -> {
            if (intentos == null || intentos.expirado()) {
                return new Intentos();
            }
            intentos.fallos.incrementAndGet();
            return intentos;
        });
    }

    private void purgarSiEsNecesario() {
        if (intentosPorCliente.size() < MAX_ENTRADAS) {
            return;
        }
        intentosPorCliente.entrySet().removeIf(entry -> entry.getValue().expirado());
    }

    private void responderDemasiadosIntentos(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(VENTANA_MS / 1000L));
        response.getWriter().write(
                "{\"ok\":false,\"msg\":\"Demasiados intentos de inicio de sesión. Esperá unos minutos e intentá nuevamente.\"}");
    }

    private String resolverCliente(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] partes = forwardedFor.split(",");
            String ultima = partes[partes.length - 1].trim();
            if (!ultima.isBlank()) {
                return ultima;
            }
        }
        String remoto = request.getRemoteAddr();
        return remoto == null ? "desconocido" : remoto;
    }

    private static final class Intentos {
        private final AtomicInteger fallos = new AtomicInteger(1);
        private final long creadoEn = System.currentTimeMillis();

        private boolean expirado() {
            return System.currentTimeMillis() - creadoEn > VENTANA_MS;
        }
    }
}
