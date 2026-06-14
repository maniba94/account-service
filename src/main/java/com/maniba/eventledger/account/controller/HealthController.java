package com.maniba.eventledger.account.controller;

import javax.sql.DataSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getHealth() {
        String dbStatus = "DOWN";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            dbStatus = "UP";
        } catch (Exception ignored) {
            dbStatus = "DOWN";
        }
        return ResponseEntity.ok(Map.of("status", "UP", "database", dbStatus));
    }
}
