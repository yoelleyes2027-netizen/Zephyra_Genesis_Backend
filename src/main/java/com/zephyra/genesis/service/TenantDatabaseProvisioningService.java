package com.zephyra.genesis.service;

import com.zephyra.genesis.config.TenantDataSourceFactory;
import com.zephyra.genesis.entity.UsuarioEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

@Service
public class TenantDatabaseProvisioningService {

    private final DataSource masterDataSource;
    private final TenantDataSourceFactory tenantDataSourceFactory;

    public TenantDatabaseProvisioningService(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            TenantDataSourceFactory tenantDataSourceFactory) {
        this.masterDataSource = masterDataSource;
        this.tenantDataSourceFactory = tenantDataSourceFactory;
    }

    public void ensureTenantDatabase(String tenantDatabase) {
        validateTenantDatabase(tenantDatabase);
        if (!databaseExists(tenantDatabase)) {
            createTenantDatabase(tenantDatabase);
        }
        initializeSchema(tenantDatabase);
        normalizePersonaFechaCreacionColumn(tenantDatabase);
        actualizarEsquemaTicket(tenantDatabase);
        actualizarEsquemaCaja(tenantDatabase);
        backfillClienteComun(tenantDatabase);
    }

    public void upsertUsuario(String tenantDatabase, UsuarioEntity usuario) {
        validateTenantDatabase(tenantDatabase);
        ensureTenantDatabase(tenantDatabase);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(tenantDataSourceFactory.getTenantDataSource(tenantDatabase));
        Connection connection = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            connection.setAutoCommit(false);

            boolean exists = usuarioExists(connection, usuario.getId());
            if (exists) {
                updatePersona(connection, usuario);
                updateUsuario(connection, usuario);
            } else {
                insertPersona(connection, usuario);
                insertUsuario(connection, usuario);
            }

            connection.commit();
        } catch (SQLException ex) {
            rollbackQuietly(connection);
            throw new IllegalStateException("No se pudo sincronizar el usuario en la base de datos del tenant.", ex);
        } finally {
            closeQuietly(connection);
        }
    }

    public void deleteUsuario(String tenantDatabase, Long usuarioId) {
        validateTenantDatabase(tenantDatabase);
        if (usuarioId == null) {
            return;
        }

        ensureTenantDatabase(tenantDatabase);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(tenantDataSourceFactory.getTenantDataSource(tenantDatabase));
        Connection connection = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement deleteUsuario = connection.prepareStatement("DELETE FROM usuario WHERE id = ?");
                 PreparedStatement deletePersona = connection.prepareStatement("DELETE FROM persona WHERE id = ?")) {
                deleteUsuario.setLong(1, usuarioId);
                deleteUsuario.executeUpdate();

                deletePersona.setLong(1, usuarioId);
                deletePersona.executeUpdate();
            }

            connection.commit();
        } catch (SQLException ex) {
            rollbackQuietly(connection);
            throw new IllegalStateException("No se pudo eliminar el usuario del tenant.", ex);
        } finally {
            closeQuietly(connection);
        }
    }

    private boolean databaseExists(String tenantDatabase) {
        try (Connection connection = masterDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, tenantDatabase);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo verificar la existencia de la base de datos del tenant.", ex);
        }
    }

    private void createTenantDatabase(String tenantDatabase) {
        try (Connection connection = masterDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE \"" + tenantDatabase + "\"");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo crear la base de datos del tenant.", ex);
        }
    }

    private void initializeSchema(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(tenantDataSource);
        factoryBean.setPackagesToScan("com.zephyra.genesis.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        factoryBean.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        factoryBean.setJpaProperties(jpaProperties);

        try {
            factoryBean.afterPropertiesSet();
            EntityManagerFactory entityManagerFactory = factoryBean.getObject();
            if (entityManagerFactory != null) {
                entityManagerFactory.close();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar el esquema del tenant.", ex);
        }
    }

    private boolean usuarioExists(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM usuario WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertPersona(Connection connection, UsuarioEntity usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO persona (id, name, email, telefono, fecha_creacion) VALUES (?, ?, ?, ?, ?)" );) {
            statement.setLong(1, usuario.getId());
            statement.setString(2, usuario.getName());
            statement.setString(3, usuario.getEmail());
            statement.setInt(4, usuario.getTelefono());
            statement.setTimestamp(5, toTimestamp(usuario.getFechaCreacion()));
            statement.executeUpdate();
        }
    }

    private void updatePersona(Connection connection, UsuarioEntity usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE persona SET name = ?, email = ?, telefono = ?, fecha_creacion = ? WHERE id = ?")) {
            statement.setString(1, usuario.getName());
            statement.setString(2, usuario.getEmail());
            statement.setInt(3, usuario.getTelefono());
            statement.setTimestamp(4, toTimestamp(usuario.getFechaCreacion()));
            statement.setLong(5, usuario.getId());
            statement.executeUpdate();
        }
    }

    private void normalizePersonaFechaCreacionColumn(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        try (Connection connection = tenantDataSource.getConnection()) {
            boolean hasFechaCreacion = columnExists(connection, "persona", "fecha_creacion");
            boolean hasFechacreacion = columnExists(connection, "persona", "fechacreacion");

            if (hasFechacreacion && !hasFechaCreacion) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE persona RENAME COLUMN fechacreacion TO fecha_creacion");
                }
                return;
            }

            if (hasFechacreacion && hasFechaCreacion) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE persona SET fecha_creacion = COALESCE(fecha_creacion, fechacreacion) WHERE fecha_creacion IS NULL");
                    statement.executeUpdate("ALTER TABLE persona DROP COLUMN IF EXISTS fechacreacion");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo normalizar la columna de fecha de creación de persona.", ex);
        }
    }

    private void backfillClienteComun(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        try (Connection connection = tenantDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO cliente_comun (id)
                    SELECT c.id
                    FROM cliente c
                    LEFT JOIN empresa e ON e.id = c.id
                    LEFT JOIN cliente_comun cc ON cc.id = c.id
                    WHERE e.id IS NULL
                      AND cc.id IS NULL
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo completar la tabla cliente_comun del tenant.", ex);
        }
    }

    private void actualizarEsquemaTicket(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        try (Connection connection = tenantDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tipo_moneda VARCHAR(3)");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS monto_pagado REAL");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS cambio_entregado REAL");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS devolucion BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo actualizar el esquema de ticket del tenant.", ex);
        }
    }

    private void actualizarEsquemaCaja(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        try (Connection connection = tenantDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS fecha_inicio DATE");
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_pos REAL NOT NULL DEFAULT 0");
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_efectivo REAL NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS caja_global (
                        id BIGSERIAL PRIMARY KEY,
                        total_ingresos REAL NOT NULL DEFAULT 0,
                        total_egresos REAL NOT NULL DEFAULT 0,
                        fecha_inicio DATE,
                        fecha_cierre DATE,
                        diferencia REAL NOT NULL DEFAULT 0,
                        diferencia_pos REAL NOT NULL DEFAULT 0,
                        diferencia_efectivo REAL NOT NULL DEFAULT 0,
                        pos_calculado REAL NOT NULL DEFAULT 0,
                        pos_declarado REAL NOT NULL DEFAULT 0,
                        efectivo_calculado INTEGER NOT NULL DEFAULT 0,
                        efectivo_declarado INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS caja_global_id BIGINT");
            statement.executeUpdate("ALTER TABLE caja_diaria DROP CONSTRAINT IF EXISTS fk_caja_diaria_caja_global");
            statement.executeUpdate("""
                    ALTER TABLE caja_diaria ADD CONSTRAINT fk_caja_diaria_caja_global
                    FOREIGN KEY (caja_global_id) REFERENCES caja_global(id)
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo actualizar el esquema de caja del tenant.", ex);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?")) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insertUsuario(Connection connection, UsuarioEntity usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO usuario (id, cedula, password, rol, tenant_database, foto_perfil, fecha_inicio_de_dia, caja_diaria_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");) {
            statement.setLong(1, usuario.getId());
            statement.setInt(2, usuario.getCedula());
            statement.setString(3, usuario.getPassword());
            statement.setString(4, usuario.getRol() != null ? usuario.getRol().name() : null);
            statement.setString(5, usuario.getTenantDatabase());
            statement.setBytes(6, usuario.getFotoPerfil());
            statement.setTimestamp(7, toTimestamp(usuario.getFechaInicioDeDia()));
            if (usuario.getCajaDiaria() != null && usuario.getCajaDiaria().getId() != null) {
                statement.setLong(8, usuario.getCajaDiaria().getId());
            } else {
                statement.setNull(8, java.sql.Types.BIGINT);
            }
            statement.executeUpdate();
        }
    }

    private void updateUsuario(Connection connection, UsuarioEntity usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE usuario SET cedula = ?, password = ?, rol = ?, tenant_database = ?, foto_perfil = ?, fecha_inicio_de_dia = ?, caja_diaria_id = ? WHERE id = ?")) {
            statement.setInt(1, usuario.getCedula());
            statement.setString(2, usuario.getPassword());
            statement.setString(3, usuario.getRol() != null ? usuario.getRol().name() : null);
            statement.setString(4, usuario.getTenantDatabase());
            statement.setBytes(5, usuario.getFotoPerfil());
            statement.setTimestamp(6, toTimestamp(usuario.getFechaInicioDeDia()));
            if (usuario.getCajaDiaria() != null && usuario.getCajaDiaria().getId() != null) {
                statement.setLong(7, usuario.getCajaDiaria().getId());
            } else {
                statement.setNull(7, java.sql.Types.BIGINT);
            }
            statement.setLong(8, usuario.getId());
            statement.executeUpdate();
        }
    }

    private Timestamp toTimestamp(Date date) {
        return date != null ? new Timestamp(date.getTime()) : null;
    }

    private void validateTenantDatabase(String tenantDatabase) {
        if (tenantDatabase == null || !tenantDatabase.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("La base de datos del tenant es inválida.");
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}