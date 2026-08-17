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
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS forma_de_pago VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS monto_total REAL");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tipo_moneda VARCHAR(3)");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS monto_pagado REAL");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS cambio_entregado REAL");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS devolucion BOOLEAN NOT NULL DEFAULT FALSE");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS devolucion_realizada BOOLEAN NOT NULL DEFAULT FALSE");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS egreso BOOLEAN NOT NULL DEFAULT FALSE");
            statement.executeUpdate("ALTER TABLE ticket ADD COLUMN IF NOT EXISTS egresos_descripcion VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE ticket ALTER COLUMN tipo_moneda SET DEFAULT 'UYU'");
            statement.executeUpdate("UPDATE ticket SET tipo_moneda = 'UYU' WHERE tipo_moneda IS NULL");
            statement.executeUpdate("UPDATE ticket SET monto_pagado = monto_total WHERE forma_de_pago IN ('TARJETA','TRANSFERENCIA') AND monto_pagado IS NULL");

            if (columnExists(connection, "ticket", "fechacreacion")) {
                statement.executeUpdate("UPDATE ticket SET fecha_creacion = COALESCE(fecha_creacion, fechacreacion) WHERE fecha_creacion IS NULL");
                statement.executeUpdate("ALTER TABLE ticket DROP COLUMN IF EXISTS fechacreacion");
            }
            if (columnExists(connection, "ticket", "formadepago")) {
                statement.executeUpdate("UPDATE ticket SET forma_de_pago = COALESCE(forma_de_pago, formadepago) WHERE forma_de_pago IS NULL");
                statement.executeUpdate("ALTER TABLE ticket DROP COLUMN IF EXISTS formadepago");
            }
            if (columnExists(connection, "ticket", "montototal")) {
                statement.executeUpdate("UPDATE ticket SET monto_total = COALESCE(monto_total, montototal) WHERE monto_total IS NULL");
                statement.executeUpdate("ALTER TABLE ticket DROP COLUMN IF EXISTS montototal");
            }

            statement.executeUpdate("ALTER TABLE detalle_ticket ADD COLUMN IF NOT EXISTS precio_unitario REAL");
            if (columnExists(connection, "detalle_ticket", "preciounitario")) {
                statement.executeUpdate("UPDATE detalle_ticket SET precio_unitario = COALESCE(precio_unitario, preciounitario) WHERE precio_unitario IS NULL");
                statement.executeUpdate("ALTER TABLE detalle_ticket DROP COLUMN IF EXISTS preciounitario");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo actualizar el esquema de ticket del tenant.", ex);
        }
    }

    private void actualizarEsquemaCaja(String tenantDatabase) {
        DataSource tenantDataSource = tenantDataSourceFactory.getTenantDataSource(tenantDatabase);
        try (Connection connection = tenantDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS fecha_inicio DATE");
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS transferencia_calculada REAL");
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_pos REAL NOT NULL DEFAULT 0");
            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS diferencia_efectivo REAL NOT NULL DEFAULT 0");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS caja_global (
                        id BIGSERIAL PRIMARY KEY,
                        total_ingresos REAL,
                        total_egresos REAL,
                        transferencia_calculada REAL,
                        fecha_inicio DATE NOT NULL,
                        fecha_cierre DATE,
                        diferencia REAL,
                        diferencia_pos REAL,
                        diferencia_efectivo REAL,
                        pos_calculado REAL,
                        pos_declarado REAL,
                        efectivo_calculado INTEGER,
                        efectivo_declarado INTEGER
                    )
                    """);
            statement.executeUpdate("UPDATE caja_global SET fecha_inicio = CURRENT_DATE WHERE fecha_inicio IS NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN fecha_inicio SET NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN total_ingresos DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN total_egresos DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ADD COLUMN IF NOT EXISTS transferencia_calculada REAL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN transferencia_calculada DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia_pos DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia_efectivo DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN pos_calculado DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN pos_declarado DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN efectivo_calculado DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN efectivo_declarado DROP NOT NULL");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN total_ingresos DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN total_egresos DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN transferencia_calculada DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia_pos DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN diferencia_efectivo DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN pos_calculado DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN pos_declarado DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN efectivo_calculado DROP DEFAULT");
            statement.executeUpdate("ALTER TABLE caja_global ALTER COLUMN efectivo_declarado DROP DEFAULT");

            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS caja_global_id BIGINT");
            statement.executeUpdate("ALTER TABLE caja_diaria DROP CONSTRAINT IF EXISTS fk_caja_diaria_caja_global");
            statement.executeUpdate("""
                    ALTER TABLE caja_diaria ADD CONSTRAINT fk_caja_diaria_caja_global
                    FOREIGN KEY (caja_global_id) REFERENCES caja_global(id)
                    """);

            statement.executeUpdate("ALTER TABLE caja_diaria ADD COLUMN IF NOT EXISTS usuario_id BIGINT");
            statement.executeUpdate("""
                    DO $$
                    BEGIN
                        IF EXISTS (
                            SELECT 1 FROM information_schema.columns
                            WHERE table_name = 'usuario' AND column_name = 'caja_diaria_id'
                        ) THEN
                            UPDATE caja_diaria cd
                            SET usuario_id = u.id
                            FROM usuario u
                            WHERE u.caja_diaria_id = cd.id
                              AND cd.usuario_id IS NULL;
                        END IF;
                    END $$;
                    """);
            statement.executeUpdate("ALTER TABLE caja_diaria DROP CONSTRAINT IF EXISTS fk_caja_diaria_usuario");
            statement.executeUpdate("""
                    ALTER TABLE caja_diaria ADD CONSTRAINT fk_caja_diaria_usuario
                    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
                    """);
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS ux_caja_diaria_usuario_id ON caja_diaria(usuario_id)");

            // Elimina relación legacy inversa para permitir TRUNCATE en caja_diaria sin CASCADE.
            statement.executeUpdate("""
                    DO $$
                    DECLARE fk_name TEXT;
                    BEGIN
                        FOR fk_name IN
                            SELECT tc.constraint_name
                            FROM information_schema.table_constraints tc
                            JOIN information_schema.key_column_usage kcu
                              ON tc.constraint_name = kcu.constraint_name
                             AND tc.table_schema = kcu.table_schema
                            WHERE tc.table_schema = 'public'
                              AND tc.table_name = 'usuario'
                              AND tc.constraint_type = 'FOREIGN KEY'
                              AND kcu.column_name = 'caja_diaria_id'
                        LOOP
                            EXECUTE format('ALTER TABLE public.usuario DROP CONSTRAINT IF EXISTS %I', fk_name);
                        END LOOP;
                    END $$;
                    """);
            statement.executeUpdate("""
                    DO $$
                    DECLARE idx_name TEXT;
                    BEGIN
                        FOR idx_name IN
                            SELECT indexname
                            FROM pg_indexes
                            WHERE schemaname = 'public'
                              AND tablename = 'usuario'
                              AND indexdef ILIKE '%(caja_diaria_id%'
                        LOOP
                            EXECUTE format('DROP INDEX IF EXISTS public.%I', idx_name);
                        END LOOP;
                    END $$;
                    """);
            statement.executeUpdate("ALTER TABLE usuario DROP COLUMN IF EXISTS caja_diaria_id");
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
                "INSERT INTO usuario (id, cedula, password, rol, tenant_database, foto_perfil, fecha_inicio_de_dia) VALUES (?, ?, ?, ?, ?, ?, ?)");) {
            statement.setLong(1, usuario.getId());
            statement.setInt(2, usuario.getCedula());
            statement.setString(3, usuario.getPassword());
            statement.setString(4, usuario.getRol() != null ? usuario.getRol().name() : null);
            statement.setString(5, usuario.getTenantDatabase());
            statement.setBytes(6, usuario.getFotoPerfil());
            statement.setTimestamp(7, toTimestamp(usuario.getFechaInicioDeDia()));
            statement.executeUpdate();
        }
    }

    private void updateUsuario(Connection connection, UsuarioEntity usuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE usuario SET cedula = ?, password = ?, rol = ?, tenant_database = ?, foto_perfil = ?, fecha_inicio_de_dia = ? WHERE id = ?")) {
            statement.setInt(1, usuario.getCedula());
            statement.setString(2, usuario.getPassword());
            statement.setString(3, usuario.getRol() != null ? usuario.getRol().name() : null);
            statement.setString(4, usuario.getTenantDatabase());
            statement.setBytes(5, usuario.getFotoPerfil());
            statement.setTimestamp(6, toTimestamp(usuario.getFechaInicioDeDia()));
            statement.setLong(7, usuario.getId());
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