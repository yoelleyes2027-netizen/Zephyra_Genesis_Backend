package com.zephyra.genesis.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class AdminDatabaseConfig {

    @Bean
    public DataSource adminDataSource(Environment environment) {
        String primaryUrl = required(environment, "spring.datasource.url");
        String primaryUsername = required(environment, "spring.datasource.username");
        String primaryPassword = required(environment, "spring.datasource.password");

        String adminUrl = resolvedUrl(environment.getProperty("spring.admin-datasource.url"), primaryUrl, "zephyra_admins");
        String adminUsername = resolvedValue(environment.getProperty("spring.admin-datasource.username"), primaryUsername);
        String adminPassword = resolvedValue(environment.getProperty("spring.admin-datasource.password"), primaryPassword);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(adminUrl);
        dataSource.setUsername(adminUsername);
        dataSource.setPassword(adminPassword);
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));
        dataSource.setPoolName("admin-datasource");
        dataSource.setConnectionInitSql("SET TIME ZONE 'America/Montevideo'");
        return dataSource;
    }

    private String resolvedUrl(String configuredValue, String primaryUrl, String databaseName) {
        if (configuredValue != null && !configuredValue.isBlank() && !configuredValue.contains("localhost:5432/zephyra_admins")) {
            return configuredValue;
        }

        int queryIndex = primaryUrl.indexOf('?');
        String urlWithoutQuery = queryIndex >= 0 ? primaryUrl.substring(0, queryIndex) : primaryUrl;
        String query = queryIndex >= 0 ? primaryUrl.substring(queryIndex) : "";
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException("La URL principal de PostgreSQL no es válida.");
        }
        return urlWithoutQuery.substring(0, lastSlash + 1) + databaseName + query;
    }

    private String resolvedValue(String configuredValue, String fallbackValue) {
        if (configuredValue != null && !configuredValue.isBlank() && !"postgres".equalsIgnoreCase(configuredValue)) {
            return configuredValue;
        }
        return fallbackValue;
    }

    private String required(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad requerida: " + key);
        }
        return value;
    }
}