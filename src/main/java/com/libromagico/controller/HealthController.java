package com.libromagico.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    /**
     * Timeout máximo de la comprobación de BD. Sin estos límites, cuando la base
     * cae la conexión TCP queda "half-open" y el {@code SELECT 1} puede bloquearse
     * sin límite, colgando el health endpoint (y por lo tanto el healthcheck).
     */
    private static final int HEALTH_TIMEOUT_MS = 3000;

    private static final ExecutorService NETWORK_TIMEOUT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "health-network-timeout");
        t.setDaemon(true);
        return t;
    });

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = isDatabaseUp();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbUp ? "UP" : "DOWN");
        body.put("db", dbUp ? "UP" : "DOWN");

        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            // Bounds the read on a possibly half-open connection.
            connection.setNetworkTimeout(NETWORK_TIMEOUT_EXECUTOR, HEALTH_TIMEOUT_MS);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(HEALTH_TIMEOUT_MS / 1000);
                try (ResultSet rs = statement.executeQuery("SELECT 1")) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
