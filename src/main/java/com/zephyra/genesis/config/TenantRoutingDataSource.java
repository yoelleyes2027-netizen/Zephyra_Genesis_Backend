package com.zephyra.genesis.config;

import com.zephyra.genesis.tenant.TenantContextHolder;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TenantRoutingDataSource extends AbstractDataSource {

    private final DataSource masterDataSource;
    private final TenantDataSourceFactory tenantDataSourceFactory;

    public TenantRoutingDataSource(DataSource masterDataSource, TenantDataSourceFactory tenantDataSourceFactory) {
        this.masterDataSource = masterDataSource;
        this.tenantDataSourceFactory = tenantDataSourceFactory;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return resolveDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return resolveDataSource().getConnection(username, password);
    }

    private DataSource resolveDataSource() {
        if (TenantContextHolder.isMaster()) {
            return masterDataSource;
        }
        return tenantDataSourceFactory.getTenantDataSource(TenantContextHolder.getCurrentTenantDatabase());
    }
}