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
                "INSERT INTO persona (id, name, email, telefono, fechacreacion) VALUES (?, ?, ?, ?, ?)" );) {
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
                "UPDATE persona SET name = ?, email = ?, telefono = ?, fechacreacion = ? WHERE id = ?")) {
            statement.setString(1, usuario.getName());
            statement.setString(2, usuario.getEmail());
            statement.setInt(3, usuario.getTelefono());
            statement.setTimestamp(4, toTimestamp(usuario.getFechaCreacion()));
            statement.setLong(5, usuario.getId());
            statement.executeUpdate();
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