package com.zephyra.genesis.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TenantDataSourceFactory {

    private final String baseUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final ConcurrentMap<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    public TenantDataSourceFactory(
            @Value("${spring.datasource.url}") String baseUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
    }

    public DataSource getTenantDataSource(String tenantDatabase) {
        validateTenantDatabase(tenantDatabase);
        return dataSources.computeIfAbsent(tenantDatabase, this::createTenantDataSource);
    }

    public String getTenantJdbcUrl(String tenantDatabase) {
        validateTenantDatabase(tenantDatabase);
        int queryIndex = baseUrl.indexOf('?');
        String urlWithoutQuery = queryIndex >= 0 ? baseUrl.substring(0, queryIndex) : baseUrl;
        String query = queryIndex >= 0 ? baseUrl.substring(queryIndex) : "";
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException("La URL base de PostgreSQL no es válida.");
        }
        return urlWithoutQuery.substring(0, lastSlash + 1) + tenantDatabase + query;
    }

    @PreDestroy
    public void closePools() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }

    private HikariDataSource createTenantDataSource(String tenantDatabase) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(getTenantJdbcUrl(tenantDatabase));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("tenant-" + tenantDatabase);
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    private void validateTenantDatabase(String tenantDatabase) {
        if (tenantDatabase == null || !tenantDatabase.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("El nombre de la base de datos es inválido.");
        }
    }
}