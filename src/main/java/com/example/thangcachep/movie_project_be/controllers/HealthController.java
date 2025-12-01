package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    // Root level health check (Azure App Service thường check endpoint này)
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "movie-project-be");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // Health check với /api/v1 prefix
    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> healthApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "movie-project-be");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // Health check alternative
    @GetMapping("/api/v1/healthcheck")
    public ResponseEntity<Map<String, Object>> healthcheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "movie-project-be");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}

