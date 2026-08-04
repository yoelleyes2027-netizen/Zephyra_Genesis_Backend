package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.LoginRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final String primaryUrl;
    private final String primaryUsername;
    private final String primaryPassword;
    private final int adminCedula;
    private final String adminPassword;

    public AdminSystemAuthService(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            PasswordEncoder passwordEncoder,
            @Value("${spring.datasource.url}") String primaryUrl,
            @Value("${spring.datasource.username}") String primaryUsername,
            @Value("${spring.datasource.password}") String primaryPassword,
            @Value("${app.admin-system.cedula:" + DEFAULT_ADMIN_CEDULA + "}") int adminCedula,
            @Value("${app.admin-system.password:" + DEFAULT_ADMIN_PASSWORD + "}") String adminPassword) {
        this.adminDataSource = adminDataSource;
        this.passwordEncoder = passwordEncoder;
        this.primaryUrl = primaryUrl;
        this.primaryUsername = primaryUsername;
        this.primaryPassword = primaryPassword;
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

        String bootstrapUrl = buildProbeUrl(primaryUrl, "postgres");
        try (Connection connection = java.sql.DriverManager.getConnection(bootstrapUrl, primaryUsername, primaryPassword);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE zephyra_admins");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo crear la base de datos zephyra_admins.", ex);
        }
    }

    private boolean databaseExists() {
        String probeUrl = buildProbeUrl(primaryUrl, "postgres");
        try (Connection connection = java.sql.DriverManager.getConnection(probeUrl, primaryUsername, primaryPassword);
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = 'zephyra_admins'")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo verificar la base de datos zephyra_admins.", ex);
        }
    }

    private String buildProbeUrl(String jdbcUrl, String databaseName) {
        int queryIndex = jdbcUrl.indexOf('?');
        String urlWithoutQuery = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
        String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException("La URL de bootstrap de PostgreSQL no es válida.");
        }
        return urlWithoutQuery.substring(0, lastSlash + 1) + databaseName + query;
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
        String encodedPassword = passwordEncoder.encode(adminPassword);
        try (Connection connection = adminDataSource.getConnection()) {
            if (findAdminByCedula(adminCedula).isPresent()) {
                try (PreparedStatement statement = connection.prepareStatement("UPDATE admin SET password = ? WHERE cedula = ?")) {
                    statement.setString(1, encodedPassword);
                    statement.setInt(2, adminCedula);
                    statement.executeUpdate();
                }
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO admin (cedula, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, adminCedula);
                statement.setString(2, encodedPassword);
                statement.executeUpdate();
            }
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