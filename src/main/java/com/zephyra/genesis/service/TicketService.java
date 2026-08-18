package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.TicketDetalleResponse;
import com.zephyra.genesis.dto.DetalleTicketKeyRequest;
import com.zephyra.genesis.dto.DevolucionRequest;
import com.zephyra.genesis.dto.TicketItemRequest;
import com.zephyra.genesis.dto.TicketRequest;
import com.zephyra.genesis.dto.TicketResponse;
import com.zephyra.genesis.entity.ClienteEntity;
import com.zephyra.genesis.entity.ClienteComunEntity;
import com.zephyra.genesis.entity.DetalleTicket;
import com.zephyra.genesis.entity.DetalleTicketId;
import com.zephyra.genesis.entity.FORMA_DE_PAGO;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.TIPO_MONEDA;
import com.zephyra.genesis.repository.DetalleTicketRepository;
import com.zephyra.genesis.entity.TicketEntity;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.ClienteRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.TicketRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final MonedasService monedasService;

    public TicketService(
            TicketRepository ticketRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository,
            DetalleTicketRepository detalleTicketRepository,
            PasswordEncoder passwordEncoder,
            MonedasService monedasService) {
        this.ticketRepository = ticketRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.detalleTicketRepository = detalleTicketRepository;
        this.passwordEncoder = passwordEncoder;
        this.monedasService = monedasService;
    }

    @Transactional
    public TicketResponse crear(TicketRequest request, Long usuarioId) {
        validarDetalle(request.detalleTickets());
        ClienteEntity cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (Long.valueOf(1L).equals(request.clienteId()) && !(cliente instanceof ClienteComunEntity)) {
            throw new IllegalArgumentException("El id 1 debe corresponder a un ClienteComun para usar consumidor final.");
        }
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        TicketEntity ticket = crearTicket(
                cliente,
                usuario,
                request.formaDePago(),
                request.tipoMoneda(),
                request.montoPagado(),
                request.egreso(),
                request.egresosDescripcion(),
                request.detalleTickets(),
                false,
                true,
                false);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse crearDevolucionPorCaja(TicketRequest request, Long usuarioId) {
        validarDetalle(request.detalleTickets());
        ClienteEntity cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (Long.valueOf(1L).equals(request.clienteId()) && !(cliente instanceof ClienteComunEntity)) {
            throw new IllegalArgumentException("El id 1 debe corresponder a un ClienteComun para usar consumidor final.");
        }

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.CAJERO) {
            throw new IllegalArgumentException("Solo admin o cajero pueden registrar una devolución por caja.");
        }

        TicketEntity ticket = crearTicket(
                cliente,
                usuario,
                request.formaDePago(),
                TIPO_MONEDA.UYU,
                0f,
                false,
                null,
                request.detalleTickets(),
                true,
                false,
                true);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse devolver(Long ticketId, DevolucionRequest request, Long usuarioId) {
        TicketEntity ticketOriginal = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
        if (ticketOriginal.isDevolucion()) {
            throw new IllegalArgumentException("No se puede devolver un ticket que ya corresponde a una devolución.");
        }
        if (ticketOriginal.isDevolucionRealizada()) {
            throw new IllegalArgumentException("Este ticket ya tiene una devolución realizada.");
        }

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        ClienteEntity cliente = ticketOriginal.getCliente();
        if (cliente == null) {
            throw new IllegalArgumentException("El ticket original no tiene un cliente asociado.");
        }

        List<TicketItemRequest> detalles = ticketOriginal.getDetalleTickets().stream()
                .map(detalle -> new TicketItemRequest(
                        detalle.getProducto().getId(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario()))
                .toList();
        validarDetalle(detalles);

        TicketEntity devolucion = crearTicket(
                cliente,
                usuario,
                request.formaDePago(),
                request.tipoMoneda(),
                request.montoPagado(),
                false,
                null,
                detalles,
                true,
                false,
                false);

            ticketOriginal.setDevolucionRealizada(true);
            ticketRepository.save(ticketOriginal);
        return toResponse(devolucion);
    }

    @Transactional(readOnly = true)
    public void autorizarDevolucion(Integer cedula, String contraseña) {
        if (cedula == null || contraseña == null || contraseña.isBlank()) {
            throw new IllegalArgumentException("La cédula y la contraseña del administrador son obligatorias.");
        }

        UsuarioEntity administrador = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Autorización denegada."));
        if (administrador.getRol() != ROL.ADMIN || !passwordEncoder.matches(contraseña, administrador.getPassword())) {
            throw new IllegalArgumentException("Autorización denegada.");
        }
    }

    private TicketEntity crearTicket(
            ClienteEntity cliente,
            UsuarioEntity usuario,
            FORMA_DE_PAGO formaDePago,
            TIPO_MONEDA tipoMoneda,
            Float montoPagado,
            Boolean egreso,
            String egresosDescripcion,
            List<TicketItemRequest> items,
            boolean devolucion,
            boolean descontarStock,
            boolean forzarMontosPagoEnCero) {
        if (formaDePago == null) {
            throw new IllegalArgumentException("La forma de pago es obligatoria.");
        }

        TicketEntity ticket = new TicketEntity();
        ticket.setFechaCreacion(new Date());
        ticket.setFormaDePago(formaDePago);
        ticket.setUsuario(usuario);
        ticket.setCliente(cliente);
        ticket.setDevolucion(devolucion);
        ticket.setEgreso(Boolean.TRUE.equals(egreso));
        ticket.setEgresosDescripcion(ticket.isEgreso() ? egresosDescripcion : null);

        List<DetalleTicket> detalles = new ArrayList<>();
        Set<Long> productosIncluidos = new HashSet<>();
        float total = 0;

        for (TicketItemRequest item : items) {
            if (item.productoId() == null || !productosIncluidos.add(item.productoId()) || item.cantidad() <= 0) {
                throw new IllegalArgumentException("Cada producto puede aparecer una sola vez por ticket.");
            }
            ProductoEntity producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            if (descontarStock && !producto.isActivo()) {
                throw new IllegalArgumentException("El producto " + producto.getDescripcion() + " no está activo.");
            }

            float precioUnitario = descontarStock ? producto.getPrecioVenta() : item.precioUnitario();
            DetalleTicket detalle = new DetalleTicket();
            detalle.setTicket(ticket);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalles.add(detalle);
            total += precioUnitario * item.cantidad();

            producto.setStock(producto.getStock() + (descontarStock ? -item.cantidad() : item.cantidad()));
            productoRepository.save(producto);
        }

        ticket.setDetalleTickets(detalles);
        ticket.setMontoTotal(devolucion ? -redondear(total) : redondear(total));
        asignarDatosDePago(ticket, formaDePago, tipoMoneda, montoPagado, total, devolucion, forzarMontosPagoEnCero);
        return ticketRepository.save(ticket);
    }

    private void validarDetalle(List<TicketItemRequest> detalleTickets) {
        if (detalleTickets == null || detalleTickets.isEmpty()) {
            throw new IllegalArgumentException("El ticket debe incluir al menos un producto.");
        }
    }

    private void asignarDatosDePago(
            TicketEntity ticket,
            FORMA_DE_PAGO formaDePago,
            TIPO_MONEDA tipoMoneda,
            Float montoPagado,
            float total,
            boolean devolucion,
            boolean forzarMontosPagoEnCero) {
        if (forzarMontosPagoEnCero) {
            ticket.setTipoMoneda(TIPO_MONEDA.UYU);
            ticket.setMontoPagado(0f);
            ticket.setCambioEntregado(0f);
            return;
        }
        if (formaDePago != FORMA_DE_PAGO.EFECTIVO) {
            ticket.setTipoMoneda(TIPO_MONEDA.UYU);
            ticket.setMontoPagado(redondear(total));
            ticket.setCambioEntregado(null);
            return;
        }
        if (devolucion) {
            ticket.setTipoMoneda(TIPO_MONEDA.UYU);
            ticket.setMontoPagado(null);
            ticket.setCambioEntregado(null);
            return;
        }
        if (tipoMoneda == null || montoPagado == null || montoPagado < 0) {
            throw new IllegalArgumentException("Para efectivo se requiere moneda y monto pagado.");
        }

        double totalEnMoneda = total;
        double factorAuyu = 1;
        if (tipoMoneda == TIPO_MONEDA.USD) {
            factorAuyu = monedasService.obtenerValorUsdUYU();
            totalEnMoneda = total / factorAuyu;
        }
        if (montoPagado + 0.0001 < totalEnMoneda) {
            throw new IllegalArgumentException("El monto recibido no cubre el total del ticket.");
        }

        float cambioEnUyu = redondear((float) ((montoPagado - totalEnMoneda) * factorAuyu));
        ticket.setTipoMoneda(tipoMoneda);
        ticket.setMontoPagado(redondear(montoPagado));
        ticket.setCambioEntregado(cambioEnUyu);
    }

    private float redondear(float monto) {
        return Math.round(monto * 100f) / 100f;
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
                ticket.getTipoMoneda() != null ? ticket.getTipoMoneda().name() : null,
                ticket.getMontoPagado(),
                ticket.getCambioEntregado(),
                ticket.isDevolucion(),
                ticket.isDevolucionRealizada(),
                ticket.isEgreso(),
                ticket.getEgresosDescripcion(),
                detalleResponses
        );
    }
}
