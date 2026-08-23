package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.AutorizacionDevolucionRequest;
import com.zephyra.genesis.dto.DetalleTicketKeyRequest;
import com.zephyra.genesis.dto.DevolucionRequest;
import com.zephyra.genesis.dto.EgresoRequest;
import com.zephyra.genesis.dto.TicketRequest;
import com.zephyra.genesis.dto.TicketResponse;
import com.zephyra.genesis.service.AuthService;
import com.zephyra.genesis.service.TicketService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final AuthService authService;

    public TicketController(TicketService ticketService, AuthService authService) {
        this.ticketService = ticketService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TicketRequest request, HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        TicketResponse ticket = ticketService.crear(request, usuarioId);
        return ResponseEntity.status(201).body(Map.of("ok", true, "mensaje", "Ticket creado con éxito", "ticket_id", ticket.id()));
    }

    @PostMapping("/devolucion-por-caja")
    public ResponseEntity<?> crearDevolucionPorCaja(@RequestBody TicketRequest request, HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        TicketResponse ticket = ticketService.crearDevolucionPorCaja(request, usuarioId);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "mensaje", "Devolución por caja registrada con éxito",
                "ticket_id", ticket.id()));
    }

    @PostMapping("/egresos")
    public ResponseEntity<?> crearEgreso(@RequestBody EgresoRequest request, HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        TicketResponse ticket = ticketService.crearEgreso(request, usuarioId);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "mensaje", "Egreso registrado con éxito",
                "ticket_id", ticket.id()));
    }

    @PostMapping("/devoluciones/autorizacion")
    public ResponseEntity<?> autorizarDevolucion(
            @RequestBody AutorizacionDevolucionRequest request,
            HttpServletRequest httpServletRequest) {
        obtenerUsuarioIdDesdeCookie(httpServletRequest);
        ticketService.autorizarDevolucion(request.cedula(), request.contraseña());
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Autorización aprobada."));
    }

    @PostMapping("/{ticketId}/devolucion")
    public ResponseEntity<?> devolver(
            @PathVariable Long ticketId,
            @RequestBody DevolucionRequest request,
            HttpServletRequest httpServletRequest) {
        Long usuarioId = obtenerUsuarioIdDesdeCookie(httpServletRequest);
        TicketResponse ticket = ticketService.devolver(ticketId, request, usuarioId);
        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("mensaje", "Devolución registrada y stock repuesto.");
        response.put("ticket_id", ticket.id());
        response.put("cambio_entregado", ticket.cambioEntregado());
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/desactivar")
    public ResponseEntity<?> desactivar(@RequestBody Map<String, Long> body) {
        ticketService.desactivar(body.get("ticket_id"));
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Ticket desactivado con éxito y stock devuelto."));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<?> buscar(@PathVariable Long ticketId) {
        try {
            TicketResponse ticket = ticketService.buscarPorId(ticketId);
            return ResponseEntity.ok(Map.of("ok", true, "ticket", ticket, "productos", ticket.detalleTickets()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }

    @PostMapping("/eliminar-articulos")
    public ResponseEntity<?> eliminarArticulos(@RequestBody Map<String, List<DetalleTicketKeyRequest>> body) {
        ticketService.eliminarArticulos(body.getOrDefault("detalles", List.of()));
        return ResponseEntity.ok(Map.of("success", true, "mensaje", "Artículos eliminados correctamente y stock actualizado."));
    }

    private Long obtenerUsuarioIdDesdeCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        for (Cookie cookie : request.getCookies()) {
            if ("token".equals(cookie.getName())) {
                String token = cookie.getValue();
                return authService.validarToken(token).id();
            }
        }

        throw new IllegalArgumentException("Usuario no autenticado");
    }
}
