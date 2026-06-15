package com.maniba.eventledger.account.controller;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getHealth() {
        log.info("GET health request received");
        String dbStatus = "DOWN";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            dbStatus = "UP";
        } catch (Exception ex) {
            dbStatus = "DOWN";
            log.warn("Health check database probe failed", ex);
        }
        Map<String, String> result = Map.of("status", "UP", "database", dbStatus);
        log.info("Health response: {}", result);
        return ResponseEntity.ok(result);
    }
}
