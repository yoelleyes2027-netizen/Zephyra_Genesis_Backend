package com.zephyra.genesis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthServiceTest {

    @Test
    void shouldReturnHealthyMessage() {
        HealthService service = new HealthServiceImpl();

        assertEquals("Application is healthy", service.getHealthStatus().getMessage());
    }
}
