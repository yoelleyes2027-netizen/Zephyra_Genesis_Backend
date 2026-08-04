package com.zephyra.genesis.service;

import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public HealthResponse getHealthStatus() {
        return new HealthResponse("Application is healthy");
    }
}
