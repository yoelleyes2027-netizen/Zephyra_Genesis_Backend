package com.zephyra.genesis.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClienteComunBackfillService {

    private final JdbcTemplate jdbcTemplate;

    public ClienteComunBackfillService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillClienteComun() {
        jdbcTemplate.update("""
                INSERT INTO cliente_comun (id)
                SELECT c.id
                FROM cliente c
                LEFT JOIN empresa e ON e.id = c.id
                LEFT JOIN cliente_comun cc ON cc.id = c.id
                WHERE e.id IS NULL
                  AND cc.id IS NULL
                """);
    }
}