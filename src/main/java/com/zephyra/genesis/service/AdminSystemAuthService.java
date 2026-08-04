package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.LoginRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

@Service
public class AdminSystemAuthService {

    private static final int DEFAULT_ADMIN_CEDULA = 14923582;
    private static final String DEFAULT_ADMIN_PASSWORD = "Xm_84Qa_Zp19_LvR_72Nk";

    private final DataSource adminDataSource;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUrl;
    private final String bootstrapUsername;
    private final String bootstrapPassword;
    private final int adminCedula;
    private final String adminPassword;

    public AdminSystemAuthService(
            DataSource adminDataSource,
            PasswordEncoder passwordEncoder,
            @Value("${spring.admin-datasource.bootstrap-url}") String bootstrapUrl,
            @Value("${spring.admin-datasource.bootstrap-username}") String bootstrapUsername,
            @Value("${spring.admin-datasource.bootstrap-password}") String bootstrapPassword,
            @Value("${app.admin-system.cedula:" + DEFAULT_ADMIN_CEDULA + "}") int adminCedula,
            @Value("${app.admin-system.password:" + DEFAULT_ADMIN_PASSWORD + "}") String adminPassword) {
        this.adminDataSource = adminDataSource;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUrl = bootstrapUrl;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
        this.adminCedula = adminCedula;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    public void initializeAdminDatabase() {
        ensureDatabaseAndSeed();
    }

    public Optional<AuthUserResponse> tryAuthenticate(LoginRequest request) {
        return findAdminByCedula(request.cedula())
                .filter(admin -> passwordEncoder.matches(request.contraseña(), admin.password()))
                .map(admin -> new AuthUserResponse(
                        admin.id(),
                        "Administrador de Sistema",
                        "admin_sistema",
                        admin.cedula(),
                        null));
    }

    private void ensureDatabaseAndSeed() {
        ensureDatabaseExists();
        ensureTableExists();
        ensureSeedAdminExists();
    }

    private void ensureDatabaseExists() {
        if (databaseExists()) {
            return;
        }

        try (Connection connection = java.sql.DriverManager.getConnection(bootstrapUrl, bootstrapUsername, bootstrapPassword);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE zephyra_admins");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo crear la base de datos zephyra_admins.", ex);
        }
    }

    private boolean databaseExists() {
        String probeUrl = buildProbeUrl();
        try (Connection connection = java.sql.DriverManager.getConnection(probeUrl, bootstrapUsername, bootstrapPassword);
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = 'zephyra_admins'")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo verificar la base de datos zephyra_admins.", ex);
        }
    }

    private String buildProbeUrl() {
        int queryIndex = bootstrapUrl.indexOf('?');
        String urlWithoutQuery = queryIndex >= 0 ? bootstrapUrl.substring(0, queryIndex) : bootstrapUrl;
        String query = queryIndex >= 0 ? bootstrapUrl.substring(queryIndex) : "";
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException("La URL de bootstrap de PostgreSQL no es válida.");
        }
        return urlWithoutQuery.substring(0, lastSlash + 1) + "postgres" + query;
    }

    private void ensureTableExists() {
        try (Connection connection = adminDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admin (
                        id BIGSERIAL PRIMARY KEY,
                        cedula INTEGER NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL
                    )
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo crear la tabla admin.", ex);
        }
    }

    private void ensureSeedAdminExists() {
        if (findAdminByCedula(adminCedula).isPresent()) {
            return;
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);
        try (Connection connection = adminDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO admin (cedula, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, adminCedula);
            statement.setString(2, encodedPassword);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo crear el admin de sistema inicial.", ex);
        }
    }

    private Optional<AdminRow> findAdminByCedula(int cedula) {
        try (Connection connection = adminDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, cedula, password FROM admin WHERE cedula = ?")) {
            statement.setInt(1, cedula);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new AdminRow(
                        resultSet.getLong("id"),
                        resultSet.getInt("cedula"),
                        resultSet.getString("password")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el admin de sistema.", ex);
        }
    }

    private record AdminRow(Long id, int cedula, String password) {
    }
}