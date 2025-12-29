package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller for monitoring and API Gateway integration.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints for monitoring")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns API health status for monitoring and gateway health checks")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "NettoApi",
                "version", "1.0.0"
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Returns whether the API is ready to receive traffic")
    public ResponseEntity<Map<String, Object>> ready() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Returns whether the API is alive")
    public ResponseEntity<Map<String, Object>> live() {
        return ResponseEntity.ok(Map.of(
                "status", "ALIVE",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
