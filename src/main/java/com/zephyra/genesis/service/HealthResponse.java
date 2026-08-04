package com.zephyra.genesis.service;

public class HealthResponse {
    private final String message;

    public HealthResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
