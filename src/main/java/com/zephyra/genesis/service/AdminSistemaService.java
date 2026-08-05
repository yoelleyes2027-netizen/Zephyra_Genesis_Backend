package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.UsuarioAdminRequest;
import com.zephyra.genesis.dto.UsuarioAdminResponse;
import com.zephyra.genesis.entity.CajaDiariaEntity;
import com.zephyra.genesis.entity.DetalleTicket;
import com.zephyra.genesis.entity.EmpresaEntity;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.TicketEntity;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.CajaDiariaRepository;
import com.zephyra.genesis.repository.DetalleTicketRepository;
import com.zephyra.genesis.repository.EmpresaRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import com.zephyra.genesis.repository.TicketRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminSistemaService {

    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final TicketRepository ticketRepository;
    private final DetalleTicketRepository detalleTicketRepository;
    private final CajaDiariaRepository cajaDiariaRepository;
    private final TenantDatabaseProvisioningService tenantDatabaseProvisioningService;
    private final DataSource masterDataSource;
    private final PasswordEncoder passwordEncoder;

    public AdminSistemaService(
            UsuarioRepository usuarioRepository,
            ProveedorRepository proveedorRepository,
            EmpresaRepository empresaRepository,
            ProductoRepository productoRepository,
            TicketRepository ticketRepository,
            DetalleTicketRepository detalleTicketRepository,
            CajaDiariaRepository cajaDiariaRepository,
            TenantDatabaseProvisioningService tenantDatabaseProvisioningService,
            @org.springframework.beans.factory.annotation.Qualifier("masterDataSource") DataSource masterDataSource,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.proveedorRepository = proveedorRepository;
        this.empresaRepository = empresaRepository;
        this.productoRepository = productoRepository;
        this.ticketRepository = ticketRepository;
        this.detalleTicketRepository = detalleTicketRepository;
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.tenantDatabaseProvisioningService = tenantDatabaseProvisioningService;
        this.masterDataSource = masterDataSource;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioAdminResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::toUsuarioResponse).toList();
    }

    public List<String> listarBasesDeDatos() {
        try (Connection connection = masterDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT datname FROM pg_database WHERE datistemplate = false AND datallowconn = true AND datname NOT IN ('postgres', 'template0', 'template1', 'rdsadmin', 'zephyra_admins') ORDER BY datname")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<String> bases = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    bases.add(resultSet.getString("datname"));
                }
                return bases;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron listar las bases de datos de RDS.", ex);
        }
    }

    public UsuarioAdminResponse buscarUsuario(int cedula) {
        return usuarioRepository.findByCedula(cedula)
                .map(this::toUsuarioResponse)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    public UsuarioAdminResponse crearUsuario(UsuarioAdminRequest request) {
        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new IllegalArgumentException("Ya existe un usuario con esa cédula.");
        }
        if (request.contraseña() == null || request.contraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (request.tenantDatabase() == null || request.tenantDatabase().isBlank()) {
            throw new IllegalArgumentException("La base de datos asignada es obligatoria.");
        }
        tenantDatabaseProvisioningService.ensureTenantDatabase(request.tenantDatabase().trim());

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setName(request.nombre());
        usuario.setEmail(normalizeEmail(request.email(), request.cedula()));
        usuario.setTelefono(request.telefono() != null ? request.telefono() : 0);
        usuario.setCedula(request.cedula());
        usuario.setPassword(passwordEncoder.encode(request.contraseña()));
        usuario.setRol(parseRol(request.rol()));
        String tenantDatabase = normalizeTenantDatabase(request.tenantDatabase());
        usuario.setTenantDatabase(tenantDatabase);
        usuario.setFechaCreacion(new Date());
        usuario.setFotoPerfil(null);
        usuario.setFechaInicioDeDia(null);

        UsuarioEntity saved = usuarioRepository.save(usuario);
        syncToTenantDatabase(saved, null);
        return toUsuarioResponse(saved);
    }

    public UsuarioAdminResponse actualizarUsuario(int cedula, UsuarioAdminRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        usuario.setName(request.nombre());
        usuario.setEmail(normalizeEmail(request.email(), request.cedula()));
        usuario.setTelefono(request.telefono() != null ? request.telefono() : 0);
        usuario.setCedula(request.cedula());
        if (request.contraseña() != null && !request.contraseña().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.contraseña()));
        }
        usuario.setRol(parseRol(request.rol()));
        String previousTenantDatabase = usuario.getTenantDatabase();
        String tenantDatabase = normalizeTenantDatabase(request.tenantDatabase());
        usuario.setTenantDatabase(tenantDatabase);

        UsuarioEntity saved = usuarioRepository.save(usuario);
        syncToTenantDatabase(saved, previousTenantDatabase);

        return toUsuarioResponse(saved);
    }

    public void eliminarUsuario(int cedula) {
        UsuarioEntity usuario = usuarioRepository.findByCedula(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        tenantDatabaseProvisioningService.deleteUsuario(usuario.getTenantDatabase(), usuario.getId());
        usuarioRepository.delete(usuario);
    }

    public List<Map<String, Object>> listarProveedores() {
        return proveedorRepository.findAll().stream().map(this::toProveedorMap).toList();
    }

    public List<Map<String, Object>> listarEmpresas() {
        return empresaRepository.findAll().stream().map(this::toEmpresaMap).toList();
    }

    public List<Map<String, Object>> listarProductos() {
        return productoRepository.findAll().stream().map(this::toProductoMap).toList();
    }

    public List<Map<String, Object>> listarTickets() {
        return ticketRepository.findAll().stream().map(this::toTicketMap).toList();
    }

    public List<Map<String, Object>> listarDetallesTicket() {
        return detalleTicketRepository.findAll().stream().map(this::toDetalleTicketMap).toList();
    }

    public List<Map<String, Object>> listarCajasDiarias() {
        return cajaDiariaRepository.findAll().stream().map(this::toCajaDiariaMap).toList();
    }

    public Map<String, Object> soporte(String tabla) {
        String normalized = tabla == null ? "" : tabla.trim().toLowerCase();
        Map<String, Object> response = new HashMap<>();
        response.put("tabla", normalized);
        response.put("data", switch (normalized) {
            case "usuario", "usuarios" -> listarUsuarios();
            case "proveedor", "proveedores" -> listarProveedores();
            case "empresa", "empresas" -> listarEmpresas();
            case "producto", "productos" -> listarProductos();
            case "ticket", "tickets" -> listarTickets();
            case "detalle_ticket", "detalle-ticket", "detalleticket" -> listarDetallesTicket();
            case "caja_diaria", "caja-diaria", "cajadiaria" -> listarCajasDiarias();
            default -> throw new IllegalArgumentException("Tabla no soportada.");
        });
        return response;
    }

    private UsuarioAdminResponse toUsuarioResponse(UsuarioEntity usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getName(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getCedula(),
                usuario.getRol() != null ? usuario.getRol().name().toLowerCase() : null,
                usuario.getTenantDatabase(),
                usuario.getFechaCreacion()
        );
    }

    private Map<String, Object> toProveedorMap(ProveedorEntity proveedor) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", proveedor.getId());
        row.put("name", proveedor.getName());
        row.put("email", proveedor.getEmail());
        row.put("telefono", proveedor.getTelefono());
        row.put("numeroDocumento", proveedor.getNumeroDocumento());
        row.put("direccion", proveedor.getDireccion());
        row.put("razonSocial", proveedor.getRazonSocial());
        row.put("tipoDocumento", proveedor.getTipoDocumento() != null ? proveedor.getTipoDocumento().name() : null);
        row.put("fechaCreacion", proveedor.getFechaCreacion());
        return row;
    }

    private Map<String, Object> toEmpresaMap(EmpresaEntity empresa) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", empresa.getId());
        row.put("name", empresa.getName());
        row.put("email", empresa.getEmail());
        row.put("telefono", empresa.getTelefono());
        row.put("numeroDocumento", empresa.getNumeroDocumento());
        row.put("direccion", empresa.getDireccion());
        row.put("razonSocial", empresa.getRazonSocial());
        row.put("tipoDocumento", empresa.getTipoDocumento() != null ? empresa.getTipoDocumento().name() : null);
        row.put("fechaCreacion", empresa.getFechaCreacion());
        return row;
    }

    private Map<String, Object> toProductoMap(ProductoEntity producto) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", producto.getId());
        row.put("codigoDeBarras", producto.getCodigoDeBarras());
        row.put("descripcion", producto.getDescripcion());
        row.put("precioVenta", producto.getPrecioVenta());
        row.put("precioCompra", producto.getPrecioCompra());
        row.put("stock", producto.getStock());
        row.put("unidadDeMedida", producto.getUnidadDeMedida() != null ? producto.getUnidadDeMedida().name() : null);
        row.put("etiqueta", producto.getEtiqueta());
        row.put("proveedorId", producto.getproveedorId() != null ? producto.getproveedorId().getId() : null);
        row.put("proveedorNombre", producto.getproveedorId() != null ? producto.getproveedorId().getRazonSocial() : null);
        row.put("activo", producto.isActivo());
        row.put("fechaDeIngreso", producto.getFechaDeIngreso());
        row.put("fechaUltimoIngreso", producto.getFechaUltimoIngreso());
        return row;
    }

    private Map<String, Object> toTicketMap(TicketEntity ticket) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", ticket.getId());
        row.put("fechaCreacion", ticket.getFechaCreacion());
        row.put("formaDePago", ticket.getFormaDePago() != null ? ticket.getFormaDePago().name() : null);
        row.put("montoTotal", ticket.getMontoTotal());
        row.put("usuarioId", ticket.getUsuario() != null ? ticket.getUsuario().getId() : null);
        row.put("usuarioNombre", ticket.getUsuario() != null ? ticket.getUsuario().getName() : null);
        row.put("clienteId", ticket.getCliente() != null ? ticket.getCliente().getId() : null);
        row.put("clienteNombre", ticket.getCliente() != null ? ticket.getCliente().getName() : null);
        row.put("detalleCount", ticket.getDetalleTickets() != null ? ticket.getDetalleTickets().size() : 0);
        return row;
    }

    private Map<String, Object> toDetalleTicketMap(DetalleTicket detalle) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", detalle.getId());
        row.put("ticketId", detalle.getTicket() != null ? detalle.getTicket().getId() : null);
        row.put("productoId", detalle.getProducto() != null ? detalle.getProducto().getId() : null);
        row.put("productoDescripcion", detalle.getProducto() != null ? detalle.getProducto().getDescripcion() : null);
        row.put("cantidad", detalle.getCantidad());
        row.put("precioUnitario", detalle.getPrecioUnitario());
        return row;
    }

    private Map<String, Object> toCajaDiariaMap(CajaDiariaEntity caja) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", caja.getId());
        row.put("totalIngresos", caja.getTotalIngresos());
        row.put("totalEgresos", caja.getTotalEgresos());
        row.put("fechaCierre", caja.getFechaCierre());
        row.put("diferencia", caja.getDiferencia());
        row.put("posCalculado", caja.getPosCalculado());
        row.put("posDeclarado", caja.getPosDeclarado());
        row.put("efectivoCalculado", caja.getEfectivoCalculado());
        row.put("efectivoDeclarado", caja.getEfectivoDeclarado());
        row.put("usuarios", caja.getUsuarios() != null ? caja.getUsuarios().stream().map(UsuarioEntity::getName).toList() : List.of());
        return row;
    }

    private ROL parseRol(String rol) {
        if (rol == null || rol.isBlank()) {
            throw new IllegalArgumentException("Rol es obligatorio.");
        }
        ROL parsed = ROL.valueOf(rol.trim().toUpperCase());
        if (parsed == ROL.ADMIN_SISTEMA) {
            throw new IllegalArgumentException("No se puede crear un usuario con rol admin_sistema desde este panel.");
        }
        return parsed;
    }

    private String normalizeTenantDatabase(String tenantDatabase) {
        if (tenantDatabase == null || tenantDatabase.isBlank()) {
            throw new IllegalArgumentException("La base de datos asignada es obligatoria.");
        }
        return tenantDatabase.trim();
    }

    private void syncToTenantDatabase(UsuarioEntity usuario, String previousTenantDatabase) {
        String tenantDatabase = usuario.getTenantDatabase();
        if (tenantDatabase == null || tenantDatabase.isBlank() || isMasterDatabase(tenantDatabase)) {
            return;
        }

        tenantDatabaseProvisioningService.ensureTenantDatabase(tenantDatabase);
        tenantDatabaseProvisioningService.upsertUsuario(tenantDatabase, usuario);
        if (previousTenantDatabase != null && !previousTenantDatabase.isBlank() && !previousTenantDatabase.equals(tenantDatabase) && !isMasterDatabase(previousTenantDatabase)) {
            tenantDatabaseProvisioningService.deleteUsuario(previousTenantDatabase, usuario.getId());
        }
    }

    private boolean isMasterDatabase(String tenantDatabase) {
        if (tenantDatabase == null || tenantDatabase.isBlank()) {
            return false;
        }
        try (Connection connection = masterDataSource.getConnection()) {
            String masterDatabaseName = connection.getCatalog();
            return masterDatabaseName != null && masterDatabaseName.equalsIgnoreCase(tenantDatabase.trim());
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalizeEmail(String email, int cedula) {
        return email != null && !email.isBlank() ? email.trim() : cedula + "@zephyra.local";
    }
}