package com.example.thangcachep.movie_project_be.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HealthController {

    private final LocalizationUtils localizationUtils;

    // Root level health check (Azure App Service thường check endpoint này)
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "movie-project-be");
        data.put("timestamp", System.currentTimeMillis());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.HEALTH_CHECK_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    // Health check với /api/v1 prefix
    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthApi() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "movie-project-be");
        data.put("timestamp", System.currentTimeMillis());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.HEALTH_CHECK_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    // Health check alternative
    @GetMapping("/api/v1/healthcheck")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthcheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "movie-project-be");
        data.put("timestamp", System.currentTimeMillis());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.HEALTH_CHECK_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }
}

