package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.TicketDetalleResponse;
import com.zephyra.genesis.dto.DetalleTicketKeyRequest;
import com.zephyra.genesis.dto.TicketItemRequest;
import com.zephyra.genesis.dto.TicketRequest;
import com.zephyra.genesis.dto.TicketResponse;
import com.zephyra.genesis.entity.ClienteEntity;
import com.zephyra.genesis.entity.DetalleTicket;
import com.zephyra.genesis.entity.DetalleTicketId;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.repository.DetalleTicketRepository;
import com.zephyra.genesis.entity.TicketEntity;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.ClienteRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.TicketRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final DetalleTicketRepository detalleTicketRepository;

    public TicketService(TicketRepository ticketRepository, ClienteRepository clienteRepository, UsuarioRepository usuarioRepository, ProductoRepository productoRepository, DetalleTicketRepository detalleTicketRepository) {
        this.ticketRepository = ticketRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.detalleTicketRepository = detalleTicketRepository;
    }

    @Transactional
    public TicketResponse crear(TicketRequest request, Long usuarioId) {
        ClienteEntity cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        TicketEntity ticket = new TicketEntity();
        ticket.setFechaCreacion(new Date());
        ticket.setFormaDePago(request.formaDePago());
        ticket.setUsuario(usuario);
        ticket.setCliente(cliente);

        List<DetalleTicket> detalles = new ArrayList<>();
        Set<Long> productosIncluidos = new HashSet<>();
        float total = 0;

        for (TicketItemRequest item : request.detalleTickets()) {
            if (item.productoId() == null || !productosIncluidos.add(item.productoId())) {
                throw new IllegalArgumentException("Cada producto puede aparecer una sola vez por ticket.");
            }
            ProductoEntity producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            DetalleTicket detalle = new DetalleTicket();
            detalle.setTicket(ticket);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(item.precioUnitario());
            detalles.add(detalle);
            total += item.precioUnitario() * item.cantidad();
        }

        ticket.setDetalleTickets(detalles);
        ticket.setMontoTotal(total);
        TicketEntity saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TicketResponse buscarPorId(Long id) {
        return ticketRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado o inactivo"));
    }

    @Transactional
    public void desactivar(Long ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        if (ticket.getDetalleTickets() != null) {
            for (DetalleTicket detalle : ticket.getDetalleTickets()) {
                ProductoEntity producto = detalle.getProducto();
                if (producto != null) {
                    producto.setStock(producto.getStock() + detalle.getCantidad());
                    productoRepository.save(producto);
                }
            }
        }
        ticketRepository.delete(ticket);
    }

    @Transactional
    public void eliminarArticulos(List<DetalleTicketKeyRequest> detallesKeys) {
        List<DetalleTicketId> detalleIds = detallesKeys.stream()
                .map(detalle -> new DetalleTicketId(detalle.ticketId(), detalle.productoId()))
                .toList();
        List<DetalleTicket> detalles = detalleTicketRepository.findAllById(detalleIds);
        if (detalles.isEmpty() || detalles.size() != detalleIds.size()) {
            throw new IllegalArgumentException("No se encontraron líneas activas para las claves indicadas.");
        }

        for (DetalleTicket detalle : detalles) {
            ProductoEntity producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);

            TicketEntity ticket = detalle.getTicket();
            if (ticket != null && ticket.getDetalleTickets() != null) {
                ticket.getDetalleTickets().removeIf(d -> detalle.getId().equals(d.getId()));
                ticketRepository.save(ticket);
            }
        }

        detalleTicketRepository.deleteAll(detalles);
    }

    private TicketResponse toResponse(TicketEntity ticket) {
        List<TicketDetalleResponse> detalleResponses = ticket.getDetalleTickets() == null ? List.of() : ticket.getDetalleTickets().stream()
                .map(detalle -> new TicketDetalleResponse(
                detalle.getTicket() != null ? detalle.getTicket().getId() : null,
                        detalle.getProducto() != null ? detalle.getProducto().getId() : null,
                        detalle.getProducto() != null ? detalle.getProducto().getDescripcion() : null,
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getPrecioUnitario() * detalle.getCantidad()))
                .toList();

        return new TicketResponse(
                ticket.getId(),
                ticket.getFechaCreacion(),
                ticket.getFormaDePago() != null ? ticket.getFormaDePago().name().toLowerCase() : null,
                ticket.getMontoTotal(),
                ticket.getUsuario() != null ? ticket.getUsuario().getId() : null,
                ticket.getUsuario() != null ? ticket.getUsuario().getName() : null,
                ticket.getCliente() != null ? ticket.getCliente().getId() : null,
                ticket.getCliente() != null ? ticket.getCliente().getName() : null,
                detalleResponses
        );
    }
}
