package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.UsuarioAdminRequest;
import com.zephyra.genesis.dto.UsuarioAdminResponse;
import com.zephyra.genesis.dto.ConsumidorFinalResponse;
import com.zephyra.genesis.dto.DetalleTicketKeyRequest;
import com.zephyra.genesis.entity.CajaDiariaEntity;
import com.zephyra.genesis.entity.ClienteEntity;
import com.zephyra.genesis.entity.DetalleTicket;
import com.zephyra.genesis.entity.EmpresaEntity;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.TicketEntity;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.ClienteRepository;
import com.zephyra.genesis.repository.CajaDiariaRepository;
import com.zephyra.genesis.repository.DetalleTicketRepository;
import com.zephyra.genesis.repository.EmpresaRepository;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import com.zephyra.genesis.repository.TicketRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import com.zephyra.genesis.config.TenantDataSourceFactory;
import com.zephyra.genesis.tenant.TenantContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Transactional
public class AdminSistemaService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpresaRepository empresaRepository;
    private final ProductoRepository productoRepository;
    private final TicketRepository ticketRepository;
    private final DetalleTicketRepository detalleTicketRepository;
    private final CajaDiariaRepository cajaDiariaRepository;
    private final TenantDatabaseProvisioningService tenantDatabaseProvisioningService;
    private final TenantDataSourceFactory tenantDataSourceFactory;
    private final DataSource masterDataSource;
    private final PasswordEncoder passwordEncoder;
    private final TicketService ticketService;

    public AdminSistemaService(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            ProveedorRepository proveedorRepository,
            EmpresaRepository empresaRepository,
            ProductoRepository productoRepository,
            TicketRepository ticketRepository,
            DetalleTicketRepository detalleTicketRepository,
            CajaDiariaRepository cajaDiariaRepository,
            TenantDatabaseProvisioningService tenantDatabaseProvisioningService,
            TenantDataSourceFactory tenantDataSourceFactory,
            @org.springframework.beans.factory.annotation.Qualifier("masterDataSource") DataSource masterDataSource,
            PasswordEncoder passwordEncoder,
            TicketService ticketService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.empresaRepository = empresaRepository;
        this.productoRepository = productoRepository;
        this.ticketRepository = ticketRepository;
        this.detalleTicketRepository = detalleTicketRepository;
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.tenantDatabaseProvisioningService = tenantDatabaseProvisioningService;
        this.tenantDataSourceFactory = tenantDataSourceFactory;
        this.masterDataSource = masterDataSource;
        this.passwordEncoder = passwordEncoder;
        this.ticketService = ticketService;
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

    public ConsumidorFinalResponse asegurarConsumidorFinal(String baseDatos) {
        String tenantDatabase = normalizeDatabaseName(baseDatos);
        tenantDatabaseProvisioningService.ensureTenantDatabase(tenantDatabase);
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);

        try (Connection connection = tenantDataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ConsumidorFinalResponse existente = buscarConsumidorFinal(connection);
                if (existente != null) {
                    connection.commit();
                    return existente;
                }

                insertarConsumidorFinal(connection);
                ajustarSecuenciaPersona(connection);
                connection.commit();
                return new ConsumidorFinalResponse(1L, "ConsumidorFinal", "consumidor.final@genesis.local", 0);
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar el consumidor final en la BDD seleccionada.", ex);
        }
    }

    private ConsumidorFinalResponse buscarConsumidorFinal(Connection connection) throws java.sql.SQLException {
        try (PreparedStatement persona = connection.prepareStatement(
                "SELECT name, email, telefono FROM persona WHERE id = 1 FOR UPDATE")) {
            try (ResultSet resultSet = persona.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                try (PreparedStatement clienteComun = connection.prepareStatement(
                        "SELECT 1 FROM cliente_comun WHERE id = 1")) {
                    try (ResultSet clienteComunResult = clienteComun.executeQuery()) {
                        if (!clienteComunResult.next()) {
                            throw new IllegalArgumentException(
                                    "La BDD ya usa el id 1 para otra persona. No se puede reemplazar con ConsumidorFinal.");
                        }
                    }
                }
                return new ConsumidorFinalResponse(
                        1L,
                        resultSet.getString("name"),
                        resultSet.getString("email"),
                        resultSet.getInt("telefono"));
            }
        }
    }

    private void insertarConsumidorFinal(Connection connection) throws java.sql.SQLException {
        try (PreparedStatement persona = connection.prepareStatement(
                    "INSERT INTO persona (id, name, email, telefono, fecha_creacion) VALUES (1, ?, ?, ?, CURRENT_TIMESTAMP)");
             PreparedStatement cliente = connection.prepareStatement("INSERT INTO cliente (id) VALUES (1)");
             PreparedStatement clienteComun = connection.prepareStatement("INSERT INTO cliente_comun (id) VALUES (1)")) {
            persona.setString(1, "ConsumidorFinal");
            persona.setString(2, "consumidor.final@genesis.local");
            persona.setInt(3, 0);
            persona.executeUpdate();
            cliente.executeUpdate();
            clienteComun.executeUpdate();
        }
    }

    private void ajustarSecuenciaPersona(Connection connection) throws java.sql.SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT setval(pg_get_serial_sequence('persona', 'id'), (SELECT MAX(id) FROM persona), true)");
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

    public List<String> listarTablas(String baseDatos) {
        String normalizedBaseDatos = normalizeDatabaseName(baseDatos);
        try (Connection connection = tenantDataSourceFactory.getTenantDataSource(normalizedBaseDatos).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), "public", "%", new String[] { "TABLE" })) {
                List<String> tablas = new ArrayList<>();
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    if (!isSystemTable(tableName)) {
                        tablas.add(tableName);
                    }
                }
                tablas.sort(String::compareToIgnoreCase);
                return tablas;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron listar las tablas de la base de datos seleccionada.", ex);
        }
    }

    public Map<String, Object> soporte(String baseDatos, String tabla) {
        String normalizedBase = normalizeDatabaseName(baseDatos);
        String normalized = tabla == null ? "" : tabla.trim().toLowerCase();
        Map<String, Object> response = new HashMap<>();
        response.put("baseDatos", normalizedBase);
        response.put("tabla", normalized);
        response.put("data", switch (normalized) {
            case "usuario", "usuarios" -> listarUsuariosDesdeBase(normalizedBase);
            case "cliente", "clientes" -> listarClientesDesdeBase(normalizedBase);
            case "proveedor", "proveedores" -> listarProveedoresDesdeBase(normalizedBase);
            case "empresa", "empresas" -> listarEmpresasDesdeBase(normalizedBase);
            case "producto", "productos" -> listarProductosDesdeBase(normalizedBase);
            case "ticket", "tickets" -> listarTicketsDesdeBase(normalizedBase);
            case "detalle_ticket", "detalle-ticket", "detalleticket" -> listarDetallesTicketDesdeBase(normalizedBase);
            case "caja_diaria", "caja-diaria", "cajadiaria" -> listarCajasDiariasDesdeBase(normalizedBase);
            default -> listarTablaGenerica(normalizedBase, normalized);
        });
        return response;
    }

    public Map<String, Object> editarSoporte(String baseDatos, String tabla, String clave, Map<String, Object> datos) {
        String normalizedBase = normalizeDatabaseName(baseDatos);
        String normalized = normalizeTableName(tabla);
        return withTenant(normalizedBase, () -> {
            switch (normalized) {
                case "usuario", "usuarios" -> editarUsuarioEnTenant(clave, datos);
                case "cliente", "clientes" -> editarClienteEnTenant(clave, datos);
                case "proveedor", "proveedores" -> editarProveedorEnTenant(clave, datos);
                case "empresa", "empresas" -> editarEmpresaEnTenant(clave, datos);
                case "producto", "productos" -> editarProductoEnTenant(clave, datos);
                default -> throw new IllegalArgumentException("La edición no está disponible para esta tabla.");
            }
            return Map.of("tabla", normalized, "clave", clave, "baseDatos", normalizedBase);
        });
    }

    public void eliminarSoporte(String baseDatos, String tabla, String clave) {
        String normalizedBase = normalizeDatabaseName(baseDatos);
        String normalized = normalizeTableName(tabla);
        withTenant(normalizedBase, () -> {
            switch (normalized) {
                case "usuario", "usuarios" -> eliminarUsuarioEnTenant(clave);
                case "cliente", "clientes" -> eliminarClienteEnTenant(clave);
                case "proveedor", "proveedores" -> eliminarProveedorEnTenant(clave);
                case "empresa", "empresas" -> eliminarEmpresaEnTenant(clave);
                case "producto", "productos" -> eliminarProductoEnTenant(clave);
                case "ticket", "tickets" -> ticketService.desactivar(parseLong(clave, "ticket"));
                case "detalle_ticket", "detalle-ticket", "detalleticket" -> ticketService.eliminarArticulos(List.of(parseDetalleTicketKey(clave)));
                default -> throw new IllegalArgumentException("La eliminación no está disponible para esta tabla.");
            }
            return null;
        });
    }

    private <T> T withTenant(String baseDatos, Supplier<T> supplier) {
        try {
            TenantContextHolder.setTenantDatabase(baseDatos);
            return supplier.get();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void editarUsuarioEnTenant(String clave, Map<String, Object> datos) {
        UsuarioEntity usuario = usuarioRepository.findByCedula(parseInt(clave, "cédula"))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        usuario.setName(stringValue(datos, "nombre", usuario.getName()));
        usuario.setEmail(normalizeEmail(stringValue(datos, "email", usuario.getEmail()), usuario.getCedula()));
        usuario.setTelefono(intValue(datos, "telefono", usuario.getTelefono()));
        usuario.setCedula(intValue(datos, "cedula", usuario.getCedula()));
        String password = stringValue(datos, "contraseña", null);
        if (password != null && !password.isBlank()) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
        usuario.setRol(parseRol(stringValue(datos, "rol", usuario.getRol() != null ? usuario.getRol().name() : null)));
        String tenantDatabase = stringValue(datos, "tenantDatabase", usuario.getTenantDatabase());
        if (tenantDatabase != null && !tenantDatabase.isBlank()) {
            usuario.setTenantDatabase(tenantDatabase.trim());
        }
        usuarioRepository.save(usuario);
    }

    private void editarClienteEnTenant(String clave, Map<String, Object> datos) {
        ClienteEntity cliente = clienteRepository.findByEmailIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        cliente.setName(stringValue(datos, "name", cliente.getName()));
        cliente.setEmail(stringValue(datos, "email", cliente.getEmail()));
        cliente.setTelefono(intValue(datos, "telefono", cliente.getTelefono()));
        clienteRepository.save(cliente);
    }

    private void editarProveedorEnTenant(String clave, Map<String, Object> datos) {
        ProveedorEntity proveedor = proveedorRepository.findByNumeroDocumentoIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        proveedor.setName(stringValue(datos, "name", proveedor.getName()));
        proveedor.setEmail(stringValue(datos, "email", proveedor.getEmail()));
        proveedor.setTelefono(intValue(datos, "telefono", proveedor.getTelefono()));
        proveedor.setNumeroDocumento(stringValue(datos, "numeroDocumento", proveedor.getNumeroDocumento()));
        proveedor.setDireccion(stringValue(datos, "direccion", proveedor.getDireccion()));
        proveedor.setRazonSocial(stringValue(datos, "razonSocial", proveedor.getRazonSocial()));
        proveedor.setTipoDocumento(parseTipoDocumento(stringValue(datos, "tipoDocumento", proveedor.getTipoDocumento() != null ? proveedor.getTipoDocumento().name() : null)));
        proveedorRepository.save(proveedor);
    }

    private void editarEmpresaEnTenant(String clave, Map<String, Object> datos) {
        EmpresaEntity empresa = empresaRepository.findByNumeroDocumentoIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        empresa.setName(stringValue(datos, "name", empresa.getName()));
        empresa.setEmail(stringValue(datos, "email", empresa.getEmail()));
        empresa.setTelefono(intValue(datos, "telefono", empresa.getTelefono()));
        empresa.setRazonSocial(stringValue(datos, "razonSocial", empresa.getRazonSocial()));
        empresa.setTipoDocumento(parseTipoDocumento(stringValue(datos, "tipoDocumento", empresa.getTipoDocumento() != null ? empresa.getTipoDocumento().name() : null)));
        empresa.setDireccion(stringValue(datos, "direccion", empresa.getDireccion()));
        empresa.setNumeroDocumento(stringValue(datos, "numeroDocumento", empresa.getNumeroDocumento()));
        empresaRepository.save(empresa);
    }

    private void editarProductoEnTenant(String clave, Map<String, Object> datos) {
        ProductoEntity producto = productoRepository.findByCodigoDeBarras(parseInt(clave, "código de barras"))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        producto.setDescripcion(stringValue(datos, "descripcion", producto.getDescripcion()));
        producto.setPrecioVenta(floatValue(datos, "precioVenta", producto.getPrecioVenta()));
        producto.setPrecioCompra(floatValue(datos, "precioCompra", producto.getPrecioCompra()));
        producto.setStock(intValue(datos, "stock", producto.getStock()));
        producto.setUnidadDeMedida(parseUnidadDeMedida(stringValue(datos, "unidadDeMedida", producto.getUnidadDeMedida() != null ? producto.getUnidadDeMedida().name() : null)));
        producto.setEtiqueta(stringValue(datos, "etiqueta", producto.getEtiqueta()));
        Long proveedorId = longValue(datos, "proveedorId", producto.getproveedorId() != null ? producto.getproveedorId().getId() : null);
        if (proveedorId == null) {
            throw new IllegalArgumentException("Proveedor es obligatorio.");
        }
        ProveedorEntity proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        producto.setproveedorId(proveedor);
        producto.setFechaUltimoIngreso(new Date());
        productoRepository.save(producto);
    }

    private void eliminarUsuarioEnTenant(String clave) {
        UsuarioEntity usuario = usuarioRepository.findByCedula(parseInt(clave, "cédula"))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        usuarioRepository.delete(usuario);
    }

    private void eliminarClienteEnTenant(String clave) {
        ClienteEntity cliente = clienteRepository.findByEmailIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        clienteRepository.delete(cliente);
    }

    private void eliminarProveedorEnTenant(String clave) {
        ProveedorEntity proveedor = proveedorRepository.findByNumeroDocumentoIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado."));
        proveedorRepository.delete(proveedor);
    }

    private void eliminarEmpresaEnTenant(String clave) {
        EmpresaEntity empresa = empresaRepository.findByNumeroDocumentoIgnoreCase(clave)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        empresaRepository.delete(empresa);
    }

    private void eliminarProductoEnTenant(String clave) {
        ProductoEntity producto = productoRepository.findByCodigoDeBarras(parseInt(clave, "código de barras"))
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        producto.setActivo(false);
        producto.setFechaUltimoIngreso(new Date());
        productoRepository.save(producto);
    }

    private List<Map<String, Object>> listarUsuariosDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    u.id,
                    p.name AS "nombre",
                    p.email,
                    p.telefono,
                    u.cedula,
                    lower(u.rol) AS "rol",
                    u.tenant_database AS "tenantDatabase",
                    p.fecha_creacion AS "fechaCreacion"
                FROM usuario u
                JOIN persona p ON p.id = u.id
                ORDER BY p.name
                """);
    }

    private List<Map<String, Object>> listarClientesDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    c.id,
                    p.name,
                    p.email,
                    p.telefono,
                    p.fecha_creacion AS "fechaCreacion"
                FROM cliente c
                JOIN persona p ON p.id = c.id
                JOIN cliente_comun cc ON cc.id = c.id
                ORDER BY p.name
                """);
    }

    private List<Map<String, Object>> listarProveedoresDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    pr.id,
                    p.name,
                    p.email,
                    p.telefono,
                    pr.numero_documento AS "numeroDocumento",
                    pr.direccion,
                    pr.razon_social AS "razonSocial",
                    pr.tipo_documento AS "tipoDocumento",
                    p.fecha_creacion AS "fechaCreacion"
                FROM proveedor pr
                JOIN persona p ON p.id = pr.id
                ORDER BY pr.razon_social
                """);
    }

    private List<Map<String, Object>> listarEmpresasDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    e.id,
                    p.name,
                    p.email,
                    p.telefono,
                    e.razon_social AS "razonSocial",
                    e.tipo_documento AS "tipoDocumento",
                    e.direccion,
                    e.numero_documento AS "numeroDocumento",
                    p.fecha_creacion AS "fechaCreacion"
                FROM empresa e
                JOIN cliente c ON c.id = e.id
                JOIN persona p ON p.id = c.id
                ORDER BY e.razon_social
                """);
    }

    private List<Map<String, Object>> listarProductosDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    pr.id,
                    pr.codigo_de_barras AS "codigoDeBarras",
                    pr.descripcion,
                    pr.precio_venta AS "precioVenta",
                    pr.precio_compra AS "precioCompra",
                    pr.stock,
                    pr.unidad_de_medida AS "unidadDeMedida",
                    pr.etiqueta,
                    pr.proveedor_id AS "proveedorId",
                    COALESCE(pp.razon_social, '') AS "proveedorNombre",
                    pr.activo,
                    pr.fecha_de_ingreso AS "fechaDeIngreso",
                    pr.fecha_ultimo_ingreso AS "fechaUltimoIngreso"
                FROM producto pr
                LEFT JOIN proveedor pv ON pv.id = pr.proveedor_id
                LEFT JOIN persona pp ON pp.id = pv.id
                ORDER BY pr.descripcion
                """);
    }

    private List<Map<String, Object>> listarTicketsDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    t.id,
                    t.fecha_creacion AS "fechaCreacion",
                    t.forma_de_pago AS "formaDePago",
                    t.monto_total AS "montoTotal",
                    t.tipo_moneda AS "tipoMoneda",
                    t.monto_pagado AS "montoPagado",
                    t.cambio_entregado AS "cambioEntregado",
                    t.devolucion,
                    t.usuario_id AS "usuarioId",
                    up.name AS "usuarioNombre",
                    t.cliente_id AS "clienteId",
                    cp.name AS "clienteNombre",
                    COUNT(dt.producto_id) AS "detalleCount"
                FROM ticket t
                LEFT JOIN usuario u ON u.id = t.usuario_id
                LEFT JOIN persona up ON up.id = u.id
                LEFT JOIN cliente c ON c.id = t.cliente_id
                LEFT JOIN persona cp ON cp.id = c.id
                LEFT JOIN detalle_ticket dt ON dt.ticket_id = t.id
                GROUP BY t.id, up.name, cp.name
                ORDER BY t.id DESC
                """);
    }

    private List<Map<String, Object>> listarDetallesTicketDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    dt.ticket_id AS "ticketId",
                    dt.producto_id AS "productoId",
                    pr.descripcion AS "productoDescripcion",
                    dt.cantidad,
                    dt.precio_unitario AS "precioUnitario"
                FROM detalle_ticket dt
                LEFT JOIN producto pr ON pr.id = dt.producto_id
                ORDER BY dt.ticket_id DESC, dt.producto_id
                """);
    }

    private List<Map<String, Object>> listarCajasDiariasDesdeBase(String baseDatos) {
        return ejecutarConsulta(baseDatos, """
                SELECT
                    c.id,
                    c.total_ingresos AS "totalIngresos",
                    c.total_egresos AS "totalEgresos",
                    c.fecha_cierre AS "fechaCierre",
                    c.diferencia,
                    c.pos_calculado AS "posCalculado",
                    c.pos_declarado AS "posDeclarado",
                    c.efectivo_calculado AS "efectivoCalculado",
                    c.efectivo_declarado AS "efectivoDeclarado",
                    COALESCE(string_agg(p.name, ', '), '') AS "usuarios"
                FROM caja_diaria c
                LEFT JOIN usuario u ON u.caja_diaria_id = c.id
                LEFT JOIN persona p ON p.id = u.id
                GROUP BY c.id
                ORDER BY c.id DESC
                """);
    }

    private List<Map<String, Object>> listarTablaGenerica(String baseDatos, String tabla) {
        String normalizedTable = normalizeTableName(tabla);
        if (normalizedTable.isBlank()) {
            throw new IllegalArgumentException("Tabla no soportada.");
        }
        return ejecutarConsulta(baseDatos, "SELECT * FROM " + quoteIdentifier(normalizedTable) + " ORDER BY 1");
    }

    private List<Map<String, Object>> ejecutarConsulta(String baseDatos, String sql) {
        try (Connection connection = tenantDataSourceFactory.getTenantDataSource(normalizeDatabaseName(baseDatos)).getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int index = 1; index <= columnCount; index++) {
                    row.put(metaData.getColumnLabel(index), resultSet.getObject(index));
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo consultar la información de soporte de la base de datos seleccionada.", ex);
        }
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

    private int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe ser numérico.");
        }
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe ser numérico.");
        }
    }

    private DetalleTicketKeyRequest parseDetalleTicketKey(String value) {
        if (value == null) {
            throw new IllegalArgumentException("La clave del detalle ticket es obligatoria.");
        }
        String[] parts = value.trim().split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("La clave del detalle ticket debe tener el formato ticketId:productoId.");
        }
        return new DetalleTicketKeyRequest(
                parseLong(parts[0], "ticketId"),
                parseLong(parts[1], "productoId"));
    }

    private int intValue(Map<String, Object> datos, String key, int fallback) {
        Object value = datos != null ? datos.get(key) : null;
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return parseInt(String.valueOf(value), key);
    }

    private long longValue(Map<String, Object> datos, String key, Long fallback) {
        Object value = datos != null ? datos.get(key) : null;
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return parseLong(String.valueOf(value), key);
    }

    private float floatValue(Map<String, Object> datos, String key, float fallback) {
        Object value = datos != null ? datos.get(key) : null;
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (Exception ex) {
            throw new IllegalArgumentException("El valor de " + key + " debe ser numérico.");
        }
    }

    private String stringValue(Map<String, Object> datos, String key, String fallback) {
        Object value = datos != null ? datos.get(key) : null;
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private com.zephyra.genesis.entity.TIPO_DOCUMENTO parseTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return com.zephyra.genesis.entity.TIPO_DOCUMENTO.CI;
        }
        String normalized = tipoDocumento.trim().toUpperCase();
        return switch (normalized) {
            case "RUT" -> com.zephyra.genesis.entity.TIPO_DOCUMENTO.RUT;
            case "RUC" -> com.zephyra.genesis.entity.TIPO_DOCUMENTO.RUC;
            default -> com.zephyra.genesis.entity.TIPO_DOCUMENTO.CI;
        };
    }

    private com.zephyra.genesis.entity.UNIDAD_MEDIDA parseUnidadDeMedida(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Unidad de medida es obligatoria.");
        }
        return com.zephyra.genesis.entity.UNIDAD_MEDIDA.valueOf(value.trim().toUpperCase());
    }

    private String normalizeDatabaseName(String baseDatos) {
        if (baseDatos == null || baseDatos.isBlank()) {
            throw new IllegalArgumentException("La base de datos es obligatoria.");
        }
        return baseDatos.trim();
    }

    private String normalizeTableName(String tabla) {
        if (tabla == null) {
            return "";
        }
        return tabla.trim().toLowerCase();
    }

    private boolean isSystemTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String normalized = tableName.trim().toLowerCase();
        return normalized.equals("flyway_schema_history")
                || normalized.equals("schema_version")
                || normalized.equals("databasechangelog")
                || normalized.equals("databasechangeloglock");
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}