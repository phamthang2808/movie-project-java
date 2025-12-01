package com.example.thangcachep.movie_project_be.models.request;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequest(
        String message,
        @JsonProperty("systemPrompt") String systemPrompt,
        @JsonProperty("conversationHistory") List<Map<String, String>> conversationHistory,
        @JsonProperty("movieData") List<Map<String, Object>> movieData
) {
    // Constructor mặc định để tương thích với request chỉ có message
    public ChatRequest(String message) {
        this(message, null, null, null);
    }
}